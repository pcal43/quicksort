package net.pcal.dropbox;

import net.minecraft.util.Identifier;

import java.util.List;

public record DropboxConfig(
        List<DropboxChestConfig> chestConfigs
) {

    public record DropboxChestConfig(
            Identifier baseBlockId,
            int range,
            int cooldownTicks,
            int animationTicks,
            float soundVolume,
            float soundPitch
            ) {
    }
}

