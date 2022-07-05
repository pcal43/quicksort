package net.pcal.footpaths;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.PositionImpl;
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
        jobs.add(new ChestJob(e));
    }


    private static class ChestJob {
        ChestJob(ChestBlockEntity chest) {
            this.chest = chest;
            this.slot = 0;
        }
        ChestBlockEntity chest;
        int slot = 0;
    }


    private final List<GhostItemEntity> ghostItems = new ArrayList<>();
    private final List<ChestJob> jobs = new ArrayList<>();

    // get notified any time an entity's blockPos is updated
    public void onChestItemPlaced(LootableContainerBlockEntity e, int slot, ItemStack stack) {
        World world = e.getWorld();
        if (!world.isClient) {
            System.out.println("GOT IT " + this.getClass().getName());
        }
    }


    @Override
    public void onEndTick(ServerWorld world) {
        for(GhostItemEntity e : this.ghostItems) {
            if (e.getAge() > 40) {
                e.setDespawnImmediately();
            }
        }
        System.out.println("JOBS");
        Iterator<ChestJob> i = this.jobs.iterator();
        while(i.hasNext()) {
            System.out.println("job");
            ChestJob job = i.next();
            if (job.slot >= job.chest.size()) {
                System.out.println("removed job!");
                i.remove();
                continue;
            }
            ItemStack stack = job.chest.getStack(job.slot);
            if (stack == null) {
                job.slot++;
                System.out.println("next slot "+job.slot);
                continue;
            }
            Item item = stack.getItem();
            stack.setCount(stack.getCount() -1);

            BlockPointerImpl blockPointerImpl = new BlockPointerImpl((ServerWorld) world, job.chest.getPos());
            Position position = getOutputLocation(blockPointerImpl);

            BlockPos target = job.chest.getPos().mutableCopy().add(world.random.nextBetween(5, 10), world.random.nextBetween(5, 10), world.random.nextBetween(5,10));
            target.subtract(job.chest.getPos());
            BlockPointerImpl tbp = new BlockPointerImpl((ServerWorld) world, target);
            Position targetPos = new PositionImpl(tbp.getX(), tbp.getY(), tbp.getZ());

            spawnItem(world, new ItemStack(item), 5, Direction.UP, position, targetPos);

        }
    }

    private static Position getOutputLocation(BlockPointer pointer) {
        Direction direction = Direction.UP;
        double d = pointer.getX() + 0.7D * (double)direction.getOffsetX();
        double e = pointer.getY() + 0.7D * (double)direction.getOffsetY();
        double f = pointer.getZ() + 0.7D * (double)direction.getOffsetZ();
        return new PositionImpl(d, e, f);
    }

    private void spawnItem(World world, ItemStack stack, int speed, Direction side, Position pos, Position targetPos) {
        double d = pos.getX();
        double e = pos.getY();
        double f = pos.getZ();
        if (side.getAxis() == Direction.Axis.Y) {
            e -= 0.125D;
        } else {
            e -= 0.15625D;
        }

        GhostItemEntity itemEntity = new GhostItemEntity(world, d, e, f, stack);
        itemEntity.setNoGravity(true);
        itemEntity.setOnGround(false);
        itemEntity.setInvulnerable(true);
        double SPEED = .01D;
        itemEntity.setVelocity((targetPos.getX() - pos.getX()) * SPEED, (targetPos.getY() - pos.getY()) * SPEED, (targetPos.getZ() - pos.getZ()) * SPEED);
        itemEntity.setAir(0);
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
