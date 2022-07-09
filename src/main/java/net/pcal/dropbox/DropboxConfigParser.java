

package net.pcal.dropbox;

import com.google.gson.Gson;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class DropboxConfigParser {

    static DropboxConfig parse(Supplier<InputStream> supplier) throws IOException {
        try (InputStream in = requireNonNull(supplier.get())) {
            final String rawJson = stripComments(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            final Gson gson = new Gson();
            final DropboxConfigGson configGson = gson.fromJson(stripComments(rawJson), DropboxConfigGson.class);
            final List<DropboxConfig.DropboxChestConfig> chests = new ArrayList<>();
            for (DropboxChestConfigGson chestGson : configGson.chests) {
                chests.add(new DropboxConfig.DropboxChestConfig(
                        new Identifier(chestGson.id),
                        chestGson.range,
                        chestGson.cooldownTicks,
                        chestGson.animationTicks,
                        chestGson.soundVolume,
                        chestGson.soundPitch
                ));
            }
            DropboxConfig config = new DropboxConfig(Collections.unmodifiableList(chests));
            return config;
        }
    }

    // ===================================================================================
    // Private methods

    private static String stripComments(String json) throws IOException {
        final StringBuilder out = new StringBuilder();
        final BufferedReader br = new BufferedReader(new StringReader(json));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.strip().startsWith(("//"))) out.append(line).append('\n');
        }
        return out.toString();
    }

    // ===================================================================================
    // Gson object model

    public static class DropboxConfigGson {
        public List<DropboxChestConfigGson> chests;
    }

    public static class DropboxChestConfigGson {
        public String id;
        Integer range;
        int cooldownTicks;
        int animationTicks;
        float soundVolume;
        float soundPitch;
    }
}
