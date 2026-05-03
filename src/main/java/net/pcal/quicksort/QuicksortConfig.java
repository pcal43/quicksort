package net.pcal.quicksort;

import org.apache.logging.log4j.Level;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record QuicksortConfig(
        List<QuicksortChestConfig> chestConfigs,
        Level logLevel
) {
    public enum TransferMode {
        ITEMS,
        STACKS;

        public static TransferMode parse(String value) {
            if (value == null) return null;
            for (TransferMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value.trim())) return mode;
            }
            throw new IllegalArgumentException("Invalid transferMode " + value);
        }
    }

    public enum AnimationMode {
        PARTICLE,
        ENTITY;

        public static AnimationMode parse(String value) {
            if (value == null) return null;
            for (AnimationMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value.trim())) return mode;
            }
            throw new IllegalArgumentException("Invalid animationMode " + value);
        }
    }

    public record QuicksortChestConfig(
            Identifier baseBlockId,
            int range,
            int cooldownTicks,
            int animationTicks,
            AnimationMode animationMode,
            float soundVolume,
            float soundPitch,
            boolean checkObstructions,
            TransferMode transferMode,
            int transferAmount,
            boolean enableRedstoneActivation,
            boolean continueWhileOpen,
            Set<Identifier> enchantmentMatchingIds,
            Set<Identifier> targetContainerIds
    ) {
    }
}

