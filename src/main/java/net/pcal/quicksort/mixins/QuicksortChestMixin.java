package net.pcal.quicksort.mixins;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.pcal.quicksort.QuicksortService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public class QuicksortChestMixin {

    @Inject(method = "onClose", at = @At("TAIL"))
    public void onClose(Player player, CallbackInfo ci) {
        ChestBlockEntity e = (ChestBlockEntity) (Object) this;
        QuicksortService.getInstance().onChestClosed(e);
    }

    @Inject(method = "onOpen", at = @At("TAIL"))
    public void onOpen(Player player, CallbackInfo ci) {
        ChestBlockEntity e = (ChestBlockEntity) (Object) this;
        QuicksortService.getInstance().onChestOpened(e);
    }
}