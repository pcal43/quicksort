package net.pcal.dropbox;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import static net.pcal.dropbox.DropboxService.LOGGER_NAME;
import static net.pcal.dropbox.DropboxService.LOG_PREFIX;

public class DropboxInitializer implements ModInitializer {

    // ===================================================================================
    // Constants

    private static final String CONFIG_RESOURCE_NAME = "dropbox-default-config.json5";

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

    private void initialize() throws IOException {
        final Logger logger = LogManager.getLogger(LOGGER_NAME);
        //
        // Load the default configuration from resources and write it as the -default in the installation
        //
        //
        // Load the default configuration from resources and write it as the -default in the installation
        //
        final InputStream in = DropboxInitializer.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE_NAME);
        if (in == null) {
            throw new FileNotFoundException("Unable to load resource " + CONFIG_RESOURCE_NAME); // wat
        }
        final DropboxConfig config = DropboxConfigParser.parse(()->in);
        DropboxService.getInstance().initConfig(config);
        ServerTickEvents.END_WORLD_TICK.register( DropboxService.getInstance());
        //
        // All done
        //
        logger.info(LOG_PREFIX + "Initialized");
    }



}