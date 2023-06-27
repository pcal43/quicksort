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

    /**
     * This prevents players from being able to pick up the ghost items.
     */
    @Override
    public void onPlayerCollision(PlayerEntity player) {}

    /**
     * The prevents hoppers from pulling the ghost items.  Also seems to block some advancement-related code.
     */
    @Override
    public boolean isAlive() { return false; }

    /**
     * Prevents ghosts from catching fire if they travel through lava (which evidently doesn't block line-of-sight).
     * There's actually not any harm if they are on fire.  Just seems like the right thing.
     */
    @Override
    public boolean isFireImmune() { return true; }

    /**
     * I don't think this actually does anything.
     */
    @Override
    protected void checkBlockCollision() {}

    /**
     * I don't think this actually does anything.
     */
    public boolean isInsideWall() { return false; }

    /**
     * I don't think this actually does anything.
     */
    public boolean isCollidable() { return false; }

    /**
     * I don't think this actually does anything.
     */
    public boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) { return true; }


    @Override
    protected void initDataTracker() {
        System.out.println("skip tracking!");
        super.initDataTracker();
    }

    @Override
    public boolean shouldSave() {
        return false;
    }
}


