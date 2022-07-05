package net.pcal.footpaths;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GhostItemEntity extends ItemEntity {

    public GhostItemEntity(World world, double d, double e, double f, ItemStack stack) {
        super(world, d, e, f, stack);
        this.setNoGravity(true);
        this.setOnGround(false);
        this.setInvulnerable(true);
        this.setVelocity(0,0.2,0);
        this.setVelocityClient(0,0.2,0);

        //this.noClip = true;
        //this.fallDistance = 0.0F;


    }

    public boolean hasNoGravity() {
        return true;
    }

    public void slowMovement(BlockState state, Vec3d multiplier) {
        System.out.println("slow");
    }
/**
    @Override
    public void move(MovementType movementType, Vec3d movement) {
//does not seem to be called?
    }
**/
    @Override
    public void tick() {
        super.tick();
        this.resetPosition();
        this.setVelocity(0,0.2,0);
        this.setVelocityClient(0,0.2,0);

        //this.setPosition(this.getX(), this.getY() + 0.2, this.getZ());

        //super.baseTick();
//System.out.println("tick");
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


