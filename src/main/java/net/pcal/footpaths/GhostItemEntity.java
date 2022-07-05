package net.pcal.footpaths;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GhostItemEntity extends ItemEntity {

    public GhostItemEntity(World world, double d, double e, double f, ItemStack stack) {
        super(world, d, e, f, stack);

    }


    public int getAge() {
        return this.age;
    }


    @Override
    protected void checkBlockCollision() {

    }



    public void onPlayerCollision(PlayerEntity player) {
        System.out.println("hey free stuff...er, no");
    }

    public boolean isInsideWall() {
        return false;
    }


    public boolean isCollidable() {

        return false;
    }

    public boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
        return true;
    }

}


