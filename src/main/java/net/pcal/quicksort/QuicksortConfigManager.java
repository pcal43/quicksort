package net.pcal.quicksort;

import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static net.pcal.quicksort.QuicksortService.LOG_PREFIX;

final class QuicksortConfigManager {

    private static final String DEFAULT_CONFIG_RESOURCE_NAME = "quicksort-default-config.json5";
    private static final String CONFIG_FILENAME = "quicksort.json5";

    private QuicksortConfigManager() {
    }

    static Path getConfigPath() {
        return Paths.get("config", CONFIG_FILENAME);
    }

    static QuicksortConfig loadOrCreate(Logger logger) throws IOException {
        final Path configDirPath = Paths.get("config");
        final Path configFilePath = getConfigPath();
        if (!configFilePath.toFile().exists()) {
            logger.info(LOG_PREFIX + "Writing default configuration to " + configFilePath);
            try (final InputStream in = openDefaultConfig()) {
                Files.createDirectories(configDirPath);
                Files.copy(in, configFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return load();
    }

    static QuicksortConfig load() throws IOException {
        final QuicksortConfig defaultConfig = loadDefault();
        try (final InputStream in = new FileInputStream(getConfigPath().toFile())) {
            return QuicksortConfigParser.parse(in, defaultConfig.chestConfigs().get(0));
        }
    }

    static QuicksortConfig loadDefault() throws IOException {
        try (final InputStream in = openDefaultConfig()) {
            return QuicksortConfigParser.parse(in, null);
        }
    }

    static void save(QuicksortConfig config) throws IOException {
        final Path configPath = getConfigPath();
        Files.createDirectories(configPath.getParent());
        try (final OutputStream out = Files.newOutputStream(configPath)) {
            QuicksortConfigParser.write(config, out);
        }
    }

    private static InputStream openDefaultConfig() {
        final InputStream in = QuicksortConfigManager.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE_NAME);
        if (in == null) throw new IllegalStateException("Unable to load " + DEFAULT_CONFIG_RESOURCE_NAME);
        return in;
    }
}
