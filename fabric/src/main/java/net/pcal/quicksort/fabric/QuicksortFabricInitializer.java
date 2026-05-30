package net.pcal.quicksort.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.pcal.quicksort.QuicksortMod;
import net.pcal.quicksort.QuicksortService;

public class QuicksortFabricInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        QuicksortMod.initialize();
        ServerTickEvents.END_WORLD_TICK.register(QuicksortService.getInstance()::onEndTick);
    }
}
