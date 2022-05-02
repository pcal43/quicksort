package net.pcal.vanillafootpaths;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import static net.pcal.vanillafootpaths.VFService.LOGGER_NAME;
import static net.pcal.vanillafootpaths.VFService.LOG_PREFIX;

public class VFInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        new ExactlyOnceInitializer();
    }

    private static class ExactlyOnceInitializer {
        static {
            final Logger logger = LogManager.getLogger(LOGGER_NAME);
            try {
                logger.info(LOG_PREFIX + "Initialized.");
            } catch (Exception e) {
                logger.catching(Level.ERROR, e);
                logger.error(LOG_PREFIX + "Failed to initialize");
            }
        }

    }
}
