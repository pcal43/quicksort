package net.pcal.quicksort;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.pcal.quicksort.QuicksortConfig.QuicksortChestConfig;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.Objects.requireNonNull;
import static net.minecraft.block.ChestBlock.getInventory;

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
    private Map<Identifier, QuicksortChestConfig> config = null;
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
        final ServerWorld world = requireNonNull((ServerWorld)e.getWorld());
        final QuicksortChestConfig chestConfig = getChestConfigFor(world, e.getPos());
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
            if (job.quicksorterPos.equals(chestEntity.getPos())) job.stop();
        }
    }

    // ===================================================================================
    // EndWorldTick implementation

    @Override
    public void onEndTick(ServerWorld world) {
        if (this.jobs.isEmpty()) return;
        Iterator<QuicksorterJob> i = this.jobs.iterator();
        while (i.hasNext()) {
            final QuicksorterJob job = i.next();
            if (job.isDone()) {
                this.logger.debug(LOG_PREFIX + " job completed for chest at " + job.quicksorterPos);
                i.remove();
            } else {
                if (world.getDimension().equals(job.dimension)) {
                    job.tick(world);
                }
            }
        }
    }

    // ===================================================================================
    // Private stuff

    private QuicksortChestConfig getChestConfigFor(ChestBlockEntity chest) {
        return getChestConfigFor(chest.getWorld(), chest.getPos());
    }

    private QuicksortChestConfig getChestConfigFor(World world, BlockPos chestPos) {
        final Block baseBlock = world.getBlockState(chestPos.down()).getBlock();
        final Identifier baseBlockId = Registries.BLOCK.getId(baseBlock);
        return this.config.get(baseBlockId);
    }

    @FunctionalInterface
    interface GhostCreator {
        void createGhost(ServerWorld world, Item item, TargetContainer targetContainer);
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

        static QuicksorterJob create(ServerWorld world, QuicksortChestConfig chestConfig, ChestBlockEntity originChest, List<TargetContainer> visibleTargets) {
            final List<SlotJob> slotJobs = new ArrayList<>();
            for (int slot = 0; slot < originChest.size(); slot++) {
                final SlotJob slotJob = SlotJob.create(world, chestConfig, originChest, slot, visibleTargets);
                if (slotJob != null) slotJobs.add(slotJob);
            }
            return slotJobs.isEmpty() ? null : new QuicksorterJob(chestConfig, world.getDimension(), originChest.getPos(), slotJobs);
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

        public void tick(ServerWorld world) {
            final Iterator<GhostItemEntity> g = this.ghostItems.iterator();
            while (g.hasNext()) {
                final GhostItemEntity e = g.next();
                if (e.getItemAge() > this.quicksorterConfig.animationTicks()) {
                    e.setDespawnImmediately();
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

        private boolean doOneTransfer(final ServerWorld world) {
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

        private void createGhost(ServerWorld world, Item item, TargetContainer targetChest) {
            world.playSound(
                    null,                            // Player - if non-null, will play sound for every nearby player *except* the specified player
                    this.quicksorterPos,             // The position of where the sound will come from
                    SoundEvents.UI_TOAST_OUT,        // The sound that will play, in this case, the sound the anvil plays when it lands.
                    SoundCategory.BLOCKS,            // This determines which of the volume sliders affect this sound
                    quicksorterConfig.soundVolume(), // .75f
                    quicksorterConfig.soundPitch()   // 2.0f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
            );
            ItemStack ghostStack = new ItemStack(item, 1);
            GhostItemEntity itemEntity = new GhostItemEntity(world,
                    targetChest.originItemPos.getX(), targetChest.originItemPos.getY(), targetChest.originItemPos.getZ(), ghostStack);
            this.ghostItems.add(itemEntity);
            itemEntity.setNoGravity(true);
            itemEntity.setOnGround(false);
            itemEntity.setInvulnerable(true);
            itemEntity.setVelocity(targetChest.itemVelocity);
            world.spawnEntity(itemEntity);
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


        static SlotJob create(ServerWorld world, QuicksortChestConfig chestConfig, LootableContainerBlockEntity originChest, int slot, List<TargetContainer> allVisibleContainers) {
            final ItemStack originStack = originChest.getStack(slot);
            if (originStack == null || originStack.isEmpty()) return null;
            final List<TargetContainer> targetContainers = new ArrayList<>();
            for (TargetContainer visibleContainer : allVisibleContainers) {
                final Inventory targetInventory = getInventoryFor(world, visibleContainer.blockPos());
                if (targetInventory == null) continue;
                if (isValidTarget(originStack, targetInventory, chestConfig.nbtMatchEnabledIds())) {
                    targetContainers.add(visibleContainer);
                }
            }
            return targetContainers.isEmpty() ? null : new SlotJob(chestConfig, originChest.getPos(), slot, targetContainers);
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
        boolean doOneTransfer(final ServerWorld world, final GhostCreator gc) {
            requireNonNull(world, "null gc");
            final Inventory quicksorterInventory = getInventoryFor(world, this.quicksorterPos);
            if (quicksorterInventory == null) {
                return false; // quicksorter presumably was destroyed
            }
            final ItemStack originStack = quicksorterInventory.getStack(this.slot);
            if (originStack.isEmpty()) return false;
            while (!this.targets.isEmpty()) {
                final ItemStack copy = originStack.copy();
                final Item ghostItem = copy.getItem();
                copy.setCount(1);
                final int candidateIndex = new Random().nextInt(this.targets.size());
                final TargetContainer candidate = this.targets.get(candidateIndex);
                final Inventory targetInventory = getInventoryFor(world, candidate.blockPos());
                if (targetInventory == null) {
                    this.targets.remove(candidateIndex); // target has probably been destroyed
                    continue;
                }
                final ItemStack itemStack2 = HopperBlockEntity.transfer((Inventory) null, targetInventory, copy, (Direction) null);
                if (!itemStack2.isEmpty()) {
                    this.targets.remove(candidateIndex); // target is full
                    continue;
                }
                // ok, we successfully transferred an item.  the minecraft code doesn't do the bookkeeping
                // for us on the origin chest, so:
                originStack.decrement(1);
                quicksorterInventory.markDirty();
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
        private static Inventory getInventoryFor(ServerWorld world, BlockPos blockPos) {
            requireNonNull(world, "null world");
            requireNonNull(blockPos, "null blockPos");
            final BlockEntity entity = world.getBlockEntity(blockPos);
            if (entity instanceof Inventory inventory) {
                if (entity instanceof ChestBlockEntity) { // this seems pretty unlikely but let's be careful
                    final BlockState blockState = world.getBlockState(blockPos);
                    final Block block = blockState.getBlock();
                    return getInventory((ChestBlock)block, blockState, world, blockPos, true);
                } else {
                    return inventory;
                }
            }
            return null;
         }

        /**
         * @return true if the given targetInventory can accept items from the given stack.
         */
        private static boolean isValidTarget(ItemStack originStack, Inventory targetInventory, Collection<Identifier> nbtMatchEnabledIds) {
            requireNonNull(targetInventory, "inventory");
            requireNonNull(originStack, "item");
            Integer firstEmptySlot = null;
            boolean hasMatchingItem = false;
            for (int slot = 0; slot < targetInventory.size(); slot++) {
                ItemStack targetStack = requireNonNull(targetInventory.getStack(slot));
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

        private static boolean isMatch(ItemStack first, ItemStack second, Collection<Identifier> nbtMatchEnabledIds) {
            return first.isOf(second.getItem()) &&
                    (!nbtMatchEnabledIds.contains(Registries.ITEM.getId(first.getItem())) ||
                            areNbtEqual(first, second));
        }

        private static boolean isFull(ItemStack stack) {
            return stack.getCount() == stack.getMaxCount();
        }

        private static boolean areNbtEqual(ItemStack left, ItemStack right) {
            if (left.isEmpty() && right.isEmpty()) {
                return true;
            } else if (!left.isEmpty() && !right.isEmpty()) {
                if (left.getNbt() == null && right.getNbt() != null) {
                    return false;
                } else {
                    return left.getNbt() == null || left.getNbt().equals(right.getNbt());
                }
            } else {
                return false;
            }
        }
    }

    private record TargetContainer(
            BlockPos blockPos,     // position of the container
            Vec3d originItemPos,   // coordinates where the GhostItem should appear outside the origin container
            Vec3d targetItemPos,   // coordinates the GhostItem should travel to outside the target container
            Vec3d itemVelocity     // how fast the GhostItem should travel
    ) {
    }

    private List<TargetContainer> getVisibleChestsNear(ServerWorld world, QuicksortChestConfig chestConfig,ChestBlockEntity originChest, int distance) {
        final GhostItemEntity itemEntity = new GhostItemEntity(world, 0, 0, 0, new ItemStack(Blocks.COBBLESTONE));
        List<TargetContainer> out = new ArrayList<>();
        for (int d = originChest.getPos().getX() - distance; d <= originChest.getPos().getX() + distance; d++) {
            for (int e = originChest.getPos().getY() - distance; e <= originChest.getPos().getY() + distance; e++) {
                for (int f = originChest.getPos().getZ() - distance; f <= originChest.getPos().getZ() + distance; f++) {
                    if (d == originChest.getPos().getX() && e == originChest.getPos().getY() && f == originChest.getPos().getZ())
                        continue;
                    final BlockPos targetPos = new BlockPos(d, e, f);
                    final BlockState targetState = world.getBlockState(new BlockPos(d, e, f));
                    final Block targetBlock = targetState.getBlock();
                    final Identifier targetId = Registries.BLOCK.getId(targetBlock);
                    if (!chestConfig.targetContainerIds().contains(targetId)) continue;
                    if (targetBlock instanceof ChestBlock && getChestConfigFor(world, targetPos) != null) {
                        continue; // skip other sorting chests
                    }

                    final Vec3d origin = getTransferPoint(originChest.getPos(), targetPos);
                    final Vec3d target = getTransferPoint(targetPos, originChest.getPos());

                    BlockHitResult result = world.raycast(new RaycastContext(origin, target,
                            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, itemEntity));
                    if (result.getPos().equals(target)) {
                        this.logger.debug(() -> LOG_PREFIX + " visible chest found at " + result.getBlockPos() + " " + targetPos);
                        Vec3d itemVelocity = new Vec3d(
                                (target.getX() - origin.getX()) / chestConfig.animationTicks(),
                                (target.getY() - origin.getY()) / chestConfig.animationTicks(),
                                (target.getZ() - origin.getZ()) / chestConfig.animationTicks());
                        out.add(new TargetContainer(targetPos, origin, target, itemVelocity));
                    }
                }
            }
        }
        return out;
    }

    private static Vec3d getTransferPoint(BlockPos origin, BlockPos target) {
        Vec3d origin3d = Vec3d.ofCenter(origin);
        Vec3d target3d = Vec3d.ofCenter(target);
        Vec3d vector = target3d.subtract(origin3d).normalize();
        return origin3d.add(vector).add(0, -0.5D, 0);
    }
}
