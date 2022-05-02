package net.pcal.vanillafootpaths;



import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


/**
 * Central singleton service.
 */
public class VFService {

    // ===================================================================================
    // Constants

    public static final String LOGGER_NAME = "CopperHopper";
    public static final String LOG_PREFIX = "[CopperHopper] ";

    private static final String CONFIG_FILENAME = "vanillafootpaths.properties";
    private static final String DEFAULT_CONFIG_FILENAME = "default-vanillafootpaths.properties";

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


    record PathCheck(
        int durability,
        Block next
    ) {}

    static final class BlockHistory {
        BlockHistory(int stepCount, long lastStepTimestamp) {
            this.stepCount = stepCount;
            this.lastStepTimestamp = lastStepTimestamp;
        }
        int stepCount;
        long lastStepTimestamp;
    }


    // ===================================================================================
    // Constructors

    VFService() {
        this.checks = new HashMap<>();
        this.stepCounts = new HashMap<>();
        checks.put(Blocks.GRASS_BLOCK, new PathCheck(2, Blocks.DIRT));
        checks.put(Blocks.DIRT, new PathCheck(5, Blocks.COARSE_DIRT));
        checks.put(Blocks.COARSE_DIRT, new PathCheck(5, Blocks.DIRT_PATH));
    }


    // ===================================================================================
    // Fields

    private final Logger logger = LogManager.getLogger(LOGGER_NAME);
    private final Path configFilePath = Paths.get("config", CONFIG_FILENAME);
    private final File configFile = configFilePath.toFile();
    private final int checkFrequency = 10;
    private final Map<Block, PathCheck> checks;
    private final Map<BlockPos, BlockHistory> stepCounts;

    public void entitySteppedOnBlock(Entity entity) {
        if (entity instanceof PlayerEntity) {
            final BlockPos pos = entity.getBlockPos().down(1);
            final World world = entity.getWorld();
            final BlockState state = world.getBlockState(pos);
            final Block block = state.getBlock();
            System.out.println("checking "+block);
            if (this.checks.containsKey(block)) {
                if (this.stepCounts.containsKey(pos)) {
                    final PathCheck pc = this.checks.get(block);
                    final BlockHistory bh = this.stepCounts.get(pos);
                    if ((bh.lastStepTimestamp - world.getTime()) > 20*10) {
                        System.out.println("too long!");
                        bh.stepCount = 1;
                        bh.lastStepTimestamp = world.getTime();
                    }
                    if (bh.stepCount >= pc.durability) {
                        System.out.println("yes!");
                        world.setBlockState(pos, pc.next().getDefaultState());
                        bh.stepCount = 0;
                    } else {
                        bh.stepCount++;
                        System.out.println("not yet "+bh.stepCount);
                    }
                } else {
                    this.stepCounts.put(pos, new BlockHistory(1, world.getTime()));
                }
            } else {
                System.out.println("not in "+this.checks);
            }
        }
    }









}
