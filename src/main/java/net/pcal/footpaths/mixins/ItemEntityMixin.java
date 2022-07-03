package net.pcal.footpaths.mixins;

import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    /**
     @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
     public void _onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
     ci.cancel();
     }
     **/

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        final ItemEntity e = (ItemEntity)((Object)this);
        Vec3d movement = e.getVelocity();
        e.setPosition(e.getX() + movement.x, e.getY() + movement.y, e.getZ() + movement.z);
        ci.cancel();
    }

}