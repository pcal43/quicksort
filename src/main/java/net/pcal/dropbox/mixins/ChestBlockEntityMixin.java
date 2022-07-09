package net.pcal.dropbox.mixins;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.pcal.dropbox.DropboxService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityMixin {


    @Inject(method = "onClose", at = @At("TAIL"))
    public void onClose(PlayerEntity player, CallbackInfo ci) {
        ChestBlockEntity e = (ChestBlockEntity)(Object)this;
        DropboxService.getInstance().onChestClosed(e);
    }

    @Inject(method = "onOpen", at = @At("TAIL"))
    public void onOpen(PlayerEntity player, CallbackInfo ci) {
        ChestBlockEntity e = (ChestBlockEntity)(Object)this;
        DropboxService.getInstance().onChestOpened(e);
    }

}