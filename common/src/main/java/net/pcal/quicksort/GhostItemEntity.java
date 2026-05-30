package net.pcal.quicksort;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.pcal.quicksort.mixins.DisplayAccessor;
import net.pcal.quicksort.mixins.ItemDisplayAccessor;

/**
 * These are the 'ghost' entities that fly from the quicksorter to the target chests when sorting is happening.
 * They are display entities rather than ItemEntities so the client will not apply item physics or block collisions.
 */
public class GhostItemEntity extends Display.ItemDisplay {

    private final Vec3 targetPos;

    public GhostItemEntity(Level world, double d, double e, double f, ItemStack stack, Vec3 targetPos) {
        super(EntityType.ITEM_DISPLAY, world);
        this.targetPos = targetPos;
        setPos(d, e, f);
        getSlot(0).set(stack);
        ((ItemDisplayAccessor)this).quicksort$setItemTransform(ItemDisplayContext.GROUND);
        setNoGravity(true);
        ((DisplayAccessor)this).quicksort$setShadowRadius(0);
        ((DisplayAccessor)this).quicksort$setShadowStrength(0);
        ((DisplayAccessor)this).quicksort$setWidth(0.25f);
        ((DisplayAccessor)this).quicksort$setHeight(0.25f);
    }

    public void setPositionInterpolationDuration(int ticks) {
        ((DisplayAccessor)this).quicksort$setPosRotInterpolationDuration(ticks);
    }

    @Override
    public void tick() {
        super.tick();
        final Vec3 nextPos = position().add(getDeltaMovement());
        setPos(reachesTarget(nextPos) ? this.targetPos : nextPos);
    }

    public boolean hasReachedTarget() {
        return reachesTarget(position());
    }

    private boolean reachesTarget(Vec3 pos) {
        return pos.distanceToSqr(this.targetPos) < 1.0E-6D ||
                this.targetPos.subtract(position()).dot(this.targetPos.subtract(pos)) <= 0;
    }

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
