package net.pcal.quicksort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import static net.minecraft.world.level.block.ChestBlock.getContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.pcal.quicksort.QuicksortConfig.AnimationMode;
import net.pcal.quicksort.QuicksortConfig.QuicksortChestConfig;
import net.pcal.quicksort.QuicksortConfig.TransferMode;

/**
 * Singleton that makes the quicksorter chests do their thing.
 */
public class QuicksortService implements ServerTickEvents.EndLevelTick {

    // ===================================================================================
    // Singleton

    private static final QuicksortService INSTANCE = new QuicksortService();

    public static QuicksortService getInstance() {
        return INSTANCE;
    }

    // ===================================================================================
    // Constants

    public static final String LOGGER_NAME = "Quicksort";
    public static final String LOG_PREFIX = "[Quicksort] ";

    // ===================================================================================
    // Fields

    private final List<QuicksorterJob> jobs = new ArrayList<>();
    private final Set<LevelChestPos> loadedChests = new HashSet<>();
    private final Set<LevelChestPos> loadedContainers = new HashSet<>();
    private final Set<LevelChestPos> openChests = new HashSet<>();
    private final Map<LevelChestPos, Boolean> redstonePowered = new HashMap<>();
    private final Map<LevelChestPos, Long> redstoneNextAttemptTick = new HashMap<>();
    private Map<Identifier, QuicksortChestConfig> config = null;
    private boolean hasRedstoneActivation = false;
    private Logger logger = null;

    // ===================================================================================
    // Package methods

    void init(QuicksortConfig config, Logger logger) {
        if (this.logger == null) this.logger = requireNonNull(logger);
        updateConfig(config);
    }

    public void updateConfig(QuicksortConfig config) {
        final Map<Identifier, QuicksortChestConfig> updatedConfig = new HashMap<>();
        boolean redstoneActivation = false;
        for (final QuicksortChestConfig chest : config.chestConfigs()) {
            updatedConfig.put(chest.baseBlockId(), chest);
            redstoneActivation |= chest.enableRedstoneActivation();
        }
        this.config = updatedConfig;
        this.hasRedstoneActivation = redstoneActivation;
        if (!this.hasRedstoneActivation) {
            this.redstonePowered.clear();
            this.redstoneNextAttemptTick.clear();
        }
    }

    public void resetRuntimeState() {
        int oldJobs = this.jobs.size();
        int oldChests = this.loadedChests.size();
        for (final QuicksorterJob job : this.jobs) {
            job.discardGhosts();
        }
        this.jobs.clear();
        this.loadedChests.clear();
        this.loadedContainers.clear();
        this.openChests.clear();
        this.redstonePowered.clear();
        this.redstoneNextAttemptTick.clear();
        this.logger.info(LOG_PREFIX + "Runtime state reset. Cleared " + oldJobs + " jobs, " + oldChests + " loaded chests");
    }

    // ===================================================================================
    // Public methods called from mixins

    /**
     * When a chest is closed, start a new job.  Called from the mixin.
     */
    public void onChestClosed(ChestBlockEntity e) {
        final ServerLevel world;
        try {
            world = requireNonNull((ServerLevel)e.getLevel());
        } catch (NullPointerException npe) {
            this.logger.warn(LOG_PREFIX + "onChestClosed: Chest at " + e.getBlockPos() + " has no world attached!");
            return;
        }
        final BlockPos chestPos = e.getBlockPos();
        this.logger.info(LOG_PREFIX + "Chest closed at " + chestPos + " in " + world.dimension());
        this.openChests.remove(LevelChestPos.of(world, chestPos));
        startJob(world, e);
    }

    public void onChestOpened(ChestBlockEntity chestEntity) {
        final ServerLevel world;
        try {
            world = requireNonNull((ServerLevel) chestEntity.getLevel());
        } catch (NullPointerException npe) {
            this.logger.warn(LOG_PREFIX + "onChestOpened: Chest at " + chestEntity.getBlockPos() + " has no world attached!");
            return;
        }
        final BlockPos chestPos = chestEntity.getBlockPos();
        this.logger.info(LOG_PREFIX + "Chest opened at " + chestPos + " in " + world.dimension());
        this.openChests.add(LevelChestPos.of(world, chestPos));
        final QuicksortChestConfig chestConfig = getChestConfigFor(world, chestPos);
        if (chestConfig != null && chestConfig.continueWhileOpen()) {
            this.logger.debug(LOG_PREFIX + "Chest at " + chestPos + " configured to continue while open");
            return;
        }
        stopJobsAt(world, chestPos);
    }

    public void onBlockEntityLoaded(BlockEntity entity, ServerLevel world) {
        if (entity instanceof Container) {
            this.loadedContainers.add(LevelChestPos.of(world, entity.getBlockPos()));
        }
        if (entity instanceof ChestBlockEntity chestEntity) {
            final BlockPos chestPos = chestEntity.getBlockPos();
            final LevelChestPos key = LevelChestPos.of(world, chestPos);
            this.loadedChests.add(key);
            this.redstonePowered.remove(key);
            this.redstoneNextAttemptTick.remove(key);
            this.logger.debug(LOG_PREFIX + "Chest loaded at " + chestPos + " in " + world.dimension() + " (total tracked: " + this.loadedChests.size() + ")");
        }
    }

    public void onBlockEntityUnloaded(BlockEntity entity, ServerLevel world) {
        if (entity instanceof Container) {
            this.loadedContainers.remove(LevelChestPos.of(world, entity.getBlockPos()));
        }
        if (entity instanceof ChestBlockEntity chestEntity) {
            final BlockPos chestPos = chestEntity.getBlockPos();
            final LevelChestPos key = LevelChestPos.of(world, chestPos);
            this.loadedChests.remove(key);
            this.openChests.remove(key);
            this.redstonePowered.remove(key);
            this.redstoneNextAttemptTick.remove(key);
            this.logger.debug(LOG_PREFIX + "Chest unloaded at " + chestPos + " in " + world.dimension() + " (total tracked: " + this.loadedChests.size() + ")");
        }
    }

    // ===================================================================================
    // EndLevelTick implementation

    @Override
    public void onEndTick(ServerLevel world) {
        if (this.hasRedstoneActivation) processRedstoneActivations(world);
        if (this.jobs.isEmpty()) return;
        this.logger.debug(LOG_PREFIX + "onEndTick: Processing " + this.jobs.size() + " active jobs in " + world.dimension());
        Iterator<QuicksorterJob> i = this.jobs.iterator();
        while (i.hasNext()) {
            final QuicksorterJob job = i.next();
            if (job.isDone()) {
                this.logger.info(LOG_PREFIX + "Job completed for chest at " + job.quicksorterPos + " in " + world.dimension());
                i.remove();
            } else {
                if (world.dimension().equals(job.dimension)) {
                    job.tick(world, this.openChests.contains(LevelChestPos.of(world, job.quicksorterPos)));
                }
            }
        }
    }

    // ===================================================================================
    // Private stuff

    private QuicksortChestConfig getChestConfigFor(Level world, BlockPos chestPos) {
        final Block baseBlock = world.getBlockState(chestPos.below()).getBlock();
        final Identifier baseBlockId = BuiltInRegistries.BLOCK.getKey(baseBlock);
        return this.config.get(baseBlockId);
    }

    private boolean startJob(ServerLevel world, ChestBlockEntity chestEntity) {
        final BlockPos chestPos = chestEntity.getBlockPos();
        final QuicksortChestConfig chestConfig = getChestConfigFor(world, chestPos);
        if (chestConfig == null) {
            this.logger.debug(LOG_PREFIX + "Skipping sort job at " + chestPos + ": no config for base block");
            return false;
        }
        if (!chestConfig.continueWhileOpen() && this.openChests.contains(LevelChestPos.of(world, chestPos))) {
            this.logger.debug(LOG_PREFIX + "Skipping sort job at " + chestPos + ": chest is open");
            return false;
        }
        if (hasActiveJob(world, chestPos)) {
            this.logger.debug(LOG_PREFIX + "Skipping sort job at " + chestPos + ": active job already exists");
            return false;
        }
        final Container sourceInventory = SlotJob.getInventoryFor(world, chestPos);
        if (sourceInventory == null || isEmpty(sourceInventory)) {
            this.logger.debug(LOG_PREFIX + "Skipping sort job at " + chestPos + ": source chest is empty or unavailable");
            return false;
        }
        this.logger.info(LOG_PREFIX + "Starting sort job for chest at " + chestPos + " with range=" + chestConfig.range() + ", checkObstructions=" + chestConfig.checkObstructions());
        final List<TargetContainer> visibles = getVisibleChestsNear(world, chestConfig, chestEntity, chestConfig.range());
        if (!visibles.isEmpty()) {
            final QuicksorterJob job = QuicksorterJob.create(world, chestConfig, chestEntity, visibles);
            if (job != null) {
                jobs.add(job);
                this.logger.info(LOG_PREFIX + "Sort job created for chest at " + chestPos + ": " + visibles.size() + " targets, " + job.slotJobs.size() + " slot jobs");
                return true;
            } else {
                this.logger.debug(LOG_PREFIX + "Sort job not created (no valid slots) for chest at " + chestPos);
            }
        } else {
            this.logger.info(LOG_PREFIX + "Sort job skipped for chest at " + chestPos + ": no target containers found");
        }
        return false;
    }

    private boolean hasActiveJob(ServerLevel world, BlockPos chestPos) {
        for (final QuicksorterJob job : this.jobs) {
            if (job.isTransferActive() && world.dimension().equals(job.dimension) && job.quicksorterPos.equals(chestPos)) {
                return true;
            }
        }
        return false;
    }

    private void stopJobsAt(ServerLevel world, BlockPos chestPos) {
        for (final QuicksorterJob job : this.jobs) {
            if (world.dimension().equals(job.dimension) && job.quicksorterPos.equals(chestPos)) job.stop();
        }
    }

    private void processRedstoneActivations(ServerLevel world) {
        int chestsChecked = 0;
        int jobsStarted = 0;
        final long currentTick = world.getServer().getTickCount();
        final Iterator<LevelChestPos> i = this.loadedChests.iterator();
        while (i.hasNext()) {
            final LevelChestPos key = i.next();
            if (!world.dimension().equals(key.dimension())) continue;
            chestsChecked++;
            final BlockEntity entity = world.getBlockEntity(key.pos());
            if (!(entity instanceof ChestBlockEntity chestEntity)) {
                i.remove();
                this.openChests.remove(key);
                this.redstonePowered.remove(key);
                this.redstoneNextAttemptTick.remove(key);
                continue;
            }
            final QuicksortChestConfig chestConfig = getChestConfigFor(world, key.pos());
            if (chestConfig == null || !chestConfig.enableRedstoneActivation()) {
                this.redstonePowered.remove(key);
                this.redstoneNextAttemptTick.remove(key);
                continue;
            }
            final boolean powered = isBasePowered(world, key.pos());
            final Boolean previousPowered = this.redstonePowered.put(key, powered);
            if (previousPowered == null) {
                if (powered) this.redstoneNextAttemptTick.put(key, Long.MAX_VALUE);
                continue;
            }
            final boolean wasPowered = previousPowered;
            this.redstonePowered.put(key, powered);
            if (!powered) {
                this.redstoneNextAttemptTick.remove(key);
                continue;
            }
            if (hasActiveJob(world, key.pos())) continue;
            final long nextAttemptTick = this.redstoneNextAttemptTick.getOrDefault(key, 0L);
            if (!wasPowered || currentTick >= nextAttemptTick) {
                this.logger.info(LOG_PREFIX + "Redstone activation detected for chest at " + key.pos());
                if (startJob(world, chestEntity)) jobsStarted++;
                this.redstoneNextAttemptTick.put(key, currentTick + Math.max(20, chestConfig.cooldownTicks()));
            }
        }
        if (chestsChecked > 0) {
            this.logger.debug(LOG_PREFIX + "Redstone scan: checked=" + chestsChecked + ", jobsStarted=" + jobsStarted + " in " + world.dimension());
        }
    }

    private static boolean isBasePowered(ServerLevel world, BlockPos chestPos) {
        return world.hasNeighborSignal(chestPos.below());
    }

    private static boolean isEmpty(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    private static boolean isPathClear(ServerLevel world, Vec3 origin, Vec3 target) {
        final BlockHitResult result = world.clip(new ClipContext(origin, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(target) < 1.0E-6D;
    }

    @FunctionalInterface
    interface GhostCreator {
        void createGhost(ServerLevel world, Item item, TargetContainer targetContainer);
    }

    private record LevelChestPos(ResourceKey<Level> dimension, BlockPos pos) {
        static LevelChestPos of(ServerLevel world, BlockPos pos) {
            return new LevelChestPos(world.dimension(), pos.immutable());
        }
    }

    /**
     * Manages the offloading of items from a particular quicksorter chest.  There should be at
     * most one of these for each quicksorter chest.
     */
    private static class QuicksorterJob {
        private final BlockPos quicksorterPos;
        private final QuicksortChestConfig quicksorterConfig;
        private final List<TargetContainer> visibleTargets;
        private final List<GhostItemEntity> ghostItems = new ArrayList<>();
        private final List<SlotJob> slotJobs;
        private final ResourceKey<Level> dimension;
        private boolean isTransferComplete = false;
        int tick = 0;

        static QuicksorterJob create(ServerLevel world, QuicksortChestConfig chestConfig, ChestBlockEntity originChest, List<TargetContainer> visibleTargets) {
            final Container originInventory = SlotJob.getInventoryFor(world, originChest.getBlockPos());
            if (originInventory == null) return null;
            final List<SlotJob> slotJobs = new ArrayList<>();
            for (int slot = 0; slot < originInventory.getContainerSize(); slot++) {
                final SlotJob slotJob = SlotJob.create(world, chestConfig, originInventory, originChest.getBlockPos(), slot, visibleTargets);
                if (slotJob != null) slotJobs.add(slotJob);
            }
            return slotJobs.isEmpty() ? null : new QuicksorterJob(chestConfig, world.dimension(), originChest.getBlockPos(), visibleTargets, slotJobs);
        }

        QuicksorterJob(QuicksortChestConfig chestConfig, ResourceKey<Level> dimension, BlockPos quicksorterPos, List<TargetContainer> visibleTargets, List<SlotJob> slotJobs) {
            this.quicksorterConfig = requireNonNull(chestConfig);
            this.dimension = requireNonNull(dimension);
            this.quicksorterPos = requireNonNull(quicksorterPos);
            this.visibleTargets = requireNonNull(visibleTargets);
            this.slotJobs = requireNonNull(slotJobs);
        }

        public boolean isDone() {
            return this.isTransferComplete && this.ghostItems.isEmpty();
        }

        public boolean isTransferActive() {
            return !this.isTransferComplete;
        }

        public void tick(ServerLevel world, boolean quicksorterChestOpen) {
            cleanupGhosts();
            if (!this.isTransferComplete) {
                if (tick++ > this.quicksorterConfig.cooldownTicks()) {
                    tick = 0;
                    refreshSlotJobs(world);
                    if (!doTransferOperation(world)) {
                        refreshSlotJobs(world);
                        if (this.slotJobs.isEmpty() && !(this.quicksorterConfig.continueWhileOpen() && quicksorterChestOpen)) {
                            this.isTransferComplete = true;
                        }
                    }
                }
            }
            cleanupGhosts();
        }

        private void cleanupGhosts() {
            final Iterator<GhostItemEntity> i = this.ghostItems.iterator();
            while (i.hasNext()) {
                if (i.next().isRemoved()) i.remove();
            }
        }

        private void refreshSlotJobs(ServerLevel world) {
            final Container originInventory = SlotJob.getInventoryFor(world, this.quicksorterPos);
            if (originInventory == null) {
                this.slotJobs.clear();
                return;
            }
            final Set<Integer> activeSlots = new HashSet<>();
            for (final SlotJob slotJob : this.slotJobs) {
                activeSlots.add(slotJob.slot());
            }
            for (int slot = 0; slot < originInventory.getContainerSize(); slot++) {
                if (activeSlots.contains(slot)) continue;
                final SlotJob slotJob = SlotJob.create(world, this.quicksorterConfig, originInventory, this.quicksorterPos, slot, this.visibleTargets);
                if (slotJob != null) this.slotJobs.add(slotJob);
            }
        }

        private boolean doTransferOperation(final ServerLevel world) {
            requireNonNull(world);
            if (this.quicksorterConfig.transferMode() == TransferMode.STACKS) {
                return doStackTransferOperation(world);
            }
            return doItemTransferOperation(world);
        }

        private boolean doStackTransferOperation(final ServerLevel world) {
            boolean movedAny = false;
            boolean blockedAny = false;
            int stacksTransferred = 0;
            final List<SlotJob> candidates = new ArrayList<>(this.slotJobs);
            Collections.shuffle(candidates, new Random());
            for (final SlotJob slotJob : candidates) {
                if (stacksTransferred >= this.quicksorterConfig.transferAmount()) break;
                if (!this.slotJobs.contains(slotJob)) continue;
                final TransferResult result = slotJob.doOneStackTransfer(world, this::createGhost);
                if (result.blocked()) {
                    blockedAny = true;
                } else if (!result.success()) {
                    this.slotJobs.remove(slotJob);
                } else {
                    stacksTransferred++;
                    movedAny = true;
                }
            }
            return movedAny || blockedAny;
        }

        private boolean doItemTransferOperation(final ServerLevel world) {
            final Map<Item, Integer> itemsTransferred = new HashMap<>();
            boolean movedAny = false;
            boolean blockedAny = false;
            boolean movedThisPass;
            do {
                movedThisPass = false;
                final Iterator<SlotJob> i = this.slotJobs.iterator();
                while (i.hasNext()) {
                    final SlotJob slotJob = i.next();
                    final Item item = slotJob.getCurrentItem(world);
                    if (item == null) {
                        i.remove();
                        continue;
                    }
                    if (itemsTransferred.getOrDefault(item, 0) >= this.quicksorterConfig.transferAmount()) {
                        continue;
                    }
                    final TransferResult result = slotJob.doOneItemTransfer(world, this::createGhost);
                    if (result.blocked()) {
                        blockedAny = true;
                    } else if (!result.success()) {
                        i.remove();
                    } else {
                        itemsTransferred.merge(result.item(), result.movedCount(), Integer::sum);
                        movedAny = true;
                        movedThisPass = true;
                    }
                }
            } while (movedThisPass && hasRemainingItemBudget(world, itemsTransferred));
            return movedAny || blockedAny;
        }

        private boolean hasRemainingItemBudget(final ServerLevel world, final Map<Item, Integer> itemsTransferred) {
            for (SlotJob slotJob : this.slotJobs) {
                final Item item = slotJob.getCurrentItem(world);
                if (item != null && itemsTransferred.getOrDefault(item, 0) < this.quicksorterConfig.transferAmount()) {
                    return true;
                }
            }
            return false;
        }

        private void createGhost(ServerLevel world, Item item, TargetContainer targetChest) {
            if (this.quicksorterConfig.soundVolume() > 0.0F) {
                world.playSound(
                    null,                            // Player - if non-null, will play sound for every nearby player *except* the specified player
                    this.quicksorterPos,             // The position of where the sound will come from
                        SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS,            // This determines which of the volume sliders affect this sound
                    quicksorterConfig.soundVolume(), // .75f
                    quicksorterConfig.soundPitch()   // 2.0f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
                );
            }
            if (this.quicksorterConfig.animationTicks() <= 0) return;
            if (this.quicksorterConfig.animationMode() == AnimationMode.ENTITY) {
                createEntityGhost(world, item, targetChest);
            } else {
                createParticleGhost(world, item, targetChest);
            }
        }

        private void createParticleGhost(ServerLevel world, Item item, TargetContainer targetChest) {
            world.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, item),
                    targetChest.originItemPos.x(),
                    targetChest.originItemPos.y(),
                    targetChest.originItemPos.z(),
                    0,
                    targetChest.itemVelocity.x(),
                    targetChest.itemVelocity.y(),
                    targetChest.itemVelocity.z(),
                    1.0D);
        }

        private void createEntityGhost(ServerLevel world, Item item, TargetContainer targetChest) {
            final GhostItemEntity itemEntity = new GhostItemEntity(
                    world,
                    targetChest.originItemPos.x(),
                    targetChest.originItemPos.y(),
                    targetChest.originItemPos.z(),
                    new ItemStack(item, 1),
                    this.quicksorterConfig.animationTicks());
            itemEntity.setNoGravity(true);
            itemEntity.setOnGround(false);
            itemEntity.setInvulnerable(true);
            itemEntity.setDeltaMovement(targetChest.itemVelocity);
            if (world.addFreshEntity(itemEntity)) {
                this.ghostItems.add(itemEntity);
            }
        }

        public void stop() {
            this.isTransferComplete = true;
            this.slotJobs.clear();
            discardGhosts();
        }

        public void discardGhosts() {
            for (final GhostItemEntity ghost : this.ghostItems) {
                ghost.discard();
            }
            this.ghostItems.clear();
        }
    }

    private enum TransferStatus {
        MOVED,
        BLOCKED,
        DONE
    }

    private record TransferResult(TransferStatus status, Item item, int movedCount) {

        boolean success() {
            return this.status == TransferStatus.MOVED;
        }

        boolean blocked() {
            return this.status == TransferStatus.BLOCKED;
        }

        static TransferResult none() {
            return new TransferResult(TransferStatus.DONE, null, 0);
        }

        static TransferResult blockedResult() {
            return new TransferResult(TransferStatus.BLOCKED, null, 0);
        }

        static TransferResult moved(Item item, int movedCount) {
            return new TransferResult(TransferStatus.MOVED, requireNonNull(item), movedCount);
        }
    }

    /**
     * Encapsulates the work to move items out of a specific slot in the quicksorter chest.
     */
    private static class SlotJob {
        private final QuicksortChestConfig quicksorterConfig;
        private final BlockPos quicksorterPos;
        private final int slot;
        private final ItemStack sourceStack;
        private final List<TargetContainer> targets;


        static SlotJob create(ServerLevel world, QuicksortChestConfig chestConfig, Container originInventory, BlockPos originPos, int slot, List<TargetContainer> allVisibleContainers) {
            final ItemStack originStack = originInventory.getItem(slot);
            if (originStack == null || originStack.isEmpty()) return null;
            final List<TargetContainer> targetContainers = new ArrayList<>();
            for (TargetContainer visibleContainer : allVisibleContainers) {
                final Container targetInventory = getInventoryFor(world, visibleContainer.blockPos());
                if (targetInventory == null) continue;
                if (isValidTarget(originStack, targetInventory, chestConfig.enchantmentMatchingIds())) {
                    targetContainers.add(visibleContainer);
                }
            }
            return targetContainers.isEmpty() ? null : new SlotJob(chestConfig, originPos, slot, originStack.copy(), targetContainers);
        }

        private SlotJob(QuicksortChestConfig chestConfig, BlockPos quicksorterPos, int slot, ItemStack sourceStack, List<TargetContainer> candidateChests) {
            this.quicksorterConfig = requireNonNull(chestConfig);
            this.quicksorterPos = requireNonNull(quicksorterPos);
            this.sourceStack = requireNonNull(sourceStack);
            this.targets = requireNonNull(candidateChests);
            this.slot = slot;
        }

        int slot() {
            return this.slot;
        }

        Item getCurrentItem(final ServerLevel world) {
            final Container quicksorterInventory = getInventoryFor(world, this.quicksorterPos);
            if (quicksorterInventory == null) return null;
            final ItemStack originStack = quicksorterInventory.getItem(this.slot);
            return isCurrentStackValid(originStack) ? originStack.getItem() : null;
        }

        /**
         * Attempt to transfer one item. Blocked targets are retried on a later cooldown instead of terminating
         * the slot job, because the obstruction may be temporary.
         */
        TransferResult doOneItemTransfer(final ServerLevel world, final GhostCreator gc) {
            requireNonNull(world, "null gc");
            final Container quicksorterInventory = getInventoryFor(world, this.quicksorterPos);
            if (quicksorterInventory == null) {
                return TransferResult.none(); // quicksorter presumably was destroyed
            }
            final ItemStack originStack = quicksorterInventory.getItem(this.slot);
            if (!isCurrentStackValid(originStack)) return TransferResult.none();
            boolean blockedAny = false;
            final List<TargetContainer> candidates = new ArrayList<>(this.targets);
            Collections.shuffle(candidates, new Random());
            for (final TargetContainer candidate : candidates) {
                if (!this.targets.contains(candidate)) continue;
                final ItemStack copy = originStack.copy();
                final Item ghostItem = copy.getItem();
                copy.setCount(1);
                final Container targetInventory = getInventoryFor(world, candidate.blockPos());
                if (targetInventory == null) {
                    this.targets.remove(candidate); // target has probably been destroyed
                    continue;
                }
                if (!isCandidatePathClear(world, candidate)) {
                    blockedAny = true;
                    continue;
                }
                final ItemStack itemStack2 = HopperBlockEntity.addItem((Container) null, targetInventory, copy, (Direction) null);
                if (!itemStack2.isEmpty()) {
                    this.targets.remove(candidate); // target is full
                    continue;
                }
                // ok, we successfully transferred an item.  the minecraft code doesn't do the bookkeeping
                // for us on the origin chest, so:
                originStack.shrink(1);
                quicksorterInventory.setChanged();
                gc.createGhost(world, ghostItem, candidate);
                return TransferResult.moved(ghostItem, 1);
            }
            return blockedAny ? TransferResult.blockedResult() : TransferResult.none();
        }

        /**
         * Attempt to transfer one source stack. If the first target only has partial space, keep trying other targets
         * as part of the same stack transfer.
         */
        TransferResult doOneStackTransfer(final ServerLevel world, final GhostCreator gc) {
            requireNonNull(world, "null gc");
            final Container quicksorterInventory = getInventoryFor(world, this.quicksorterPos);
            if (quicksorterInventory == null) {
                return TransferResult.none(); // quicksorter presumably was destroyed
            }
            final ItemStack originStack = quicksorterInventory.getItem(this.slot);
            if (!isCurrentStackValid(originStack)) return TransferResult.none();
            final Item movedItem = originStack.getItem();
            ItemStack remaining = originStack.copy();
            int movedCount = 0;
            boolean blockedAny = false;
            final List<TargetContainer> candidates = new ArrayList<>(this.targets);
            Collections.shuffle(candidates, new Random());
            for (final TargetContainer candidate : candidates) {
                if (remaining.isEmpty()) break;
                if (!this.targets.contains(candidate)) continue;
                final Container targetInventory = getInventoryFor(world, candidate.blockPos());
                if (targetInventory == null) {
                    this.targets.remove(candidate); // target has probably been destroyed
                    continue;
                }
                if (!isCandidatePathClear(world, candidate)) {
                    blockedAny = true;
                    continue;
                }
                final int before = remaining.getCount();
                final ItemStack notMoved = HopperBlockEntity.addItem((Container) null, targetInventory, remaining.copy(), (Direction) null);
                final int movedToTarget = before - notMoved.getCount();
                if (movedToTarget <= 0) {
                    this.targets.remove(candidate); // target is full
                    continue;
                }
                movedCount += movedToTarget;
                remaining = notMoved;
                gc.createGhost(world, movedItem, candidate);
                if (!remaining.isEmpty()) {
                    this.targets.remove(candidate); // no more space in this target for the rest of this stack
                }
            }
            if (movedCount <= 0) return blockedAny ? TransferResult.blockedResult() : TransferResult.none();
            originStack.shrink(movedCount);
            quicksorterInventory.setChanged();
            return TransferResult.moved(movedItem, movedCount);
        }

        private boolean isCandidatePathClear(ServerLevel world, TargetContainer candidate) {
            return !this.quicksorterConfig.checkObstructions() ||
                    QuicksortService.isPathClear(world, candidate.originItemPos(), candidate.targetItemPos());
        }

        private boolean isCurrentStackValid(ItemStack stack) {
            return !stack.isEmpty() && isMatch(this.sourceStack, stack, this.quicksorterConfig.enchantmentMatchingIds());
        }

        /**
         * @return the Inventory at the given block position, or null if there is none.  For double chests, this will
         * return an appropriate DoubleInventory.
         */
        private static Container getInventoryFor(ServerLevel world, BlockPos blockPos) {
            requireNonNull(world, "null world");
            requireNonNull(blockPos, "null blockPos");
            final BlockEntity entity = world.getBlockEntity(blockPos);
            if (entity instanceof Container inventory) {
                if (entity instanceof ChestBlockEntity) { // this seems pretty unlikely but let's be careful
                    final BlockState blockState = world.getBlockState(blockPos);
                    final Block block = blockState.getBlock();
                    return getContainer((ChestBlock)block, blockState, world, blockPos, true);
                } else {
                    return inventory;
                }
            }
            return null;
         }

        /**
         * @return true if the given targetInventory can accept items from the given stack.
         */
        private static boolean isValidTarget(ItemStack originStack, Container targetInventory, Collection<Identifier> enchantmentMatchingIds) {
            requireNonNull(targetInventory, "inventory");
            requireNonNull(originStack, "item");
            Integer firstEmptySlot = null;
            boolean hasMatchingItem = false;
            for (int slot = 0; slot < targetInventory.getContainerSize(); slot++) {
                ItemStack targetStack = requireNonNull(targetInventory.getItem(slot));
                if (targetStack.isEmpty()) {
                    if (hasMatchingItem) return true; // this one's empty and a match was found earlier. done.
                    if (firstEmptySlot == null) firstEmptySlot = slot; // else remember this empty slot
                } else if (isMatch(originStack, targetStack, enchantmentMatchingIds)) {
                    if (firstEmptySlot != null) return true;
                    if (!isFull(targetStack)) return true;
                    hasMatchingItem = true;
                }
            }
            return false;
        }

        /**
         * @return if the two items are of the same type and, if the item is contained in enchantmentMatchingIds, the
         * stacks have the same enchantments.
         */
        private static boolean isMatch(ItemStack first, ItemStack second, Collection<Identifier> enchantmentMatchEnabledIds) {
            return first.is(second.getItem()) &&
                    (!enchantmentMatchEnabledIds.contains(BuiltInRegistries.ITEM.getKey(first.getItem())) ||
                            areEnchantmentsEqual(first, second));
        }

        /**
         * @return whether the enchantments on the two items are equal.  The implementation here elides distinctions
         * between different kinds of enchantments that minecraft now maintains.  But this is probably aligned with
         * what most folks will expect.
         */
        private static boolean areEnchantmentsEqual(ItemStack left, ItemStack right) {
            if (left.isEmpty() && right.isEmpty()) {
                return true;
            } else if (!left.isEmpty() && !right.isEmpty()) {
                return left.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).equals(
                        right.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)) &&
                        left.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).equals(
                                right.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)) &&
                        left.getOrDefault(DataComponents.POTION_CONTENTS, ItemEnchantments.EMPTY).equals(
                                right.getOrDefault(DataComponents.POTION_CONTENTS, ItemEnchantments.EMPTY));
            } else {
                return false;
            }
        }

        /**
         * @return true if the given stack has as many items as are allowed for the stacked item type.
         */
        private static boolean isFull(ItemStack stack) {
            return stack.getCount() == stack.getMaxStackSize();
        }
    }

    private record TargetContainer(
            BlockPos blockPos,     // position of the container
            Vec3 originItemPos,   // coordinates where the GhostItem should appear outside the origin container
            Vec3 targetItemPos,   // coordinates the GhostItem should travel to outside the target container
            Vec3 itemVelocity     // how fast the GhostItem should travel
    ) {
    }

    private List<TargetContainer> getVisibleChestsNear(ServerLevel world, QuicksortChestConfig chestConfig,ChestBlockEntity originChest, int distance) {
        final long startTime = System.currentTimeMillis();
        final BlockPos originPos = originChest.getBlockPos();
        this.logger.info(LOG_PREFIX + "[PERF] Scanning for targets: origin=" + originPos + ", range=" + distance + ", checkObstructions=" + chestConfig.checkObstructions());

        List<TargetContainer> out = new ArrayList<>();
        int containersScanned = 0;
        int unloadedContainers = 0;
        final List<LevelChestPos> candidates = this.loadedContainers.stream()
                .filter(key -> world.dimension().equals(key.dimension()))
                .sorted((left, right) -> left.pos().compareTo(right.pos()))
                .toList();
        for (final LevelChestPos candidate : candidates) {
            final BlockPos targetPos = candidate.pos();
            if (targetPos.equals(originPos)) continue;
            if (!isWithinCubeRange(originPos, targetPos, distance)) continue;
            containersScanned++;

            if (!world.hasChunkAt(targetPos)) {
                unloadedContainers++;
                this.logger.debug(LOG_PREFIX + "[PERF] Skipping unloaded container at " + targetPos);
                continue;
            }

            final BlockState targetState = world.getBlockState(targetPos);
            final Block targetBlock = targetState.getBlock();
            final Identifier targetId = BuiltInRegistries.BLOCK.getKey(targetBlock);
            if (!chestConfig.targetContainerIds().contains(targetId)) continue;
            if (targetBlock instanceof ChestBlock && getChestConfigFor(world, targetPos) != null) {
                continue; // skip other sorting chests
            }

            final Vec3 origin = getTransferPoint(originChest.getBlockPos(), targetPos);
            final Vec3 target = getTransferPoint(targetPos, originChest.getBlockPos());

            if (chestConfig.checkObstructions() && !isPathClear(world, origin, target)) continue;
            this.logger.debug(LOG_PREFIX + "Target container found at " + targetPos);
            Vec3 itemVelocity = chestConfig.animationTicks() > 0 ?
                    new Vec3(
                            (target.x() - origin.x()) / chestConfig.animationTicks(),
                            (target.y() - origin.y()) / chestConfig.animationTicks(),
                            (target.z() - origin.z()) / chestConfig.animationTicks()) :
                    Vec3.ZERO;
            out.add(new TargetContainer(targetPos, origin, target, itemVelocity));
        }

        final long elapsed = System.currentTimeMillis() - startTime;
        this.logger.info(LOG_PREFIX + "[PERF] getVisibleChestsNear: completed in " + elapsed + "ms, containersScanned=" + containersScanned + ", unloadedContainers=" + unloadedContainers + ", targetsFound=" + out.size());
        if (elapsed > 1000) {
            this.logger.warn(LOG_PREFIX + "[PERF] Scan took " + elapsed + "ms! Consider reducing range (currently " + distance + ")");
        }
        return out;
    }

    private static boolean isWithinCubeRange(BlockPos origin, BlockPos target, int distance) {
        return Math.abs(target.getX() - origin.getX()) <= distance &&
                Math.abs(target.getY() - origin.getY()) <= distance &&
                Math.abs(target.getZ() - origin.getZ()) <= distance;
    }

    private static Vec3 getTransferPoint(BlockPos origin, BlockPos target) {
        Vec3 origin3d = Vec3.atCenterOf(origin);
        Vec3 target3d = Vec3.atCenterOf(target);
        Vec3 vector = target3d.subtract(origin3d).normalize();
        return origin3d.add(vector).add(0, -0.5D, 0);
    }
}
