package net.pcal.quicksort;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * ItemEntity that tries to disable collision checking.  The only one that really seems to work here
 * is onPlayerCollision (but that's the most important one).
 */
public class GhostItemEntity extends ItemEntity {

    public GhostItemEntity(World world, double d, double e, double f, ItemStack stack) {
        super(world, d, e, f, stack);
    }

    @Override
    protected void checkBlockCollision() {
    }

    public void onPlayerCollision(PlayerEntity player) {
        // never collide with a player
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


