package net.pcal.quicksort.mixins.compat.expandedstorage;

import static java.util.Objects.requireNonNull;

import net.pcal.quicksort.QuicksortingContainer;
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
@SuppressWarnings("ReferenceToMixin")
@Mixin(targets = "compasses.expandedstorage.impl.block.entity.extendable.InventoryBlockEntity$1")
public class InventoryBlockEntityWorldlyContainerMixin {
    @Inject(method = "startOpen", at = @At("HEAD"), remap = false)
    private void onStartOpen(Player player, CallbackInfo ci) {
        final InventoryBlockEntityInnerAccessor accessor = (InventoryBlockEntityInnerAccessor) this;
        final InventoryBlockEntity inventoryBlockEntity = accessor.getOuter();
        ContainerBlockEvents.CONTAINER_OPENED.invoker().onContainerOpened(
                QuicksortingContainer.of(
                        requireNonNull((ServerLevel) player.level()),
                        inventoryBlockEntity.getBlockPos(),
                        inventoryBlockEntity.getInventory()));
    }

    @Inject(method = "stopOpen", at = @At("HEAD"), remap = false)
    private void onStopOpen(Player player, CallbackInfo ci) {
        final InventoryBlockEntityInnerAccessor accessor = (InventoryBlockEntityInnerAccessor) this;
        final InventoryBlockEntity inventoryBlockEntity = accessor.getOuter();
        ContainerBlockEvents.CONTAINER_CLOSED.invoker().onContainerClosed(
                QuicksortingContainer.of(
                        requireNonNull((ServerLevel) player.level()),
                        inventoryBlockEntity.getBlockPos(),
                        inventoryBlockEntity.getInventory()));
    }
}
