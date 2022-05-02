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
    @Shadow private BlockPos blockPos;

    //@Inject(method = "setPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ChunkSectionPos;getSectionCoord(I)I"))
    @Redirect(method = "setPos", at =  @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;blockPos:Lnet/minecraft/util/math/BlockPos;", opcode = Opcodes.PUTFIELD))
    void _entity_setPosition(Entity entity, BlockPos pos) {
        this.blockPos = pos;
        if (entity.getWorld().isClient()) return;
        VFService.getInstance().entitySteppedOnBlock(entity);
        if (entity instanceof PlayerEntity) System.out.println("setPos! "+entity.getWorld().isClient());
    }
}