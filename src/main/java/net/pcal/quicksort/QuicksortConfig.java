package net.pcal.quicksort;

import org.apache.logging.log4j.Level;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record QuicksortConfig(
        List<QuicksortChestConfig> chestConfigs,
        Level logLevel
) {

    public record QuicksortChestConfig(
            ResourceLocation baseBlockId,
            int range,
            int cooldownTicks,
            int animationTicks,
            float soundVolume,
            float soundPitch,
            Set<ResourceLocation> nbtMatchEnabledIds,
            Set<ResourceLocation> targetContainerIds
    ) {

    }
}

