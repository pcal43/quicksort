package net.pcal.quicksort.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.pcal.quicksort.api.events.ContainerBlockEvents;

import static java.util.Objects.requireNonNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public class QuicksortChestMixin {
    @Inject(method = "stopOpen", at = @At("TAIL"))
    public void onClose(Player player, CallbackInfo ci) {
        final var chest = (ChestBlockEntity) (Object) this;
        final var world = requireNonNull((ServerLevel) chest.getLevel());

        ContainerBlockEvents.CONTAINER_CLOSED.invoker().onContainerClosed(world, player, chest, chest);
    }

    @Inject(method = "startOpen", at = @At("TAIL"))
    public void onOpen(Player player, CallbackInfo ci) {
        final var chest = (ChestBlockEntity) (Object) this;
        final var world = requireNonNull((ServerLevel) chest.getLevel());

        ContainerBlockEvents.CONTAINER_OPENED.invoker().onContainerOpened(world, player, chest, chest);
    }
}