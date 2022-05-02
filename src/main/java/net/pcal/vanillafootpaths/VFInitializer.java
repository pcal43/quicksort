package net.pcal.vanillafootpaths;

import com.google.gson.Gson;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static net.pcal.vanillafootpaths.VFService.LOGGER_NAME;
import static net.pcal.vanillafootpaths.VFService.LOG_PREFIX;

public class VFInitializer implements ModInitializer {

    private static final String CONFIG_FILENAME = "footpaths.json5";
    private static final String DEFAULT_CONFIG_FILENAME = "footpaths-builtin.json5";

    @Override
    public void onInitialize() {
        try {
            initialize();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);

        }
    }

    private void initialize() throws IOException {
        final Logger logger = LogManager.getLogger(LOGGER_NAME);
        try {
            logger.info(LOG_PREFIX + "Initialized.");
        } catch (Exception e) {
            logger.catching(Level.ERROR, e);
            logger.error(LOG_PREFIX + "Failed to initialize");
        }

        final Path configFilePath = Paths.get("config", CONFIG_FILENAME);

        final Map<Identifier, VFConfig.RuntimeBlockConfig> config = new HashMap<>();
        final String builtinConfigRaw;
        try (InputStream in = VFInitializer.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILENAME)) {
            if (in == null) {
                throw new FileNotFoundException("Unable to load " + DEFAULT_CONFIG_FILENAME);
            }
            builtinConfigRaw = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            Gson gson = new Gson();
            FootpathsConfig builtinConfig = gson.fromJson(builtinConfigRaw, FootpathsConfig.class);
            updateConfig(config, builtinConfig);
        }

        VFService.getInstance().initBlockConfig(config);

        /**
         if (configFilePath.toFile().exists()) {
         final String userConfig;
         userConfig = Files.readString(configFilePath);
         JsonElement json = JsonParser.parseString(stripComments(userConfig));
         updateConfig(config, json);
         } else {
         Files.writeString(configFilePath, builtinConfig);
         }
         **/

    }

    private static void updateConfig(Map<Identifier, VFConfig.RuntimeBlockConfig> map, FootpathsConfig config) {
        requireNonNull(config);
        System.out.println("xxxx "+config.defaultStepCount);
        System.out.println("xxxx "+config);

        for(BlockConfig block : config.blocks) {
            final Identifier blockId = new Identifier(requireNonNull(block.id));
            final VFConfig.RuntimeBlockConfig rbc = new VFConfig.RuntimeBlockConfig(
                    block.nextId == null ? null : new Identifier(block.nextId),
                    block.stepCount == null ? config.defaultStepCount : block.stepCount,
                    block.timeoutTicks == null ? config.defaultTimeoutTicks : block.timeoutTicks
            );
            map.put(blockId, rbc);
            System.out.println("!!!"+rbc.toString());

        }

        //requireNonNull(config.blocks);
        //FootpathsConfig footpaths = requireNonNull(config.footpaths, "malformed config, missing 'footpaths'");



    }

    private static String stripComments(String json) throws IOException {
        final StringBuilder out = new StringBuilder();
        final BufferedReader br = new BufferedReader(new StringReader(json));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.strip().startsWith(("//"))) out.append(line).append('\n');
        }
        return out.toString();
    }

    public static class FootpathsConfig {

        public int defaultStepCount;
        public int defaultTimeoutTicks;
        public boolean ignoreBuiltinConfig;

        public List<BlockConfig> blocks;

    }

    public static class BlockConfig {
        public String id;
        public String nextId;
        Integer timeoutTicks;
        Integer stepCount;
    }
}