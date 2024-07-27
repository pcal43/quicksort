package net.pcal.quicksort.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.pcal.quicksort.QuicksortingContainer;

public class ContainerBlockEvents {
	public static final Event<ContainerOpenedCallback> CONTAINER_OPENED = EventFactory.createArrayBacked(
			ContainerOpenedCallback.class,
			(listeners) -> (player, container) -> {
				for (ContainerOpenedCallback listener : listeners) {
					listener.onContainerOpened(player, container);
				}
			});

	public static final Event<ContainerClosedCallback> CONTAINER_CLOSED = EventFactory.createArrayBacked(
			ContainerClosedCallback.class,
			(listeners) -> (player, container) -> {
				for (ContainerClosedCallback listener : listeners) {
					listener.onContainerClosed(player, container);
				}
			});

	@FunctionalInterface
	public interface ContainerOpenedCallback {
		void onContainerOpened(Player player, QuicksortingContainer container);
	}

	@FunctionalInterface
	public interface ContainerClosedCallback {
		void onContainerClosed(Player player, QuicksortingContainer container);
	}
}
