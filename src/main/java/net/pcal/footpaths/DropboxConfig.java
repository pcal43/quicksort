package net.pcal.footpaths;

import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

import java.util.List;

public record DropboxConfig(
        Identifier baseBlockId,
        int range,
        boolean soundVolume
) {


    static DropboxConfig fromJson(JsonObject json) {
        String blockId = json.get

    }


    public static class GsonDropboxModConfig {
        public List<GsonDropboxChestConfig> blocks;
    }

    public static class GsonDropboxChestConfig {
        public String blockId;
        Integer animationTicks;
    }
}
