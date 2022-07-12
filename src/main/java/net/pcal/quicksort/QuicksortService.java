package net.pcal.quicksort;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
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

    private final List<ChestJob> jobs = new ArrayList<>();
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
    // Public methods


    /**
     * When a chest is closed, start a new job.  Called from the mixin.
     */
    public void onChestClosed(ChestBlockEntity e) {
        final QuicksortChestConfig chestConfig = getChestConfigFor(e);
        if (chestConfig != null) {
            final List<TargetChest> visibles = getVisibleChestsNear(chestConfig, e.getWorld(), e, chestConfig.range());
            if (!visibles.isEmpty()) {
                final ChestJob job = ChestJob.create(chestConfig, e, visibles);
                if (job != null) jobs.add(job);
            }
        }
    }

    /**
     * When a chest is opened, we stop any jobs running on it (there really should be at most one).
     * Called from the mixin.
     */
    public void onChestOpened(ChestBlockEntity chestEntity) {
        for (ChestJob job : this.jobs) {
            if (job.originChest == chestEntity) job.stop();
        }
    }

    // ===================================================================================
    // EndWorldTick implementation

    @Override
    public void onEndTick(ServerWorld world) {
        if (this.jobs.isEmpty()) return;
        Iterator<ChestJob> i = this.jobs.iterator();
        while (i.hasNext()) {
            final ChestJob job = i.next();
            if (job.isDone()) {
                this.logger.debug(LOG_PREFIX + " job completed for chest at " + job.originChest.getPos());
                i.remove();
            } else {
                job.tick();
            }
        }
    }

    // ===================================================================================
    // Private stuff

    private QuicksortChestConfig getChestConfigFor(ChestBlockEntity chest) {
        final Block baseBlock = requireNonNull(chest.getWorld()).getBlockState(chest.getPos().down()).getBlock();
        final Identifier baseBlockId = Registry.BLOCK.getId(baseBlock);
        return this.config.get(baseBlockId);
    }

    @FunctionalInterface
    interface GhostCreator {
        void createGhost(Item item, TargetChest targetChest);
    }

    private static class ChestJob {

        private final QuicksortChestConfig chestConfig;
        private final List<GhostItemEntity> ghostItems = new ArrayList<>();
        private final List<SlotJob> slotJobs;
        private final ChestBlockEntity originChest;
        private boolean isTransferComplete = false;
        int tick = 0;

        static ChestJob create(QuicksortChestConfig chestConfig, ChestBlockEntity originChest, List<TargetChest> visibleTargets) {
            final List<SlotJob> slotJobs = new ArrayList<>();
            for (int slot = 0; slot < originChest.size(); slot++) {
                SlotJob slotJob = SlotJob.create(chestConfig, originChest, slot, visibleTargets);
                if (slotJob != null) slotJobs.add(slotJob);
            }
            return slotJobs.isEmpty() ? null : new ChestJob(chestConfig, originChest, slotJobs);
        }

        ChestJob(QuicksortChestConfig chestConfig, ChestBlockEntity originChest, List<SlotJob> slotJobs) {
            this.chestConfig = requireNonNull(chestConfig);
            this.originChest = requireNonNull(originChest);
            this.slotJobs = requireNonNull(slotJobs);
        }

        public boolean isDone() {
            return this.isTransferComplete && this.ghostItems.isEmpty();
        }

        public void tick() {
            final Iterator<GhostItemEntity> g = this.ghostItems.iterator();
            while (g.hasNext()) {
                final GhostItemEntity e = g.next();
                if (e.getItemAge() > this.chestConfig.animationTicks()) {
                    e.setDespawnImmediately();
                    g.remove();
                }
            }
            if (!this.isTransferComplete) {
                if (tick++ > this.chestConfig.cooldownTicks()) {
                    tick = 0;
                    if (!doOneTransfer()) this.isTransferComplete = true;
                }
            }
        }

        private boolean doOneTransfer() {
            SlotJob slotJob;
            do {
                final int jobIndex = new Random().nextInt(this.slotJobs.size());
                slotJob = this.slotJobs.get(jobIndex);
                if (!slotJob.doOneTransfer(this::createGhost)) {
                    this.slotJobs.remove(jobIndex);
                } else {
                    return true;
                }
            } while (!slotJobs.isEmpty());
            return false;
        }

        private void createGhost(Item item, TargetChest targetChest) {
            World world = requireNonNull(this.originChest.getWorld());
            world.playSound(
                    null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                    this.originChest.getPos(), // The position of where the sound will come from
                    SoundEvents.UI_TOAST_OUT, // The sound that will play, in this case, the sound the anvil plays when it lands.
                    SoundCategory.BLOCKS, // This determines which of the volume sliders affect this sound
                    chestConfig.soundVolume(), // .75f
                    chestConfig.soundPitch()  // 2.0f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
            );
            ItemStack ghostStack = new ItemStack(item, 1);
            GhostItemEntity itemEntity = new GhostItemEntity(this.originChest.getWorld(),
                    targetChest.originPos.getX(), targetChest.originPos.getY(), targetChest.originPos.getZ(), ghostStack);
            this.ghostItems.add(itemEntity);
            itemEntity.setNoGravity(true);
            itemEntity.setOnGround(false);
            itemEntity.setInvulnerable(true);
            itemEntity.setVelocity(targetChest.itemVelocity);
            this.originChest.getWorld().spawnEntity(itemEntity);
        }

        public void stop() {
            this.isTransferComplete = true;
            this.slotJobs.clear();
        }
    }

    private static class SlotJob {
        private final QuicksortChestConfig chestConfig;
        private final List<TargetChest> candidateChests;
        private final ItemStack originStack;


        static SlotJob create(QuicksortChestConfig chestConfig, LootableContainerBlockEntity originChest, int slot, List<TargetChest> allVisibleChests) {
            final ItemStack originStack = originChest.getStack(slot);
            if (originStack == null || originStack.isEmpty()) return null;
            final List<TargetChest> candidateChests = new ArrayList<>();
            for (TargetChest visibleChest : allVisibleChests) {
                if (isValidTarget(originStack, visibleChest.targetChest(), chestConfig.nbtMatchEnabledIds())) {
                    candidateChests.add(visibleChest);
                }
            }
            return candidateChests.isEmpty() ? null : new SlotJob(chestConfig, originChest, slot, candidateChests);
        }

        private SlotJob(QuicksortChestConfig chestConfig, Inventory originChest, int slot, List<TargetChest> candidateChests) {
            this.chestConfig = requireNonNull(chestConfig);
            this.candidateChests = requireNonNull(candidateChests);
            this.originStack = originChest.getStack(slot);
        }

        boolean doOneTransfer(GhostCreator gc) {
            if (this.originStack.isEmpty()) return false;
            while (!this.candidateChests.isEmpty()) {
                final ItemStack copy = this.originStack.copy();
                final Item ghostItem = copy.getItem();
                copy.setCount(1);
                final int candidateIndex = new Random().nextInt(this.candidateChests.size());
                final TargetChest candidate = this.candidateChests.get(candidateIndex);
                ItemStack itemStack2 = HopperBlockEntity.transfer((Inventory) null, candidate.targetChest(), copy, (Direction) null);
                if (itemStack2.isEmpty()) {
                    if (this.chestConfig.animationTicks() > 0) gc.createGhost(ghostItem, candidate);
                    this.originStack.decrement(1);
                    return true;
                } else {
                    this.candidateChests.remove(candidateIndex); // this one's full
                }
            }
            return false;
        }

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
                    (!nbtMatchEnabledIds.contains(Registry.ITEM.getId(first.getItem())) ||
                            ItemStack.areNbtEqual(first, second));
        }

        private static boolean isFull(ItemStack stack) {
            return stack.getCount() == stack.getMaxCount();
        }
    }

    private record TargetChest(
            LootableContainerBlockEntity targetChest,
            Vec3d originPos,
            Vec3d targetPos,
            Vec3d itemVelocity) {
    }

    private List<TargetChest> getVisibleChestsNear(QuicksortChestConfig chestConfig, World world, ChestBlockEntity originChest, int distance) {
        final GhostItemEntity itemEntity = new GhostItemEntity(world, 0, 0, 0, new ItemStack(Blocks.COBBLESTONE));
        List<TargetChest> out = new ArrayList<>();
        for (int d = originChest.getPos().getX() - distance; d <= originChest.getPos().getX() + distance; d++) {
            for (int e = originChest.getPos().getY() - distance; e <= originChest.getPos().getY() + distance; e++) {
                for (int f = originChest.getPos().getZ() - distance; f <= originChest.getPos().getZ() + distance; f++) {
                    if (d == originChest.getPos().getX() && e == originChest.getPos().getY() && f == originChest.getPos().getZ())
                        continue;
                    BlockEntity bs = world.getBlockEntity(new BlockPos(d, e, f));
                    if (!(bs instanceof ChestBlockEntity targetChest)) continue;
                    if (getChestConfigFor((ChestBlockEntity) bs) != null)
                        continue; // don't send to other sorting chests

                    final Vec3d origin = getTransferPoint(originChest.getPos(), targetChest.getPos());
                    final Vec3d target = getTransferPoint(targetChest.getPos(), originChest.getPos());

                    BlockHitResult result = world.raycast(new RaycastContext(origin, target,
                            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, itemEntity));
                    if (result.getPos().equals(target)) {
                        this.logger.debug(() -> LOG_PREFIX + " visible chest found at " + result.getBlockPos() + " " + targetChest.getPos());
                        Vec3d itemVelocity = new Vec3d(
                                (target.getX() - origin.getX()) / chestConfig.animationTicks(),
                                (target.getY() - origin.getY()) / chestConfig.animationTicks(),
                                (target.getZ() - origin.getZ()) / chestConfig.animationTicks());
                        out.add(new TargetChest(targetChest, origin, target, itemVelocity));
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
