package net.pcal.quicksort;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Temporary visual item used by animationMode ENTITY.
 */
public class GhostItemEntity extends ItemEntity {

    private final int lifetimeTicks;
    private int quicksortAge = 0;

    public GhostItemEntity(Level world, double x, double y, double z, ItemStack stack, int lifetimeTicks) {
        super(world, x, y, z, stack);
        this.lifetimeTicks = Math.max(1, lifetimeTicks);
        setPickUpDelay(Integer.MAX_VALUE);
    }

    @Override
    public void tick() {
        baseTick();
        final Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x(), getY() + movement.y(), getZ() + movement.z());
        setOnGround(false);
        if (++this.quicksortAge > this.lifetimeTicks) discard();
    }

    @Override
    public void playerTouch(Player player) {
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean isAffectedByBlocks() {
        return false;
    }

    @Override
    public boolean isColliding(BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean shouldPlayLavaHurtSound() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isFree(double offsetX, double offsetY, double offsetZ) {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
