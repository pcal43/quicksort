package net.pcal.footpaths;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static java.util.Objects.requireNonNull;

public class DropboxService implements ServerTickEvents.EndWorldTick {

    private static final DropboxService INSTANCE = new DropboxService();

    public static DropboxService getInstance() {
        return INSTANCE;

    }

    public void onChestOpened(ChestBlockEntity e) {
        Iterator<ChestJob> i = this.jobs.iterator();
        while (i.hasNext()) {
            ChestJob job = i.next();
            if (job.originChest == e) job.stop();
        }
    }

    public void onChestClosed(ChestBlockEntity e) {
        if (isDropbox(e)) {
            final List<TargetChest> visibles = getVisibleChestsNear(e.getWorld(), e, 5);
            if (!visibles.isEmpty()) {
                final ChestJob job = ChestJob.create(e, visibles);
                if (job != null) jobs.add(job);
            }
        }
    }

    private boolean isDropbox(ChestBlockEntity chest) {
        return chest.getWorld().getBlockState(chest.getPos().down()).getBlock() == Blocks.DIAMOND_BLOCK;
    }

    @FunctionalInterface
    interface GhostCreator {
        void createGhost(Item item, TargetChest targetChest);
    }

    private static class ChestJob {

        private static final int TRANSFER_COOLDOWN = 10;

        private final List<GhostItemEntity> ghostItems = new ArrayList<>();
        private final List<SlotJob> slotJobs;
        private final ChestBlockEntity originChest;
        int tick = 0;
        private boolean isTransferComplete = false;

        static ChestJob create(ChestBlockEntity originChest, List<TargetChest> visibleTargets) {
            final List<SlotJob> slotJobs = new ArrayList<>();
            for (int slot = 0; slot < originChest.size(); slot++) {
                SlotJob slotJob = SlotJob.create(originChest, slot, visibleTargets);
                if (slotJob != null ) slotJobs.add(slotJob);
            }
            return slotJobs.isEmpty() ? null : new ChestJob(originChest, slotJobs);
        }

        ChestJob(ChestBlockEntity originChest, List<SlotJob> slotJobs) {
            this.originChest = requireNonNull(originChest);
            this.slotJobs = requireNonNull(slotJobs);
        }

        public boolean isDone() {
            return this.isTransferComplete && this.ghostItems.isEmpty();
        }

        public void tick() {
            final Iterator<GhostItemEntity> g = this.ghostItems.iterator();
            while(g.hasNext()) {
                final GhostItemEntity e = g.next();
                if (e.getItemAge() > GHOST_TTL) {
                    e.setDespawnImmediately();
                    g.remove();
                }
            }
            if (!this.isTransferComplete) {
                if (tick++ > TRANSFER_COOLDOWN) {
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
            } while(!slotJobs.isEmpty());
            return false;
        }

        private void createGhost(Item item, TargetChest targetChest) {
            World world = this.originChest.getWorld();
            world.playSound(
                    null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                    this.originChest.getPos(), // The position of where the sound will come from
                    SoundEvents.UI_TOAST_OUT, // The sound that will play, in this case, the sound the anvil plays when it lands.
                    SoundCategory.BLOCKS, // This determines which of the volume sliders affect this sound
                    .75f, //Volume multiplier, 1 is normal, 0.5 is half volume, etc
                    2.0f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
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
        private final List<TargetChest> candidateChests;
        private final ItemStack originStack;


        private SlotJob(Inventory originChest, int slot, List<TargetChest> candidateChests) {
            this.candidateChests = requireNonNull(candidateChests);
            this.originStack = originChest.getStack(slot);
        }

        static SlotJob create(LootableContainerBlockEntity originChest, int slot, List<TargetChest> allVisibleChests) {
            final ItemStack originStack = originChest.getStack(slot);
            if (originStack == null || originStack.isEmpty()) return null;
            final List<TargetChest> candidateChests = new ArrayList<>();
            for (TargetChest visibleChest : allVisibleChests) {
                if (getTargetSlot(originStack.getItem(), visibleChest.targetChest()) != null) {
                    candidateChests.add(visibleChest);
                }
            }
            return candidateChests.isEmpty() ? null : new SlotJob(originChest, slot, candidateChests);
        }

        boolean doOneTransfer(GhostCreator gc) {
            if (this.originStack.isEmpty()) return false;
            while(!this.candidateChests.isEmpty()) {
                final int candidateIndex = new Random().nextInt(this.candidateChests.size());
                final TargetChest candidate = this.candidateChests.get(candidateIndex);
                final Integer targetSlot = getTargetSlot(this.originStack.getItem(), candidate.targetChest());
                if (targetSlot == null) {
                    this.candidateChests.remove(candidateIndex); // this one's full
                } else {
                    final Item item = this.originStack.getItem();
                    this.originStack.decrement(1);
                    if (candidate.targetChest.getStack(targetSlot).isEmpty()) {
                        candidate.targetChest.setStack(targetSlot, new ItemStack(item, 1));
                    } else {
                        candidate.targetChest.getStack(targetSlot).increment(1);
                    }
                    gc.createGhost(item, candidate);
                    return true;
                }
            }
            return false;
        }

        private static Integer getTargetSlot(Item forItem, Inventory targetInventory) {
            requireNonNull(targetInventory, "inventory");
            requireNonNull(forItem, "item");
            Integer firstEmptySlot = null;
            boolean hasMatchingItem = false;
            for (int slot = 0; slot < targetInventory.size(); slot++) {
                ItemStack itemStack = requireNonNull(targetInventory.getStack(slot));
                if (itemStack.isEmpty()) {
                    if (hasMatchingItem) return slot; // this one's empty and a match was found earlier. done.
                    if (firstEmptySlot == null) firstEmptySlot = slot; // else remember this empty slot
                } else if (isMatch(forItem, itemStack)) {
                    if (firstEmptySlot != null) return firstEmptySlot; // this one matches and a previous was empty. done.
                    if (!isFull(itemStack)) return slot;
                    hasMatchingItem = true;
                }
            }
            return null;
        }


        private static boolean isMatch(Item item, ItemStack stack) {
            return stack.isOf(item);
        }

        private static boolean isFull(ItemStack stack) {
            return stack.getCount() == stack.getMaxCount();
        }
    }


    private final List<ChestJob> jobs = new ArrayList<>();

    private static final int GHOST_TTL = 7;

    @Override
    public void onEndTick(ServerWorld world) {
        if (this.jobs.isEmpty()) return;
        Iterator<ChestJob> i = this.jobs.iterator();
        while (i.hasNext()) {
            ChestJob job = i.next();
            if (job.isDone()) {
                System.out.println("JOB DONE!");
                i.remove();
            } else {
                job.tick();
            }
        }
    }

    private static record TargetChest(
            LootableContainerBlockEntity targetChest,
            Vec3d originPos,
            Vec3d targetPos,
            Vec3d itemVelocity) {}

    private List<TargetChest> getVisibleChestsNear(World world, ChestBlockEntity originChest, int distance) {
        final GhostItemEntity itemEntity = new GhostItemEntity(world, 0,0,0, new ItemStack(Blocks.COBBLESTONE));
        System.out.println("looking for chests:");
        List<TargetChest> out = new ArrayList<>();
        for(int d = originChest.getPos().getX() - distance; d <= originChest.getPos().getX() + distance; d++) {
            for(int e = originChest.getPos().getY() - distance; e <= originChest.getPos().getY() + distance; e++) {
                for(int f = originChest.getPos().getZ() - distance; f <= originChest.getPos().getZ() + distance; f++) {
                    if (d == originChest.getPos().getX() && e == originChest.getPos().getY() && f == originChest.getPos().getZ()) continue;
                    BlockEntity bs = world.getBlockEntity(new BlockPos(d, e, f));
                    if (!(bs instanceof LootableContainerBlockEntity targetChest)) continue;

                    Vec3d origin = getTransferPoint(itemEntity, originChest.getPos(), targetChest.getPos());
                    Vec3d target = getTransferPoint(itemEntity, targetChest.getPos(), originChest.getPos());

                    Vec3d adjustedOrigin = origin; //origin.add(0,-itemEntity.getHeight()/2,0);
                    Vec3d adjustedTarget = target; //target.add(0,-itemEntity.getHeight()/2,0);

                    BlockHitResult result = world.raycast(new RaycastContext(adjustedOrigin, adjustedTarget,
                            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, itemEntity));
                    if (result.getPos().equals(adjustedTarget)) {
                        System.out.println("VISIBLE! "+result.getBlockPos()+" "+targetChest.getPos());
                        Vec3d itemVelocity = new Vec3d(
                                (target.getX() - origin.getX()) / (GHOST_TTL),
                                (target.getY() - origin.getY()) / (GHOST_TTL),
                                (target.getZ() - origin.getZ()) / (GHOST_TTL));
                        out.add(new TargetChest(targetChest, origin, target, itemVelocity));
                    } else {
                        System.out.println("NOT VISIBLE "+result.getBlockPos()+" "+targetChest.getPos());
                    }
                }
            }
        }
        return out;
    }

    private static Vec3d getTransferPoint(ItemEntity itemEntity, BlockPos origin, BlockPos target) {
        Vec3d origin3d = Vec3d.ofCenter(origin);
        Vec3d target3d = Vec3d.ofCenter(target);
        Vec3d vector = target3d.subtract(origin3d).normalize();
        return origin3d.add(vector).add(0, -0.5D, 0);

    }
}
