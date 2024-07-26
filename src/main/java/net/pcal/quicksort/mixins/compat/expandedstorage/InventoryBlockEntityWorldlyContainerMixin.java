package net.pcal.quicksort.mixins.compat.expandedstorage;

import static java.util.Objects.requireNonNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import compasses.expandedstorage.impl.block.entity.extendable.InventoryBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.pcal.quicksort.api.events.ContainerBlockEvents;

/**
 * Targets the first anonymous inner class inside of
 * {@link InventoryBlockEntity} which is a
 * {@link WorldlyContainer} named `inventory`.
 */
@Mixin(targets = "compasses.expandedstorage.impl.block.entity.extendable.InventoryBlockEntity$1")
public class InventoryBlockEntityWorldlyContainerMixin {
    @Inject(method = "startOpen", at = @At("HEAD"), remap = false)
    private void onStartOpen(Player player, CallbackInfo ci) {
        final var accessor = (InventoryBlockEntityInnerAccessor) this;
        final var inventoryBlockEntity = accessor.getOuter();
        final var world = requireNonNull((ServerLevel) player.level());

        ContainerBlockEvents.CONTAINER_OPENED.invoker().onContainerOpened(
                world,
                player,
                inventoryBlockEntity.getInventory(),
                inventoryBlockEntity);
    }

    @Inject(method = "stopOpen", at = @At("HEAD"), remap = false)
    private void onStopOpen(Player player, CallbackInfo ci) {
        final var accessor = (InventoryBlockEntityInnerAccessor) this;
        final var inventoryBlockEntity = accessor.getOuter();
        final var world = requireNonNull((ServerLevel) player.level());

        ContainerBlockEvents.CONTAINER_CLOSED.invoker().onContainerClosed(
                world,
                player,
                inventoryBlockEntity.getInventory(),
                inventoryBlockEntity);
    }
}
