package net.pcal.footpaths;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.PositionImpl;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class DropboxService implements ServerTickEvents.EndWorldTick {

    private static DropboxService INSTANCE = new DropboxService();

    public static DropboxService getInstance() {
        return INSTANCE;

    }

    public void onChestClosed(ChestBlockEntity e) {
        List<LootableContainerBlockEntity>  visibles = getVisibleChestsNear(e.getWorld(), e, 10);
        if (!visibles.isEmpty()) {
            jobs.add(new ChestJob(e, visibles));
        }
    }


    private static class ChestJob {
        List<LootableContainerBlockEntity> visibleChests;
        ChestBlockEntity chest;
        int slot = 0;

        ChestJob(ChestBlockEntity chest, List<LootableContainerBlockEntity> visibleChests) {
            this.chest = chest;
            this.slot = 0;
            this.visibleChests = visibleChests;
        }
    }


    private final List<GhostItemEntity> ghostItems = new ArrayList<>();
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

    @Override
    public void onEndTick(ServerWorld world) {

        /**
        System.out.println("1 0 0 = " +Direction.getFacing(1, 0, 0));
        System.out.println("1 5 0 = " +Direction.getFacing(1, 5, 0));
        System.out.println("1 -5 0 = " +Direction.getFacing(1, -5, 0));
        System.out.println("1 -5 -6 = " +Direction.getFacing(1, -5, -6));
*/
        for(GhostItemEntity e : this.ghostItems) {
            if (e.getAge() > 40) {
                e.setDespawnImmediately();
            }
        }
        Iterator<ChestJob> i = this.jobs.iterator();
        while(i.hasNext()) {
            ChestJob job = i.next();
            System.out.println("Processing job "+job.chest.getPos()+" "+job.visibleChests+" "+job.slot);
            if (job.slot >= job.chest.size()) {
                System.out.println("removed job!");
                i.remove();
                continue;
            }
            ItemStack stack = job.chest.getStack(job.slot);
            if (stack == null || stack.isEmpty()) {
                job.slot++;
                System.out.println("next slot "+job.slot);
                continue;
            }
            if (job.visibleChests.isEmpty()) continue;
            BlockEntity targetChest = job.visibleChests.get(world.random.nextBetween(0, job.visibleChests.size() - 1));
            BlockPos target = targetChest.getPos();


            Item item = stack.getItem();
            stack.setCount(stack.getCount() -1);

            //BlockPointerImpl blockPointerImpl = new BlockPointerImpl((ServerWorld) world, job.chest.getPos());
            Position outputLocation = getOutputLocation(job.chest.getPos(), target);

            BlockPos pos = new BlockPos(outputLocation.getX(), outputLocation.getY(), outputLocation.getZ());
            Vec3d origin = Vec3d.ofCenter(pos);// new Vec3d(outputPos.getX(), outputPos.getY(), outputPos.getZ());


            //target.subtract(job.chest.getPos());
            //BlockPointerImpl tbp = new BlockPointerImpl(world, target);
            //Position targetPos = new PositionImpl(tbp.getX(), tbp.getY(), tbp.getZ());


            GhostItemEntity itemEntity = new GhostItemEntity(world, origin.getX(), origin.getY(), origin.getZ(), stack);
            itemEntity.setNoGravity(true);
            itemEntity.setOnGround(false);
            itemEntity.setInvulnerable(true);
            double SPEED = .03D;
            itemEntity.setVelocity((target.getX() - origin.getX()) * SPEED, (target.getY() - origin.getY()) * SPEED, (target.getZ() - origin.getZ()) * SPEED);
            //itemEntity.setAir(0);

            //itemEntity.itemAge = 5000;
//        itemEntity.setVelocityClient(0,0.2,0);


            //itemEntity.noClip = true;

            //double g = world.random.nextDouble() * 0.1D + 0.2D;
            //itemEntity.setVelocity(world.random.nextTriangular((double)side.getOffsetX() * g, 0.0172275D * (double)speed), world.random.nextTriangular(0.2D, 0.0172275D * (double)speed), world.random.nextTriangular((double)side.getOffsetZ() * g, 0.0172275D * (double)speed));
//        itemEntity.setVelocity(0.0, 0.0, 0.0);
//        itemEntity.setVelocityClient(0.0, 0.0, 0.0);
            world.spawnEntity(itemEntity);
            this.ghostItems.add(itemEntity);


//            spawnItem(world, new ItemStack(item), 5, position, targetChest.getPos());

        }
    }

    private List<LootableContainerBlockEntity> getVisibleChestsNear(World world, ChestBlockEntity chest,  int distance) {
        final GhostItemEntity itemEntity = new GhostItemEntity(world, 0,0,0, new ItemStack(Blocks.COBBLESTONE));
        System.out.println("looking for chests:");
        List<LootableContainerBlockEntity> out = new ArrayList<>();
        for(int d = chest.getPos().getX() - distance; d <= chest.getPos().getX() + distance; d++) {
            for(int e = chest.getPos().getY() - distance; e <= chest.getPos().getY() + distance; e++) {
                for(int f = chest.getPos().getZ() - distance; f <= chest.getPos().getZ() + distance; f++) {
                    if (d == chest.getPos().getX() && e == chest.getPos().getY() && f == chest.getPos().getZ()) continue;
                    BlockEntity bs = world.getBlockEntity(new BlockPos(d, e, f));
                    if (!(bs instanceof LootableContainerBlockEntity)) continue;
                    Vec3d target = new Vec3d(bs.getPos().getX(), bs.getPos().getY(), bs.getPos().getZ());

                    Position outputPos = getOutputLocation(chest.getPos(), bs.getPos());
                    BlockPos pos = new BlockPos(outputPos.getX(), outputPos.getY(), outputPos.getZ());
                    Vec3d origin = Vec3d.ofCenter(pos);// new Vec3d(outputPos.getX(), outputPos.getY(), outputPos.getZ());

//                    BlockHitResult result = world.raycastBlock(new BlockStateRaycastContext(origin, target,
//                            blockState -> blockState.));

                    BlockHitResult result = world.raycast(new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, itemEntity));
                    if (result.getBlockPos().equals(bs.getPos())) {
                        System.out.println("VISIBLE! "+result.getBlockPos()+" "+bs.getPos());
                        out.add((LootableContainerBlockEntity) bs);
                    } else {
                        System.out.println("NOT VISIBLE "+result.getBlockPos()+" "+bs.getPos());
                    }
                }
            }
        }
        return out;
    }

    private static Position getOutputLocation(BlockPos chest, BlockPos target) {
        Direction direction = Direction.getFacing(target.getX() - chest.getX(), target.getY() - chest.getY(), target.getZ() - chest.getZ());
        System.out.println("OUTPUT DIRECTION = "+direction);
        double d = chest.getX() + 1 * (double)direction.getOffsetX();
        double e = chest.getY() +1 * (double)direction.getOffsetY();
        double f = chest.getZ() + 1 * (double)direction.getOffsetZ();
        return new PositionImpl(d, e, f);
    }

    private void spawnItem(World world, ItemStack stack, int speed, Position pos, Position targetPos) {
        double d = pos.getX();
        double e = pos.getY();
        double f = pos.getZ();

        /**
         if (side.getAxis() == Direction.Axis.Y) {
         e -= 0.125D;
         } else {
         e -= 0.15625D;
         }**/

        GhostItemEntity itemEntity = new GhostItemEntity(world, d, e, f, stack);
        //itemEntity.setNoGravity(true);
        //itemEntity.setOnGround(false);
        //itemEntity.setInvulnerable(true);
        double SPEED = .01D;
        itemEntity.setVelocity((targetPos.getX() - pos.getX()) * SPEED, (targetPos.getY() - pos.getY()) * SPEED, (targetPos.getZ() - pos.getZ()) * SPEED);
        //itemEntity.setAir(0);

        //itemEntity.itemAge = 5000;
//        itemEntity.setVelocityClient(0,0.2,0);


        //itemEntity.noClip = true;

        //double g = world.random.nextDouble() * 0.1D + 0.2D;
        //itemEntity.setVelocity(world.random.nextTriangular((double)side.getOffsetX() * g, 0.0172275D * (double)speed), world.random.nextTriangular(0.2D, 0.0172275D * (double)speed), world.random.nextTriangular((double)side.getOffsetZ() * g, 0.0172275D * (double)speed));
//        itemEntity.setVelocity(0.0, 0.0, 0.0);
//        itemEntity.setVelocityClient(0.0, 0.0, 0.0);
        world.spawnEntity(itemEntity);
        this.ghostItems.add(itemEntity);
//        itemEntity.setNoGravity(true);
//        itemEntity.setOnGround(false);
//        itemEntity.noClip = true;

//        itemEntity.setBoundingBox(NULL_BOX);

    }


}
