package net.pcal.footpaths.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.PositionImpl;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootableContainerBlockEntity.class)
public class ChestItemPlaced {

    // get notified any time an entity's blockPos is updated
    @Inject(method = "setStack", at = @At("TAIL"))
    void _place_item_in_chest(int slot, ItemStack stack, CallbackInfo ci) {
        Object o = this;
        BlockEntity e = (BlockEntity) o;
        World world = (World) e.getWorld();
        if (!world.isClient) {
            System.out.println("GOT IT " + this.getClass().getName());
            BlockPointerImpl blockPointerImpl = new BlockPointerImpl((ServerWorld) world, e.getPos());
            Position position = getOutputLocation(blockPointerImpl);
            spawnItem(world, stack, 100, Direction.UP, position);
        }
    }

    private static Position getOutputLocation(BlockPointer pointer) {
        Direction direction = Direction.UP;
        double d = pointer.getX() + 0.7D * (double)direction.getOffsetX();
        double e = pointer.getY() + 0.7D * (double)direction.getOffsetY();
        double f = pointer.getZ() + 0.7D * (double)direction.getOffsetZ();
        return new PositionImpl(d, e, f);
    }

    private static final Box NULL_BOX = new Box(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    private static void spawnItem(World world, ItemStack stack, int speed, Direction side, Position pos) {
        double d = pos.getX();
        double e = pos.getY();
        double f = pos.getZ();
        if (side.getAxis() == Direction.Axis.Y) {
            e -= 0.125D;
        } else {
            e -= 0.15625D;
        }

        ItemEntity itemEntity = new ItemEntity(world, d, e, f, stack);

        //itemEntity.noClip = true;

        double g = world.random.nextDouble() * 0.1D + 0.2D;
        itemEntity.setVelocity(world.random.nextTriangular((double)side.getOffsetX() * g, 0.0172275D * (double)speed), world.random.nextTriangular(0.2D, 0.0172275D * (double)speed), world.random.nextTriangular((double)side.getOffsetZ() * g, 0.0172275D * (double)speed));
        //itemEntity.setVelocity(0.0, world.random.nextTriangular(0.2D, 0.0172275D * (double)speed), 0.0);
        world.spawnEntity(itemEntity);
//        itemEntity.setNoGravity(true);
//        itemEntity.setOnGround(false);
//        itemEntity.noClip = true;

//        itemEntity.setBoundingBox(NULL_BOX);

    }
}