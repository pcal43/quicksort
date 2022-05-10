package net.pcal.footpaths;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * Central singleton service.
 */
public class FootpathsService {

    // ===================================================================================
    // Constants

    public static final String LOGGER_NAME = "footpaths";
    public static final String LOG_PREFIX = "[Footpaths] ";

    // ===================================================================================
    // Fields

    private Set<String> spawnGroups;
    private Set<Identifier> entityIds;

    // ===================================================================================
    // Singleton

    private static final class SingletonHolder {
        private static final FootpathsService INSTANCE;

        static {
            INSTANCE = new FootpathsService();
        }
    }

    public static FootpathsService getInstance() {
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
            return "stepCount: " + this.stepCount + " lastStepTimestamp: " + this.lastStepTimestamp;
        }
    }

    // ===================================================================================
    // Constructors

    FootpathsService() {
        this.stepCounts = new HashMap<>();
    }

    public void initBlockConfig(FootpathsRuntimeConfig config) {
        this.config = requireNonNull(config);
        this.spawnGroups = new HashSet<>();
        this.entityIds = new HashSet<>();
        for(FootpathsRuntimeConfig.RuntimeBlockConfig rbc : config.getAllConfigs()) {
            this.spawnGroups.addAll(rbc.spawnGroups());
            this.entityIds.addAll(rbc.entityIds());
        }
        if (this.spawnGroups.isEmpty()) this.spawnGroups = null;
        if (this.entityIds.isEmpty()) this.entityIds = null;
    }

    // ===================================================================================
    // Fields

    private final Logger logger = LogManager.getLogger(LOGGER_NAME);
    private FootpathsRuntimeConfig config;
    private final Map<BlockPos, BlockHistory> stepCounts;

    public void entitySteppedOnBlock(Entity entity) {
        if (!isMatchingEntity(entity, this.entityIds, this.spawnGroups)) {
            // this is presumably going to be the case the vast majority of the time, so try to detect and
            // short-circuit it as quickly as possible.
            return;
        }
        final BlockPos pos = entity.getBlockPos().down(1);
        final World world = entity.getWorld();
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();
        final Identifier blockId = Registry.BLOCK.getId(block);
        logger.info(() -> "checking " + blockId);
        if (this.config.hasBlockConfig(blockId)) {
            final FootpathsRuntimeConfig.RuntimeBlockConfig pc = this.config.getBlockConfig(blockId);
            if (!isMatchingEntity(entity, pc.entityIds(), pc.spawnGroups())) return;
            if (!this.stepCounts.containsKey(pos)) {
                this.stepCounts.put(pos, new BlockHistory(1, world.getTime()));
            }
            final BlockHistory bh = this.stepCounts.get(pos);
            if ((world.getTime() - bh.lastStepTimestamp) > 20 * 10) {
                logger.info(() -> "step timeout " + block + " " + bh);
                bh.stepCount = 1;
                bh.lastStepTimestamp = world.getTime();
            } else {
                bh.stepCount++;
            }
            if (bh.stepCount >= pc.stepCount()) {
                logger.info(() -> "changed! " + block + " " + bh);
                final Identifier nextId = pc.nextId();
                world.setBlockState(pos, Registry.BLOCK.get(nextId).getDefaultState());
                if (this.config.hasBlockConfig(nextId)) {
                    bh.stepCount = 0;
                } else {
                    this.stepCounts.remove(pos);
                }
            } else {
                logger.info(() -> "stepCount++ " + block + " " + bh);
            }
        }
    }

    private static boolean isMatchingEntity(Entity entity, Set<Identifier> entityIds, Set<String> spawnGroups) {
        if (entityIds != null) {
            final Identifier entityId = Registry.ENTITY_TYPE.getId(entity.getType());
            if (entityIds.contains(entityId)) return true;
        }
        if (spawnGroups != null) {
            final SpawnGroup group = entity.getType().getSpawnGroup();
            if (group != null && spawnGroups.contains(group.getName())) return true;
        }
        return false;
    }

}
