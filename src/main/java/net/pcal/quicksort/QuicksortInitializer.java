package net.pcal.quicksort;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;

import static net.pcal.quicksort.QuicksortService.LOGGER_NAME;
import static net.pcal.quicksort.QuicksortService.LOG_PREFIX;

public class QuicksortInitializer implements ModInitializer {

    private static final Logger LOG = LogManager.getLogger(LOGGER_NAME);

    @Override
    public void onInitialize() {
        LOG.info(LOG_PREFIX + "=== Quicksort mod initializing ===");
        try {
            initialize();
            LOG.info(LOG_PREFIX + "=== Quicksort mod initialized successfully ===");
        } catch (IOException ioe) {
            LOG.error(LOG_PREFIX + "Failed to initialize Quicksort mod", ioe);
            throw new RuntimeException(ioe);
        }
    }

    private void initialize() throws IOException {
        final Logger logger = LogManager.getLogger(LOGGER_NAME);

        LOG.info(LOG_PREFIX + "Loading configuration from " + QuicksortConfigManager.getConfigPath());
        final QuicksortConfig config = QuicksortConfigManager.loadOrCreate(logger);
        LOG.info(LOG_PREFIX + "Loaded " + config.chestConfigs().size() + " chest configs, logLevel=" + config.logLevel());
        applyConfig(config, logger);

        LOG.info(LOG_PREFIX + "Registering SERVER_STARTING event");
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);

        LOG.info(LOG_PREFIX + "Registering SERVER_STARTED event");
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);

        LOG.info(LOG_PREFIX + "Registering SERVER_STOPPING event");
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        LOG.info(LOG_PREFIX + "Registering SERVER_STOPPED event");
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);

        LOG.info(LOG_PREFIX + "Registering BLOCK_ENTITY_LOAD event");
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(this::onBlockEntityLoaded);

        LOG.info(LOG_PREFIX + "Registering BLOCK_ENTITY_UNLOAD event");
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(this::onBlockEntityUnloaded);

        LOG.info(LOG_PREFIX + "Registering END_LEVEL_TICK event");
        ServerTickEvents.END_LEVEL_TICK.register(QuicksortService.getInstance());
    }

    private int chestLoadCount = 0;
    private int chestUnloadCount = 0;

    private void onServerStarting(net.minecraft.server.MinecraftServer server) {
        LOG.info(LOG_PREFIX + ">>> SERVER_STARTING: Resetting runtime state");
        QuicksortService.getInstance().resetRuntimeState();
        chestLoadCount = 0;
        chestUnloadCount = 0;
        LOG.info(LOG_PREFIX + "Runtime state reset complete");
    }

    private void onServerStarted(net.minecraft.server.MinecraftServer server) {
        LOG.info(LOG_PREFIX + ">>> SERVER_STARTED: World loading complete. Chests loaded=" + chestLoadCount + ", Chests unloaded=" + chestUnloadCount);
    }

    private void onServerStopping(net.minecraft.server.MinecraftServer server) {
        LOG.info(LOG_PREFIX + ">>> SERVER_STOPPING: Resetting runtime state");
        QuicksortService.getInstance().resetRuntimeState();
        LOG.info(LOG_PREFIX + "Runtime state reset complete");
    }

    private void onServerStopped(net.minecraft.server.MinecraftServer server) {
        LOG.info(LOG_PREFIX + ">>> SERVER_STOPPED: Resetting runtime state");
        QuicksortService.getInstance().resetRuntimeState();
        LOG.info(LOG_PREFIX + "Runtime state reset complete");
    }

    void onBlockEntityLoaded(net.minecraft.world.level.block.entity.BlockEntity entity, net.minecraft.server.level.ServerLevel world) {
        chestLoadCount++;
        QuicksortService.getInstance().onBlockEntityLoaded(entity, world);
    }

    void onBlockEntityUnloaded(net.minecraft.world.level.block.entity.BlockEntity entity, net.minecraft.server.level.ServerLevel world) {
        chestUnloadCount++;
        QuicksortService.getInstance().onBlockEntityUnloaded(entity, world);
    }

    static void applyConfig(QuicksortConfig config, Logger logger) {
        Configurator.setLevel(LOGGER_NAME, config.logLevel());
        if (config.logLevel() != Level.INFO) logger.info(LOG_PREFIX + "LogLevel set to " + config.logLevel());
        QuicksortService.getInstance().init(config, logger);
    }
}
