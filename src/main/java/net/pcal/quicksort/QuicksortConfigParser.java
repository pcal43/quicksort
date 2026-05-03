package net.pcal.quicksort;

import com.google.gson.Gson;
import net.minecraft.resources.Identifier;
import net.pcal.quicksort.QuicksortConfig.AnimationMode;
import net.pcal.quicksort.QuicksortConfig.QuicksortChestConfig;
import net.pcal.quicksort.QuicksortConfig.TransferMode;
import org.apache.logging.log4j.Level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class QuicksortConfigParser {

    static QuicksortConfig parse(final InputStream in, QuicksortChestConfig defaultChestConfig) throws IOException {
        final List<QuicksortChestConfig> chests = new ArrayList<>();
        final String rawJson = stripComments(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        final Gson gson = new Gson();

        final QuicksortConfigGson configGson = gson.fromJson(rawJson, QuicksortConfigGson.class);
        for (QuicksortChestConfigGson chestGson : configGson.quicksortChests) {
            chests.add(defaultChestConfig = createWithDefaults(defaultChestConfig,
                    chestGson.baseBlockId,
                    chestGson.range,
                    chestGson.cooldownTicks,
                    chestGson.animationTicks,
                    chestGson.animationMode,
                    chestGson.soundVolume,
                    chestGson.soundPitch,
                    chestGson.checkObstructions,
                    chestGson.transferMode,
                    chestGson.transferAmount,
                    chestGson.enableRedstoneActivation,
                    chestGson.continueWhileOpen,
                    chestGson.nbtMatchEnabledIds != null ? chestGson.nbtMatchEnabledIds : chestGson.enchantmentMatchingIds,
                    chestGson.targetContainerIds));
        }
        // adjust logging to configured level
        final String configuredLevel = configGson.logLevel == null || configGson.logLevel.isBlank() ?
                Level.INFO.toString() :
                configGson.logLevel.trim().toUpperCase();
        final Level logLevel = Level.getLevel(configuredLevel);
        if (logLevel == null) throw new IllegalArgumentException("Invalid logLevel " + configuredLevel);
        return new QuicksortConfig(Collections.unmodifiableList(chests), logLevel);
    }

    static QuicksortChestConfig createWithDefaults(
            QuicksortChestConfig dflt,
            String baseBlockId,
            Integer range,
            Integer cooldownTicks,
            Integer animationTicks,
            String animationMode,
            Float soundVolume,
            Float soundPitch,
            Boolean checkObstructions,
            String transferMode,
            Integer transferAmount,
            Boolean enableRedstoneActivation,
            Boolean continueWhileOpen,
            Collection<String> enchantmentMatchingIds,
            Collection<String> targetContainerIds) {
        final Integer resolvedTransferAmount = requireNonNull(
                transferAmount != null ? transferAmount : dflt == null ? null : dflt.transferAmount(),
                "transferAmount is required");
        if (resolvedTransferAmount < 1) throw new IllegalArgumentException("transferAmount must be at least 1");
        return new QuicksortChestConfig(
                Identifier.parse(requireNonNull(baseBlockId, "baseBlockId is required")),
                requireNonNull(range != null ? range : dflt == null ? null : dflt.range(),
                        "range is required"),
                requireNonNull(cooldownTicks != null ? cooldownTicks : dflt == null ? null : dflt.cooldownTicks(),
                        "cooldownTicks is required"),
                requireNonNull(animationTicks != null ? animationTicks : dflt == null ? null : dflt.animationTicks(),
                        "animationTicks is required"),
                requireNonNull(animationMode != null ? AnimationMode.parse(animationMode) : dflt == null ? null : dflt.animationMode(),
                        "animationMode is required"),
                requireNonNull(soundVolume != null ? soundVolume : dflt == null ? null : dflt.soundVolume(),
                        "soundVolume is required"),
                requireNonNull(soundPitch != null ? soundPitch : dflt == null ? null : dflt.soundPitch(),
                        "soundPitch is required"),
                requireNonNull(checkObstructions != null ? checkObstructions : dflt == null ? null : dflt.checkObstructions(),
                        "checkObstructions is required"),
                requireNonNull(transferMode != null ? TransferMode.parse(transferMode) : dflt == null ? null : dflt.transferMode(),
                        "transferMode is required"),
                resolvedTransferAmount,
                requireNonNull(enableRedstoneActivation != null ? enableRedstoneActivation : dflt == null ? null : dflt.enableRedstoneActivation(),
                        "enableRedstoneActivation is required"),
                requireNonNull(continueWhileOpen != null ? continueWhileOpen : dflt == null ? null : dflt.continueWhileOpen(),
                        "continueWhileOpen is required"),
                requireNonNull(enchantmentMatchingIds != null ? toIdentifierSet(enchantmentMatchingIds) : dflt == null ? null : dflt.enchantmentMatchingIds(),
                        "enchantmentMatchingIds"),
                requireNonNull(targetContainerIds != null ? toIdentifierSet(targetContainerIds) : dflt == null ? null : dflt.targetContainerIds(),
                        "targetContainerIds")
        );
    }

    private static Set<Identifier> toIdentifierSet(Collection<String> enchantmentMatchingIds) {
        final Set<Identifier> set = new HashSet<>();
        for (String id : enchantmentMatchingIds) set.add(Identifier.parse(id));
        return set;
    }

    static void write(QuicksortConfig config, OutputStream out) throws IOException {
        out.write(toJson5(config).getBytes(StandardCharsets.UTF_8));
    }

    static String toJson5(QuicksortConfig config) {
        final StringBuilder out = new StringBuilder();
        out.append("//\n");
        out.append("// Quicksort configuration file\n");
        out.append("//\n");
        out.append("// This file is parsed as JSON5-like JSON after lines starting with // are removed.\n");
        out.append("//\n\n");
        out.append("{\n");
        out.append("  // List of quicksort chest types. The type is selected by the block under the chest.\n");
        out.append("  // Later entries can omit values and inherit them from the previous entry.\n");
        out.append("  'quicksortChests' : [\n");
        final List<QuicksortChestConfig> chests = config.chestConfigs();
        for (int i = 0; i < chests.size(); i++) {
            final QuicksortChestConfig chest = chests.get(i);
            out.append("    {\n");
            out.append("      // Block id under the quicksort chest.\n");
            appendString(out, "baseBlockId", chest.baseBlockId().toString(), true);
            out.append("\n      // Cube radius where target containers are detected.\n");
            appendNumber(out, "range", chest.range(), true);
            out.append("\n      // Number of ticks between transfer attempts.\n");
            appendNumber(out, "cooldownTicks", chest.cooldownTicks(), true);
            out.append("\n      // Ticks for the item animation. Use -1 to disable animation.\n");
            appendNumber(out, "animationTicks", chest.animationTicks(), true);
            out.append("\n      // Animation renderer. PARTICLE is the default and safest. ENTITY uses temporary item entities.\n");
            appendString(out, "animationMode", chest.animationMode().name(), true);
            out.append("\n      // Volume of the transfer sound.\n");
            appendNumber(out, "soundVolume", chest.soundVolume(), true);
            out.append("\n      // Pitch of the transfer sound.\n");
            appendNumber(out, "soundPitch", chest.soundPitch(), true);
            out.append("\n      // If true, solid blocks between the quicksorter and target container will block transfers.\n");
            appendBoolean(out, "checkObstructions", chest.checkObstructions(), true);
            out.append("\n      // ITEMS sends up to transferAmount items per item type each cycle. STACKS sends up to transferAmount stacks total.\n");
            appendString(out, "transferMode", chest.transferMode().name(), true);
            out.append("\n      // Amount used by transferMode each time cooldownTicks elapses.\n");
            appendNumber(out, "transferAmount", chest.transferAmount(), true);
            out.append("\n      // If true, a redstone rising edge on the base block starts sorting automatically.\n");
            appendBoolean(out, "enableRedstoneActivation", chest.enableRedstoneActivation(), true);
            out.append("\n      // If true, sorting continues while the quicksort chest is open.\n");
            appendBoolean(out, "continueWhileOpen", chest.continueWhileOpen(), true);
            out.append("\n      // Item ids that only match when enchantments/components also match.\n");
            appendStringList(out, "enchantmentMatchingIds", chest.enchantmentMatchingIds(), true);
            out.append("\n      // Block ids that are valid sorting targets. They must be block entities with inventories.\n");
            appendStringList(out, "targetContainerIds", chest.targetContainerIds(), false);
            out.append("\n    }");
            if (i < chests.size() - 1) out.append(",");
            out.append("\n");
        }
        out.append("  ],\n\n");
        out.append("  'logLevel' : '").append(config.logLevel()).append("'\n");
        out.append("}\n");
        return out.toString();
    }

    private static void appendString(StringBuilder out, String name, String value, boolean comma) {
        out.append("      '").append(name).append("': '").append(value).append("'");
        if (comma) out.append(",");
        out.append("\n");
    }

    private static void appendNumber(StringBuilder out, String name, Number value, boolean comma) {
        out.append("      '").append(name).append("': ").append(value);
        if (comma) out.append(",");
        out.append("\n");
    }

    private static void appendBoolean(StringBuilder out, String name, boolean value, boolean comma) {
        out.append("      '").append(name).append("': ").append(value);
        if (comma) out.append(",");
        out.append("\n");
    }

    private static void appendStringList(StringBuilder out, String name, Collection<Identifier> values, boolean comma) {
        out.append("      '").append(name).append("': [");
        final List<String> sorted = values.stream().map(Identifier::toString).sorted(Comparator.naturalOrder()).toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) out.append(", ");
            out.append("'").append(sorted.get(i)).append("'");
        }
        out.append("]");
        if (comma) out.append(",");
        out.append("\n");
    }

    // ===================================================================================
    // Private methods

    private static String stripComments(String json) throws IOException {
        final StringBuilder out = new StringBuilder();
        final BufferedReader br = new BufferedReader(new StringReader(json));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.strip().startsWith(("//"))) out.append(line).append('\n');
        }
        return out.toString();
    }

    // ===================================================================================
    // Gson object model

    public static class QuicksortConfigGson {
        List<QuicksortChestConfigGson> quicksortChests;
        String logLevel;
    }

    public static class QuicksortChestConfigGson {
        String baseBlockId;
        Integer range;
        Integer cooldownTicks;
        Integer animationTicks;
        String animationMode;
        Float soundVolume;
        Float soundPitch;
        Boolean checkObstructions;
        String transferMode;
        Integer transferAmount;
        Boolean enableRedstoneActivation;
        Boolean continueWhileOpen;
        List<String> enchantmentMatchingIds;
        List<String> targetContainerIds;

        @Deprecated // this was the old name for enchantmentMatchingIds
        List<String> nbtMatchEnabledIds;
    }
}
