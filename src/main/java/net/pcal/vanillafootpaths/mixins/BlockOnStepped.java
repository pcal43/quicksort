package net.pcal.vanillafootpaths.mixins;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.pcal.vanillafootpaths.VFService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(Block.class)
public class BlockOnStepped {
    @Inject(method = "onSteppedOn", cancellable = true, at = @At(value = "HEAD"))
    private void transformGrassToPathWhenSteppedOn(World world, BlockPos pos, BlockState state, Entity entity,CallbackInfo ignored) {
        //VFService.getInstance().blockSteppedOn(world, pos, state, entity);
    }
}