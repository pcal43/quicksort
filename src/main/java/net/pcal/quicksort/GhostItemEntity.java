package net.pcal.quicksort;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * ItemEntity that tries to disable collision checking.  The only one that really seems to work here
 * is onPlayerCollision (but that's the most important one).
 */
public class GhostItemEntity extends ItemEntity {

    public GhostItemEntity(Level world, double d, double e, double f, ItemStack stack) {
        super(world, d, e, f, stack);
    }

    /**
     * This prevents players from being able to pick up the ghost items.
     */
    @Override
    public void playerTouch(Player player) {}

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
    public boolean fireImmune() { return true; }

    /**
     * I don't think this actually does anything.
     */
    @Override
    protected void checkInsideBlocks() {}

    /**
     * I don't think this actually does anything.
     */
    public boolean isInWall() { return false; }

    /**
     * I don't think this actually does anything.
     */
    public boolean canBeCollidedWith() { return false; }

    /**
     * I don't think this actually does anything.
     */
    public boolean isFree(double offsetX, double offsetY, double offsetZ) { return true; }

    /**
     * Don't save the ghosts.  Otherwise, the item is effectively duplicated if the chunk is unloaded while
     * the ghost is in flight.
     */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}


