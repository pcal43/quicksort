package net.pcal.quicksort;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static net.pcal.quicksort.QuicksortService.LOGGER_NAME;
import static net.pcal.quicksort.QuicksortService.LOG_PREFIX;

public class QuicksortInitializer implements ModInitializer {

    // ===================================================================================
    // Constants

    private static final String DEFAULT_CONFIG_RESOURCE_NAME = "quicksort-default-config.json5";
    private static final String CONFIG_FILENAME = "quicksort.json5";

    // ===================================================================================
    // ModInitializer implementation

    @Override
    public void onInitialize() {
        try {
            initialize();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    // ===================================================================================
    // Private

    private void initialize() throws IOException {
        Logger logger = LogManager.getLogger(LOGGER_NAME);

        final Path configDirPath = Paths.get("config");

        //
        // Always write out the default config to the .disabled so the can rename/edit it if they want.
        //
        final Path configFilePath = Paths.get("config", CONFIG_FILENAME);
        final QuicksortConfig config;

        if (!configFilePath.toFile().exists()) {
            logger.info(LOG_PREFIX + "Writing default configuration to " + configFilePath);
            try (final InputStream in = QuicksortInitializer.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE_NAME)) {
                if (in == null) throw new IllegalStateException("Unable to load " + DEFAULT_CONFIG_RESOURCE_NAME);
                java.nio.file.Files.createDirectories(configDirPath); // dir doesn't exist on fresh install
                java.nio.file.Files.copy(in, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        logger.info(LOG_PREFIX + "Loading configuration from " + configFilePath);
        final QuicksortConfig defaultConfig;
        try (final InputStream in = QuicksortInitializer.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE_NAME)) {
            defaultConfig = QuicksortConfigParser.parse(in, null);
        }
        try (final InputStream in = new FileInputStream(configFilePath.toFile())) {
            // parse the config file using the first chest config from our embedded defaults as the default
            config = QuicksortConfigParser.parse(in, defaultConfig.chestConfigs().get(0));
        }
        if (config.logLevel() != Level.INFO) {
            Configurator.setLevel(LOGGER_NAME, config.logLevel());
            logger.info(LOG_PREFIX + "LogLevel set to " + config.logLevel());
        }
        QuicksortService.getInstance().init(config, logger);
        ServerTickEvents.END_WORLD_TICK.register(QuicksortService.getInstance());
        logger.info(LOG_PREFIX + "Initialized");
    }
}