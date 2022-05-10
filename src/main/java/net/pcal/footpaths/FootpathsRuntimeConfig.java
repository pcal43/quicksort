package net.pcal.footpaths;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Runtime representation of configuration. FIXME should allow more than one RBC per block id
 */
@SuppressWarnings("ClassCanBeRecord")
public class FootpathsRuntimeConfig {

    private final Map<Identifier, RuntimeBlockConfig> blocksConfig;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<Identifier, RuntimeBlockConfig> blocksConfig = new HashMap<>();

        public Builder blockConfig(Identifier blockId, RuntimeBlockConfig rbc) {
            this.blocksConfig.put(blockId, rbc);
            return this;
        }

        FootpathsRuntimeConfig build() {
            return new FootpathsRuntimeConfig(this.blocksConfig);
        }
    }

    private FootpathsRuntimeConfig(Map<Identifier, RuntimeBlockConfig> blocksConfig) {
        this.blocksConfig = requireNonNull(blocksConfig);
    }

    public RuntimeBlockConfig getBlockConfig(Identifier blockId) {
        return this.blocksConfig.get(blockId);
    }

    public Set<RuntimeBlockConfig> getAllConfigs() {
        return ImmutableSet.copyOf(this.blocksConfig.values());
    }

    public boolean hasBlockConfig(Identifier blockId) {
        return this.blocksConfig.containsKey(blockId);
    }

    public record RuntimeBlockConfig(
            Identifier nextId,
            int stepCount,
            int timeoutTicks,
            Set<Identifier> entityIds,
            Set<String> spawnGroups
    ) {}


}
