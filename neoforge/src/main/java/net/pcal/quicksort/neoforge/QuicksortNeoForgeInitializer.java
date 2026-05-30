package net.pcal.quicksort.neoforge;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.pcal.quicksort.QuicksortMod;
import net.pcal.quicksort.QuicksortService;

@Mod(QuicksortNeoForgeInitializer.MOD_ID)
public class QuicksortNeoForgeInitializer {

    public static final String MOD_ID = "quicksort";

    public QuicksortNeoForgeInitializer(IEventBus modEventBus) {
        QuicksortMod.initialize();
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                QuicksortService.getInstance().onEndTick(serverLevel);
            }
        });
    }
}
