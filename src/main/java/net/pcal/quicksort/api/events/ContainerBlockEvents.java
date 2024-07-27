package net.pcal.quicksort.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContainerBlockEvents {
	public static final Event<ContainerOpenedCallback> CONTAINER_OPENED = EventFactory.createArrayBacked(
			ContainerOpenedCallback.class,
			(listeners) -> (world, player, container, blockEntity) -> {
				for (ContainerOpenedCallback listener : listeners) {
					listener.onContainerOpened(world, player, container, blockEntity);
				}
			});

	public static final Event<ContainerClosedCallback> CONTAINER_CLOSED = EventFactory.createArrayBacked(
			ContainerClosedCallback.class,
			(listeners) -> (world, player, container, blockEntity) -> {
				for (ContainerClosedCallback listener : listeners) {
					listener.onContainerClosed(world, player, container, blockEntity);
				}
			});

	@FunctionalInterface
	public interface ContainerOpenedCallback {
		void onContainerOpened(ServerLevel world, Player player, Container container, BlockEntity blockEntity);
	}

	@FunctionalInterface
	public interface ContainerClosedCallback {
		void onContainerClosed(ServerLevel world, Player player, Container container, BlockEntity blockEntity);
	}
}
