package net.pcal.vanillafootpaths.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pcal.vanillafootpaths.VFService;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMoved {

//    @Shadow private BlockPos blockPos;

    //@Inject(method = "setPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ChunkSectionPos;getSectionCoord(I)I"))
    //@Redirect(method = "setPos", at =  @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;blockPos:Lnet/minecraft/util/math/BlockPos;", opcode = Opcodes.PUTFIELD))
    //@Inject(method = "setPos", at = @At("RETURN"), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;tagManager:Lnet/minecraft/tag/TagManager;", opcode = Opcodes.PUTFIELD)))


    //@Inject(method = "tick()V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/network/EntityTrackerEntry;trackingTick:I"), cancellable = true)

    @Inject(method = "setPos", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/entity/Entity;blockPos:Lnet/minecraft/util/math/BlockPos;"))
    void _entity_setPosition(double x, double y, double z, CallbackInfo ci) {

//        this.blockPos = pos;
        Entity entity = (Entity)(Object)this;
        if (entity.getWorld().isClient()) return;
        VFService.getInstance().entitySteppedOnBlock(entity);
    }
}