package net.pcal.footpaths;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DropboxService implements ServerTickEvents.EndWorldTick {

    private static DropboxService INSTANCE = new DropboxService();

    public static DropboxService getInstance() {
        return INSTANCE;

    }

    public void onChestClosed(ChestBlockEntity e) {
        List<VisibleChest>  visibles = getVisibleChestsNear(e.getWorld(), e, 10);
        if (!visibles.isEmpty()) {
            jobs.add(new ChestJob(e, visibles));
        }
    }


    private static class ChestJob {
        private static final int JOB_DELAY = 3;

        final List<GhostItemEntity> ghostItems = new ArrayList<>();
        List<VisibleChest> visibleChests;
        ChestBlockEntity originChest;
        int slot = 0;
        int tick = JOB_DELAY;

        ChestJob(ChestBlockEntity chest, List<VisibleChest> visibleChests) {
            this.originChest = chest;
            this.slot = 0;
            this.visibleChests = visibleChests;
        }

        boolean tick() {
            if (tick++ >= JOB_DELAY) {
                tick = 0;
                return true;
            } else {
                return false;
            }
        }

    }


    private final List<ChestJob> jobs = new ArrayList<>();

    /**
    // get notified any time an entity's blockPos is updated
    public void onChestItemPlaced(LootableContainerBlockEntity e, int slot, ItemStack stack) {
        World world = e.getWorld();
        if (!world.isClient) {
            System.out.println("GOT IT " + this.getClass().getName());
        }
    }
**/

    private static final int GHOST_TTL = 7;

    @Override
    public void onEndTick(ServerWorld world) {


        /**
        System.out.println("1 0 0 = " +Direction.getFacing(1, 0, 0));
        System.out.println("1 5 0 = " +Direction.getFacing(1, 5, 0));
        System.out.println("1 -5 0 = " +Direction.getFacing(1, -5, 0));
        System.out.println("1 -5 -6 = " +Direction.getFacing(1, -5, -6));
*/
        Iterator<ChestJob> i = this.jobs.iterator();
        while(i.hasNext()) {
            ChestJob job = i.next();
            Iterator<GhostItemEntity> g = job.ghostItems.iterator();
            while(g.hasNext()) {
                final GhostItemEntity e = g.next();
                if (e.getItemAge() > GHOST_TTL) {
                    e.setDespawnImmediately();
                    g.remove();
                }
            }

            if (!job.tick()) continue;
            //System.out.println("Processing job "+job.chest.getPos()+" "+job.visibleChests+" "+job.slot);
            if (job.slot >= job.originChest.size()) {
                if (job.ghostItems.isEmpty()) {
                    System.out.println("JOBS DONE!");
                    i.remove();
                }
                continue;
            }
            ItemStack stack = job.originChest.getStack(job.slot);
            if (stack == null || stack.isEmpty()) {
                job.slot++;
                System.out.println("next slot "+job.slot);
                continue;
            }
            if (job.visibleChests.isEmpty()) continue;
            int chestIndex = world.random.nextBetween(0, job.visibleChests.size() - 1);
            VisibleChest visible = job.visibleChests.get(chestIndex);
            stack.decrement(1);

            ItemStack ghostStack = new ItemStack(stack.getItem(), 1);
            GhostItemEntity itemEntity = new GhostItemEntity(world, visible.originPos.getX(), visible.originPos.getY(), visible.originPos.getZ(), ghostStack);
            itemEntity.setNoGravity(true);
            itemEntity.setOnGround(false);
            itemEntity.setInvulnerable(true);
            itemEntity.setVelocity(visible.itemVelocity);
            //itemEntity.setAir(0);

            //itemEntity.itemAge = 5000;
//        itemEntity.setVelocityClient(0,0.2,0);


            //itemEntity.noClip = true;

            //double g = world.random.nextDouble() * 0.1D + 0.2D;
            //itemEntity.setVelocity(world.random.nextTriangular((double)side.getOffsetX() * g, 0.0172275D * (double)speed), world.random.nextTriangular(0.2D, 0.0172275D * (double)speed), world.random.nextTriangular((double)side.getOffsetZ() * g, 0.0172275D * (double)speed));
//        itemEntity.setVelocity(0.0, 0.0, 0.0);
//        itemEntity.setVelocityClient(0.0, 0.0, 0.0);
            world.spawnEntity(itemEntity);
            job.ghostItems.add(itemEntity);


//            spawnItem(world, new ItemStack(item), 5, position, targetChest.getPos());

        }
    }

    private static record VisibleChest(
            LootableContainerBlockEntity targetChest,
            Vec3d originPos,
            Vec3d targetPos,
            Vec3d itemVelocity) {}

    private List<VisibleChest> getVisibleChestsNear(World world, ChestBlockEntity originChest,  int distance) {
        final GhostItemEntity itemEntity = new GhostItemEntity(world, 0,0,0, new ItemStack(Blocks.COBBLESTONE));
        System.out.println("looking for chests:");
        List<VisibleChest> out = new ArrayList<>();
        for(int d = originChest.getPos().getX() - distance; d <= originChest.getPos().getX() + distance; d++) {
            for(int e = originChest.getPos().getY() - distance; e <= originChest.getPos().getY() + distance; e++) {
                for(int f = originChest.getPos().getZ() - distance; f <= originChest.getPos().getZ() + distance; f++) {
                    if (d == originChest.getPos().getX() && e == originChest.getPos().getY() && f == originChest.getPos().getZ()) continue;
                    BlockEntity bs = world.getBlockEntity(new BlockPos(d, e, f));
                    if (!(bs instanceof LootableContainerBlockEntity targetChest)) continue;

                    Vec3d origin = getTransferPoint(originChest.getPos(), targetChest.getPos());
                    Vec3d target = getTransferPoint(targetChest.getPos(), originChest.getPos());

                    BlockHitResult result = world.raycast(new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, itemEntity));
                    if (result.getPos().equals(target)) {
                        System.out.println("VISIBLE! "+result.getBlockPos()+" "+targetChest.getPos());
                        Vec3d itemVelocity = new Vec3d(
                                (target.getX() - origin.getX()) / (GHOST_TTL),
                                (target.getY() - origin.getY()) / (GHOST_TTL),
                                (target.getZ() - origin.getZ()) / (GHOST_TTL));
                        out.add(new VisibleChest(targetChest, origin, target, itemVelocity));
                    } else {
                        System.out.println("NOT VISIBLE "+result.getBlockPos()+" "+targetChest.getPos());
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
