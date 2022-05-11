package net.pcal.footpaths.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.pcal.footpaths.FootpathsService;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.spongepowered.asm.mixin.injection.At.Shift.AFTER;
import static org.spongepowered.asm.mixin.injection.At.Shift.BEFORE;

@Mixin(Entity.class)
public class EntityMoved {

    //@Shadow
    //private BlockPos blockPos;

    // get notified any time an entity's blockPos is updated
    @Inject(method = "setPos", at = @At(value = "FIELD", shift = AFTER, opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/entity/Entity;blockPos:Lnet/minecraft/util/math/BlockPos;"))
    void _entity_blockPos_update(double x, double y, double z, CallbackInfo ci) {
        final Entity entity = (Entity)(Object)this;
        if (entity.getWorld().isClient()) return; // only process on the server
        FootpathsService.getInstance().entitySteppedOnBlock(entity);
    }
}