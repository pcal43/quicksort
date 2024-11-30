package net.pcal.quicksort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import static net.minecraft.world.level.block.ChestBlock.getContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.pcal.quicksort.QuicksortConfig.QuicksortChestConfig;

/**
 * Singleton that makes the quicksorter chests do their thing.
 */
public class QuicksortService implements ServerTickEvents.EndWorldTick {

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
    private Map<ResourceLocation, QuicksortChestConfig> config = null;
    private Logger logger = null;

    // ===================================================================================
    // Package methods

    void init(QuicksortConfig config, Logger logger) {
        if (this.logger != null) throw new IllegalStateException();
        this.logger = requireNonNull(logger);
        if (this.config != null) throw new IllegalStateException();
        this.config = new HashMap<>();
        for (final QuicksortChestConfig chest : config.chestConfigs()) {
            this.config.put(chest.baseBlockId(), chest);
        }
    }

    // ===================================================================================
    // Public methods called from mixins

    /**
     * When a chest is closed, start a new job.  Called from the mixin.
     */
    public void onChestClosed(ChestBlockEntity e) {
        final ServerLevel world = requireNonNull((ServerLevel)e.getLevel());
        final QuicksortChestConfig chestConfig = getChestConfigFor(world, e.getBlockPos());
        if (chestConfig != null) {
            final List<TargetContainer> visibles = getVisibleChestsNear(world, chestConfig, e, chestConfig.range());
            if (!visibles.isEmpty()) {
                final QuicksorterJob job = QuicksorterJob.create(world, chestConfig, e, visibles);
                if (job != null) jobs.add(job);
            }
        }
    }

    /**
     * When a chest is opened, we stop any jobs running on it (there really should be at most one).
     * Called from the mixin.
     */
    public void onChestOpened(ChestBlockEntity chestEntity) {
        for (final QuicksorterJob job : this.jobs) {
            if (job.quicksorterPos.equals(chestEntity.getBlockPos())) job.stop();
        }
    }

    // ===================================================================================
    // EndWorldTick implementation

    @Override
    public void onEndTick(ServerLevel world) {
        if (this.jobs.isEmpty()) return;
        Iterator<QuicksorterJob> i = this.jobs.iterator();
        while (i.hasNext()) {
            final QuicksorterJob job = i.next();
            if (job.isDone()) {
                this.logger.debug(LOG_PREFIX + " job completed for chest at " + job.quicksorterPos);
                i.remove();
            } else {
                if (world.dimensionType().equals(job.dimension)) {
                    job.tick(world);
                }
            }
        }
    }

    // ===================================================================================
    // Private stuff

    private QuicksortChestConfig getChestConfigFor(Level world, BlockPos chestPos) {
        final Block baseBlock = world.getBlockState(chestPos.below()).getBlock();
        final ResourceLocation baseBlockId = BuiltInRegistries.BLOCK.getKey(baseBlock);
        return this.config.get(baseBlockId);
    }

    @FunctionalInterface
    interface GhostCreator {
        void createGhost(ServerLevel world, Item item, TargetContainer targetContainer);
    }

    /**
     * Manages the offloading of items from a particular quicksorter chest.  There should be at
     * most one of these for each quicksorter chest.
     */
    private static class QuicksorterJob {
        private final BlockPos quicksorterPos;
        private final QuicksortChestConfig quicksorterConfig;
        private final List<GhostItemEntity> ghostItems = new ArrayList<>();
        private final List<SlotJob> slotJobs;
        private final DimensionType dimension;
        private boolean isTransferComplete = false;
        int tick = 0;

        static QuicksorterJob create(ServerLevel world, QuicksortChestConfig chestConfig, ChestBlockEntity originChest, List<TargetContainer> visibleTargets) {
            final List<SlotJob> slotJobs = new ArrayList<>();
            for (int slot = 0; slot < originChest.getContainerSize(); slot++) {
                final SlotJob slotJob = SlotJob.create(world, chestConfig, originChest, slot, visibleTargets);
                if (slotJob != null) slotJobs.add(slotJob);
            }
            return slotJobs.isEmpty() ? null : new QuicksorterJob(chestConfig, world.dimensionType(), originChest.getBlockPos(), slotJobs);
        }

        QuicksorterJob(QuicksortChestConfig chestConfig, DimensionType dimension, BlockPos quicksorterPos, List<SlotJob> slotJobs) {
            this.quicksorterConfig = requireNonNull(chestConfig);
            this.dimension = requireNonNull(dimension);
            this.quicksorterPos = requireNonNull(quicksorterPos);
            this.slotJobs = requireNonNull(slotJobs);
        }

        public boolean isDone() {
            return this.isTransferComplete && this.ghostItems.isEmpty();
        }

        public void tick(ServerLevel world) {
            final Iterator<GhostItemEntity> g = this.ghostItems.iterator();
            while (g.hasNext()) {
                final GhostItemEntity e = g.next();
                if (e.getAge() > this.quicksorterConfig.animationTicks()) {
                    e.makeFakeItem();
                    g.remove();
                }
            }
            if (!this.isTransferComplete) {
                if (tick++ > this.quicksorterConfig.cooldownTicks()) {
                    tick = 0;
                    if (!doOneTransfer(world)) this.isTransferComplete = true;
                }
            }
        }

        private boolean doOneTransfer(final ServerLevel world) {
            requireNonNull(world);
            SlotJob slotJob;
            do {
                final int jobIndex = new Random().nextInt(this.slotJobs.size());
                slotJob = this.slotJobs.get(jobIndex);
                if (!slotJob.doOneTransfer(world, this::createGhost)) {
                    this.slotJobs.remove(jobIndex);
                } else {
                    return true;
                }
            } while (!slotJobs.isEmpty());
            return false;
        }

        private void createGhost(ServerLevel world, Item item, TargetContainer targetChest) {
            world.playSound(
                    null,                            // Player - if non-null, will play sound for every nearby player *except* the specified player
                    this.quicksorterPos,             // The position of where the sound will come from
                    SoundEvents.UI_TOAST_OUT,        // The sound that will play, in this case, the sound the anvil plays when it lands.
                    SoundSource.BLOCKS,            // This determines which of the volume sliders affect this sound
                    quicksorterConfig.soundVolume(), // .75f
                    quicksorterConfig.soundPitch()   // 2.0f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
            );
            ItemStack ghostStack = new ItemStack(item, 1);
            GhostItemEntity itemEntity = new GhostItemEntity(world,
                    targetChest.originItemPos.x(), targetChest.originItemPos.y(), targetChest.originItemPos.z(), ghostStack);
            this.ghostItems.add(itemEntity);
            itemEntity.setNoGravity(true);
            itemEntity.setOnGround(false);
            itemEntity.setInvulnerable(true);
            itemEntity.setDeltaMovement(targetChest.itemVelocity);
            world.addFreshEntity(itemEntity);
        }

        public void stop() {
            this.isTransferComplete = true;
            this.slotJobs.clear();
        }
    }


    /**
     * Encapsulates the work to move items out of a specific slot in the quicksorter chest.
     */
    private static class SlotJob {
        private final QuicksortChestConfig quicksorterConfig;
        private final BlockPos quicksorterPos;
        private final int slot;
        private final List<TargetContainer> targets;


        static SlotJob create(ServerLevel world, QuicksortChestConfig chestConfig, RandomizableContainerBlockEntity originChest, int slot, List<TargetContainer> allVisibleContainers) {
            final ItemStack originStack = originChest.getItem(slot);
            if (originStack == null || originStack.isEmpty()) return null;
            final List<TargetContainer> targetContainers = new ArrayList<>();
            for (TargetContainer visibleContainer : allVisibleContainers) {
                final Container targetInventory = getInventoryFor(world, visibleContainer.blockPos());
                if (targetInventory == null) continue;
                if (isValidTarget(originStack, targetInventory, chestConfig.nbtMatchEnabledIds())) {
                    targetContainers.add(visibleContainer);
                }
            }
            return targetContainers.isEmpty() ? null : new SlotJob(chestConfig, originChest.getBlockPos(), slot, targetContainers);
        }

        private SlotJob(QuicksortChestConfig chestConfig, BlockPos quicksorterPos, int slot, List<TargetContainer> candidateChests) {
            this.quicksorterConfig = requireNonNull(chestConfig);
            this.quicksorterPos = requireNonNull(quicksorterPos);
            this.targets = requireNonNull(candidateChests);
            this.slot = slot;
        }

        /**
         * Attempt to transfer one item, creating an appropriate GhostEntity.  Return true if successful; if not,
         * false is returned with the expectation that the SlotJob will then be terminated.
         */
        boolean doOneTransfer(final ServerLevel world, final GhostCreator gc) {
            requireNonNull(world, "null gc");
            final Container quicksorterInventory = getInventoryFor(world, this.quicksorterPos);
            if (quicksorterInventory == null) {
                return false; // quicksorter presumably was destroyed
            }
            final ItemStack originStack = quicksorterInventory.getItem(this.slot);
            if (originStack.isEmpty()) return false;
            while (!this.targets.isEmpty()) {
                final ItemStack copy = originStack.copy();
                final Item ghostItem = copy.getItem();
                copy.setCount(1);
                final int candidateIndex = new Random().nextInt(this.targets.size());
                final TargetContainer candidate = this.targets.get(candidateIndex);
                final Container targetInventory = getInventoryFor(world, candidate.blockPos());
                if (targetInventory == null) {
                    this.targets.remove(candidateIndex); // target has probably been destroyed
                    continue;
                }
                final ItemStack itemStack2 = HopperBlockEntity.addItem((Container) null, targetInventory, copy, (Direction) null);
                if (!itemStack2.isEmpty()) {
                    this.targets.remove(candidateIndex); // target is full
                    continue;
                }
                // ok, we successfully transferred an item.  the minecraft code doesn't do the bookkeeping
                // for us on the origin chest, so:
                originStack.shrink(1);
                quicksorterInventory.setChanged();
                if (this.quicksorterConfig.animationTicks() > 0) {
                    // create some animation (after the fact but whatever)
                    gc.createGhost(world, ghostItem, candidate);
                }
                return true;
            }
            return false;
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
        private static boolean isValidTarget(ItemStack originStack, Container targetInventory, Collection<ResourceLocation> nbtMatchEnabledIds) {
            requireNonNull(targetInventory, "inventory");
            requireNonNull(originStack, "item");
            Integer firstEmptySlot = null;
            boolean hasMatchingItem = false;
            for (int slot = 0; slot < targetInventory.getContainerSize(); slot++) {
                ItemStack targetStack = requireNonNull(targetInventory.getItem(slot));
                if (targetStack.isEmpty()) {
                    if (hasMatchingItem) return true; // this one's empty and a match was found earlier. done.
                    if (firstEmptySlot == null) firstEmptySlot = slot; // else remember this empty slot
                } else if (isMatch(originStack, targetStack, nbtMatchEnabledIds)) {
                    if (firstEmptySlot != null) return true;
                    if (!isFull(targetStack)) return true;
                    hasMatchingItem = true;
                }
            }
            return false;
        }

        private static boolean isMatch(ItemStack first, ItemStack second, Collection<ResourceLocation> nbtMatchEnabledIds) {
            return first.is(second.getItem()) &&
                    (!nbtMatchEnabledIds.contains(BuiltInRegistries.ITEM.getKey(first.getItem())) ||
                            areNbtEqual(first, second));
        }

        private static boolean isFull(ItemStack stack) {
            return stack.getCount() == stack.getMaxStackSize();
        }

        private static boolean areNbtEqual(ItemStack left, ItemStack right) {
            if (left.isEmpty() && right.isEmpty()) {
                return true;
            } else if (!left.isEmpty() && !right.isEmpty()) {
                final TagKey<Item> leftTag = left.getTags().findFirst().orElse(null);
                final TagKey<Item> rightTag = right.getTags().findFirst().orElse(null);
                if (leftTag == null && rightTag != null) {
                    return false;
                } else {
                    return leftTag == null || leftTag.equals(rightTag);
                }
            } else {
                return false;
            }
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
        final GhostItemEntity itemEntity = new GhostItemEntity(world, 0, 0, 0, new ItemStack(Blocks.COBBLESTONE));
        List<TargetContainer> out = new ArrayList<>();
        for (int d = originChest.getBlockPos().getX() - distance; d <= originChest.getBlockPos().getX() + distance; d++) {
            for (int e = originChest.getBlockPos().getY() - distance; e <= originChest.getBlockPos().getY() + distance; e++) {
                for (int f = originChest.getBlockPos().getZ() - distance; f <= originChest.getBlockPos().getZ() + distance; f++) {
                    if (d == originChest.getBlockPos().getX() && e == originChest.getBlockPos().getY() && f == originChest.getBlockPos().getZ())
                        continue;
                    final BlockPos targetPos = new BlockPos(d, e, f);
                    final BlockState targetState = world.getBlockState(new BlockPos(d, e, f));
                    final Block targetBlock = targetState.getBlock();
                    final ResourceLocation targetId = BuiltInRegistries.BLOCK.getKey(targetBlock);
                    if (!chestConfig.targetContainerIds().contains(targetId)) continue;
                    if (targetBlock instanceof ChestBlock && getChestConfigFor(world, targetPos) != null) {
                        continue; // skip other sorting chests
                    }

                    final Vec3 origin = getTransferPoint(originChest.getBlockPos(), targetPos);
                    final Vec3 target = getTransferPoint(targetPos, originChest.getBlockPos());

                    BlockHitResult result = world.clip(new ClipContext(origin, target,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, itemEntity));
                    if (result.getLocation().equals(target)) {
                        this.logger.debug(() -> LOG_PREFIX + " visible chest found at " + result.getBlockPos() + " " + targetPos);
                        Vec3 itemVelocity = new Vec3(
                                (target.x() - origin.x()) / chestConfig.animationTicks(),
                                (target.y() - origin.y()) / chestConfig.animationTicks(),
                                (target.z() - origin.z()) / chestConfig.animationTicks());
                        out.add(new TargetContainer(targetPos, origin, target, itemVelocity));
                    }
                }
            }
        }
        return out;
    }

    private static Vec3 getTransferPoint(BlockPos origin, BlockPos target) {
        Vec3 origin3d = Vec3.atCenterOf(origin);
        Vec3 target3d = Vec3.atCenterOf(target);
        Vec3 vector = target3d.subtract(origin3d).normalize();
        return origin3d.add(vector).add(0, -0.5D, 0);
    }
}
