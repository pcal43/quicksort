package net.pcal.quicksort.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.pcal.quicksort.QuicksortingContainer;

public class ContainerBlockEvents {
	public static final Event<ContainerOpenedCallback> CONTAINER_OPENED = EventFactory.createArrayBacked(
			ContainerOpenedCallback.class,
			(listeners) -> (container) -> {
				for (ContainerOpenedCallback listener : listeners) {
					listener.onContainerOpened(container);
				}
			});

	public static final Event<ContainerClosedCallback> CONTAINER_CLOSED = EventFactory.createArrayBacked(
			ContainerClosedCallback.class,
			(listeners) -> (container) -> {
				for (ContainerClosedCallback listener : listeners) {
					listener.onContainerClosed(container);
				}
			});

	@FunctionalInterface
	public interface ContainerOpenedCallback {
		void onContainerOpened(QuicksortingContainer container);
	}

	@FunctionalInterface
	public interface ContainerClosedCallback {
		void onContainerClosed(QuicksortingContainer container);
	}
}
