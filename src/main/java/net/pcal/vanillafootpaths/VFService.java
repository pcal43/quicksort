package net.pcal.vanillafootpaths;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;


/**
 * Central singleton service.
 */
public class VFService {

    // ===================================================================================
    // Constants

    public static final String LOGGER_NAME = "footpaths";
    public static final String LOG_PREFIX = "[footpaths] ";

    private static final String CONFIG_FILENAME = "footpaths.properties";
    private static final String DEFAULT_CONFIG_FILENAME = "default-footpaths.properties";

    // ===================================================================================
    // Singleton

    private static final class SingletonHolder {
        private static final VFService INSTANCE;
        static {
            INSTANCE = new VFService();
        }
    }

    public static VFService getInstance() {
        return SingletonHolder.INSTANCE;
    }


    static final class BlockHistory {
        BlockHistory(int stepCount, long lastStepTimestamp) {
            this.stepCount = stepCount;
            this.lastStepTimestamp = lastStepTimestamp;
        }
        int stepCount;
        long lastStepTimestamp;

        @Override
        public String toString() {
            return "stepCount: "+this.stepCount+" lastStepTimestamp: "+this.lastStepTimestamp;
        }
    }

    // ===================================================================================
    // Constructors

    private static int RESET_TICKS_DEFAULT = (20 * 60 * 24);

    VFService() {
        this.checks = new HashMap<>();
        this.stepCounts = new HashMap<>();
    }

    public void initBlockConfig(Map<Identifier, VFConfig.RuntimeBlockConfig> config) {
        this.checks = requireNonNull(config);
    }


    // ===================================================================================
    // Fields

    private final Logger logger = LogManager.getLogger(LOGGER_NAME);
    private final Path configFilePath = Paths.get("config", CONFIG_FILENAME);
    private Map<Identifier, VFConfig.RuntimeBlockConfig> checks;
    private final Map<BlockPos, BlockHistory> stepCounts;

    public void entitySteppedOnBlock(Entity entity) {
        if (entity instanceof PlayerEntity) {
            final BlockPos pos = entity.getBlockPos().down(1);
            final World world = entity.getWorld();
            final BlockState state = world.getBlockState(pos);
            final Block block = state.getBlock();
            final Identifier blockId = Registry.BLOCK.getId(block);
            logger.info(()-> "checking "+blockId);
            if (this.checks.containsKey(blockId)) {
                if (this.stepCounts.containsKey(pos)) {
                    final VFConfig.RuntimeBlockConfig pc = this.checks.get(blockId);
                    final BlockHistory bh = this.stepCounts.get(pos);
                    if (( world.getTime() - bh.lastStepTimestamp) > 20*10) {
                        logger.info(()-> "step timeout "+block+" "+bh);
                        bh.stepCount = 1;
                        bh.lastStepTimestamp = world.getTime();
                    }
                    if (bh.stepCount >= pc.stepCount()) {
                        logger.info(()-> "changed! "+block+" "+bh);
                        final Identifier nextId = pc.nextId();
                        world.setBlockState(pos, Registry.BLOCK.get(nextId).getDefaultState());
                        if (this.checks.containsKey(nextId)) {
                            bh.stepCount = 0;
                        } else {
                            this.stepCounts.remove(pos);
                        }
                    } else {
                        bh.stepCount++;
                        logger.info(()-> "stepCount++ "+block+" "+bh);
                    }
                } else {
                    this.stepCounts.put(pos, new BlockHistory(1, world.getTime()));
                }
            }
        }
    }
}
