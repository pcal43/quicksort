package net.pcal.quicksort;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.pcal.quicksort.QuicksortConfig.AnimationMode;
import net.pcal.quicksort.QuicksortConfig.QuicksortChestConfig;
import net.pcal.quicksort.QuicksortConfig.TransferMode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static net.pcal.quicksort.QuicksortService.LOGGER_NAME;

public class QuicksortModMenuIntegration implements ModMenuApi {

    private static final String[] LOG_LEVELS = {
            "TRACE",
            "DEBUG",
            "INFO",
            "WARN",
            "ERROR",
            "FATAL",
            "OFF"
    };

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return QuicksortModMenuIntegration::createConfigScreen;
    }

    private static Screen createConfigScreen(Screen parent) {
        final MutableConfig config = loadMutableConfig();
        final QuicksortConfig defaultConfig = loadDefaultConfig();
        final QuicksortChestConfig defaultChest = defaultConfig.chestConfigs().get(0);

        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Quicksort"));
        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        final ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(entryBuilder.startSelector(Component.literal("Log level"), LOG_LEVELS, config.logLevel)
                .setDefaultValue(defaultConfig.logLevel().toString())
                .setNameProvider(Component::literal)
                .setSaveConsumer(value -> config.logLevel = value.trim().toUpperCase(Locale.ROOT))
                .build());

        final ConfigCategory chests = builder.getOrCreateCategory(Component.literal("Quicksort Chests"));
        for (MutableChestConfig chest : config.chests) {
            chests.addEntry(createChestCategory(entryBuilder, chest, defaultChest).build());
        }

        builder.setSavingRunnable(() -> saveConfig(config));
        return builder.build();
    }

    private static SubCategoryBuilder createChestCategory(
            ConfigEntryBuilder entryBuilder,
            MutableChestConfig chest,
            QuicksortChestConfig defaultChest) {
        final SubCategoryBuilder subCategory = entryBuilder.startSubCategory(Component.literal(chest.baseBlockId));
        subCategory.setExpanded(false);
        subCategory.add(entryBuilder.startStrField(Component.literal("Base block id"), chest.baseBlockId)
                .setDefaultValue(defaultChest.baseBlockId().toString())
                .setErrorSupplier(QuicksortModMenuIntegration::validateIdentifier)
                .setSaveConsumer(value -> chest.baseBlockId = value.trim())
                .build());
        subCategory.add(entryBuilder.startIntField(Component.literal("Detection radius"), chest.range)
                .setDefaultValue(defaultChest.range())
                .setMin(1)
                .setSaveConsumer(value -> chest.range = value)
                .build());
        subCategory.add(entryBuilder.startBooleanToggle(Component.literal("Check obstructions"), chest.checkObstructions)
                .setDefaultValue(defaultChest.checkObstructions())
                .setTooltip(Component.literal("When enabled, solid blocks between the quicksorter and target container will block transfers."))
                .setSaveConsumer(value -> chest.checkObstructions = value)
                .build());
        subCategory.add(entryBuilder.startEnumSelector(Component.literal("Transfer mode"), TransferMode.class, chest.transferMode)
                .setDefaultValue(defaultChest.transferMode())
                .setEnumNameProvider(value -> Component.literal(value == TransferMode.STACKS ? "Stacks" : "Items"))
                .setSaveConsumer(value -> chest.transferMode = value)
                .build());
        subCategory.add(entryBuilder.startIntField(Component.literal("Transfer amount"), chest.transferAmount)
                .setDefaultValue(defaultChest.transferAmount())
                .setMin(1)
                .setTooltip(Component.literal("Per cooldown cycle. Items mode: amount per item type. Stacks mode: total stacks."))
                .setSaveConsumer(value -> chest.transferAmount = value)
                .build());
        subCategory.add(entryBuilder.startBooleanToggle(Component.literal("Redstone activation"), chest.enableRedstoneActivation)
                .setDefaultValue(defaultChest.enableRedstoneActivation())
                .setTooltip(Component.literal("Starts sorting when the base block signal changes from off to on."))
                .setSaveConsumer(value -> chest.enableRedstoneActivation = value)
                .build());
        subCategory.add(entryBuilder.startBooleanToggle(Component.literal("Continue while open"), chest.continueWhileOpen)
                .setDefaultValue(defaultChest.continueWhileOpen())
                .setTooltip(Component.literal("Keeps sorting while the quicksort chest is open."))
                .setSaveConsumer(value -> chest.continueWhileOpen = value)
                .build());
        subCategory.add(entryBuilder.startIntField(Component.literal("Cooldown ticks"), chest.cooldownTicks)
                .setDefaultValue(defaultChest.cooldownTicks())
                .setMin(0)
                .setSaveConsumer(value -> chest.cooldownTicks = value)
                .build());
        subCategory.add(entryBuilder.startIntField(Component.literal("Animation ticks"), chest.animationTicks)
                .setDefaultValue(defaultChest.animationTicks())
                .setMin(-1)
                .setSaveConsumer(value -> chest.animationTicks = value)
                .build());
        subCategory.add(entryBuilder.startEnumSelector(Component.literal("Animation mode"), AnimationMode.class, chest.animationMode)
                .setDefaultValue(defaultChest.animationMode())
                .setEnumNameProvider(value -> Component.literal(value == AnimationMode.ENTITY ? "Entity" : "Particle"))
                .setTooltip(Component.literal("Particle is the safest default. Entity uses temporary item entities for the old flying-item style."))
                .setSaveConsumer(value -> chest.animationMode = value)
                .build());
        subCategory.add(entryBuilder.startFloatField(Component.literal("Sound volume"), chest.soundVolume)
                .setDefaultValue(defaultChest.soundVolume())
                .setMin(0.0F)
                .setSaveConsumer(value -> chest.soundVolume = value)
                .build());
        subCategory.add(entryBuilder.startFloatField(Component.literal("Sound pitch"), chest.soundPitch)
                .setDefaultValue(defaultChest.soundPitch())
                .setMin(0.0F)
                .setSaveConsumer(value -> chest.soundPitch = value)
                .build());
        subCategory.add(entryBuilder.startStrList(Component.literal("NBT/component match item ids"), chest.enchantmentMatchingIds)
                .setDefaultValue(toStringList(defaultChest.enchantmentMatchingIds()))
                .setCellErrorSupplier(QuicksortModMenuIntegration::validateIdentifier)
                .setSaveConsumer(value -> chest.enchantmentMatchingIds = new ArrayList<>(value))
                .build());
        subCategory.add(entryBuilder.startStrList(Component.literal("Target container block ids"), chest.targetContainerIds)
                .setDefaultValue(toStringList(defaultChest.targetContainerIds()))
                .setCellErrorSupplier(QuicksortModMenuIntegration::validateIdentifier)
                .setSaveConsumer(value -> chest.targetContainerIds = new ArrayList<>(value))
                .build());
        return subCategory;
    }

    private static MutableConfig loadMutableConfig() {
        try {
            return MutableConfig.from(QuicksortConfigManager.load());
        } catch (IOException e) {
            throw new RuntimeException("Unable to load Quicksort configuration", e);
        }
    }

    private static QuicksortConfig loadDefaultConfig() {
        try {
            return QuicksortConfigManager.loadDefault();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load Quicksort default configuration", e);
        }
    }

    private static void saveConfig(MutableConfig mutableConfig) {
        final QuicksortConfig config = mutableConfig.toConfig();
        try {
            QuicksortConfigManager.save(config);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save Quicksort configuration", e);
        }
        QuicksortInitializer.applyConfig(config, LogManager.getLogger(LOGGER_NAME));
    }

    private static Optional<Component> validateIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(Component.literal("Identifier is required."));
        }
        try {
            Identifier.parse(value.trim());
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.of(Component.literal("Invalid identifier."));
        }
    }

    private static List<String> toStringList(Set<Identifier> ids) {
        return ids.stream().map(Identifier::toString).sorted().toList();
    }

    private static Set<Identifier> toIdentifierSet(List<String> ids) {
        final Set<Identifier> out = new LinkedHashSet<>();
        for (String id : ids) {
            out.add(Identifier.parse(id.trim()));
        }
        return out;
    }

    private static final class MutableConfig {
        private final List<MutableChestConfig> chests;
        private String logLevel;

        private MutableConfig(List<MutableChestConfig> chests, String logLevel) {
            this.chests = chests;
            this.logLevel = logLevel;
        }

        static MutableConfig from(QuicksortConfig config) {
            return new MutableConfig(
                    config.chestConfigs().stream().map(MutableChestConfig::from).toList(),
                    config.logLevel().toString());
        }

        QuicksortConfig toConfig() {
            final Level level = Level.getLevel(this.logLevel.trim().toUpperCase(Locale.ROOT));
            return new QuicksortConfig(this.chests.stream().map(MutableChestConfig::toConfig).toList(), level);
        }
    }

    private static final class MutableChestConfig {
        private String baseBlockId;
        private int range;
        private int cooldownTicks;
        private int animationTicks;
        private AnimationMode animationMode;
        private float soundVolume;
        private float soundPitch;
        private boolean checkObstructions;
        private TransferMode transferMode;
        private int transferAmount;
        private boolean enableRedstoneActivation;
        private boolean continueWhileOpen;
        private List<String> enchantmentMatchingIds;
        private List<String> targetContainerIds;

        static MutableChestConfig from(QuicksortChestConfig config) {
            final MutableChestConfig mutable = new MutableChestConfig();
            mutable.baseBlockId = config.baseBlockId().toString();
            mutable.range = config.range();
            mutable.cooldownTicks = config.cooldownTicks();
            mutable.animationTicks = config.animationTicks();
            mutable.animationMode = config.animationMode();
            mutable.soundVolume = config.soundVolume();
            mutable.soundPitch = config.soundPitch();
            mutable.checkObstructions = config.checkObstructions();
            mutable.transferMode = config.transferMode();
            mutable.transferAmount = config.transferAmount();
            mutable.enableRedstoneActivation = config.enableRedstoneActivation();
            mutable.continueWhileOpen = config.continueWhileOpen();
            mutable.enchantmentMatchingIds = toStringList(config.enchantmentMatchingIds());
            mutable.targetContainerIds = toStringList(config.targetContainerIds());
            return mutable;
        }

        QuicksortChestConfig toConfig() {
            return new QuicksortChestConfig(
                    Identifier.parse(this.baseBlockId.trim()),
                    this.range,
                    this.cooldownTicks,
                    this.animationTicks,
                    this.animationMode,
                    this.soundVolume,
                    this.soundPitch,
                    this.checkObstructions,
                    this.transferMode,
                    this.transferAmount,
                    this.enableRedstoneActivation,
                    this.continueWhileOpen,
                    toIdentifierSet(this.enchantmentMatchingIds),
                    toIdentifierSet(this.targetContainerIds)
            );
        }
    }
}
