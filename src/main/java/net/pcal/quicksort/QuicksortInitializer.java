package net.pcal.quicksort;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static net.pcal.quicksort.QuicksortService.LOGGER_NAME;
import static net.pcal.quicksort.QuicksortService.LOG_PREFIX;

public class QuicksortInitializer implements ModInitializer {

    // ===================================================================================
    // Constants

    private static final String CONFIG_RESOURCE_NAME = "quicksort-default.json5";
    private static final String DEFAULT_CONFIG_FILENAME = "quicksort-default.json5";
    private static final String CUSTOM_CONFIG_FILENAME = "quicksort-custom.json5";

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
        final Path customConfigFilePath = Paths.get("config", CUSTOM_CONFIG_FILENAME);
        final QuicksortConfig config;
        if (customConfigFilePath.toFile().exists()) {
            logger.info(LOG_PREFIX + "Loading custom configuration from " + customConfigFilePath);
            try (final InputStream in = new FileInputStream(customConfigFilePath.toFile())) {
                config = QuicksortConfigParser.parse(in);
            }
        } else {
            try (final InputStream in = QuicksortInitializer.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE_NAME)) {
                if (in == null) throw new IllegalStateException("Unable to load " + CONFIG_RESOURCE_NAME);
                config = QuicksortConfigParser.parse(in);
            }
            final Path disabledConfigFilepath = configDirPath.resolve(DEFAULT_CONFIG_FILENAME);
            try (InputStream in = QuicksortInitializer.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE_NAME)) {
                if (in == null) throw new IllegalStateException("Unable to load " + CONFIG_RESOURCE_NAME);
                java.nio.file.Files.createDirectories(configDirPath); // dir doesn't exist on fresh install
                java.nio.file.Files.copy(in, disabledConfigFilepath, StandardCopyOption.REPLACE_EXISTING);
            }
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