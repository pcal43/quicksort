package net.pcal.quicksort;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * These are the 'ghost' entities that fly from the quicksorter to the target chests when sorting is happening.
 * They disable collision checking and other normal ItemEntity behaviors - they are intended to solely be visual
 * artifacts.
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
    public boolean isIgnoringBlockTriggers() { return true;} 

    /**
     * I don't think this actually does anything.
     */
    @Override
    public boolean isAffectedByBlocks() { return false; }

    /**
     * I don't think this actually does anything.
     */
    @Override
    public boolean isColliding(BlockPos pos, BlockState state) { return false; }

    /**
     * I don't think this actually does anything.
     */
    @Override
    public boolean isInWall() { return false; }

    /**
     * Seems like a no.
     */
    @Override
    public boolean shouldPlayLavaHurtSound() { return false; }

    /**
     * Seems like a no.
     */
    @Override
	public boolean ignoreExplosion(Explosion explosion) { return true; }
    
    /**
     * I don't think this actually does anything.
     */
    @Override    
    public boolean canCollideWith(Entity e) { return false; }

    /**
     * I don't think this actually does anything.
     */
    @Override    
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

