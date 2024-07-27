package net.pcal.quicksort.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.pcal.quicksort.QuicksortingContainer;
import net.pcal.quicksort.api.events.ContainerBlockEvents;

import static java.util.Objects.requireNonNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnresolvedMixinReference")
@Mixin(ChestBlockEntity.class)
public class QuicksortChestMixin {
    @Inject(method = "stopOpen", at = @At("TAIL"))
    public void onClose(Player player, CallbackInfo ci) {
        final ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        final ServerLevel world = requireNonNull((ServerLevel) chest.getLevel());
        ContainerBlockEvents.CONTAINER_CLOSED.invoker().onContainerClosed(player,
                QuicksortingContainer.of(world, chest.getBlockPos(), chest));
    }

    @Inject(method = "startOpen", at = @At("TAIL"))
    public void onOpen(Player player, CallbackInfo ci) {
        final ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        final ServerLevel world = requireNonNull((ServerLevel) chest.getLevel());
        ContainerBlockEvents.CONTAINER_OPENED.invoker().onContainerOpened(player,
                QuicksortingContainer.of(world, chest.getBlockPos(), chest));
    }
}