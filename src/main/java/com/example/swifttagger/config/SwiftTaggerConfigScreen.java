package com.example.swifttagger.config;

import com.example.swifttagger.SwiftTaggerClient;
import com.example.swifttagger.kit.GamemodeCycle;
import com.example.swifttagger.text.Messages;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class SwiftTaggerConfigScreen {
    private SwiftTaggerConfigScreen() {}

    private static final String[] MODE_SELECTOR_VALUES = buildModeSelectorValues();

    private static String[] buildModeSelectorValues() {
        String[] values = new String[GamemodeCycle.ORDER.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = GamemodeCycle.displayName(GamemodeCycle.ORDER[i]);
        }
        return values;
    }

    public static Screen create(Screen parent) {
        ModConfig config = ConfigManager.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal(Messages.get("config.title")))
                .setSavingRunnable(() -> {
                    ConfigManager.save();
                    SwiftTaggerClient.applyDetectionConfig();
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal(Messages.get("config.category.general")));

        general.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.enabled")), config.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.enabled.tooltip")))
                .setSaveConsumer(v -> config.enabled = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.auto_detect")), config.autoDetectEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.auto_detect.tooltip")))
                .setSaveConsumer(v -> config.autoDetectEnabled = v)
                .build());

        general.addEntry(eb.startSelector(Text.literal(Messages.get("config.displayed_tier_mode")),
                        new String[]{"Selected", "Highest"},
                        normalize(config.displayedTierMode, "Selected", "Highest", "Highest"))
                .setDefaultValue("Highest")
                .setTooltip(Text.literal(Messages.get("config.displayed_tier_mode.tooltip")))
                .setSaveConsumer(v -> config.displayedTierMode = v)
                .build());

        general.addEntry(eb.startSelector(Text.literal(Messages.get("config.selected_gamemode")),
                        MODE_SELECTOR_VALUES,
                        normalizeMode(GamemodeCycle.displayName(config.selectedGamemode)))
                .setDefaultValue(GamemodeCycle.displayName("SWORD"))
                .setTooltip(Text.literal(Messages.get("config.selected_gamemode.tooltip")))
                .setSaveConsumer(v -> config.selectedGamemode = GamemodeCycle.canonicalize(v))
                .build());

        ConfigCategory display = builder.getOrCreateCategory(Text.literal(Messages.get("config.category.display")));

        display.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.show_tier")), config.showTier)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.show_tier.tooltip")))
                .setSaveConsumer(v -> config.showTier = v)
                .build());

        display.addEntry(eb.startSelector(Text.literal(Messages.get("config.badge_position")),
                        new String[]{"Gauche", "Droite"}, normalize(config.badgePosition, "Gauche", "Droite", "Droite"))
                .setDefaultValue("Droite")
                .setTooltip(Text.literal(Messages.get("config.badge_position.tooltip")))
                .setSaveConsumer(v -> config.badgePosition = v)
                .build());

        display.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.show_separator")), config.showSeparator)
                .setDefaultValue(false)
                .setTooltip(Text.literal(Messages.get("config.show_separator.tooltip")))
                .setSaveConsumer(v -> config.showSeparator = v)
                .build());

        display.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.show_chat")), config.showOnChat)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.show_chat.tooltip")))
                .setSaveConsumer(v -> config.showOnChat = v)
                .build());

        display.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.show_tab")), config.showOnTab)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.show_tab.tooltip")))
                .setSaveConsumer(v -> config.showOnTab = v)
                .build());

        display.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.show_nametag")), config.showOnNametag)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.show_nametag.tooltip")))
                .setSaveConsumer(v -> config.showOnNametag = v)
                .build());

        ConfigCategory colors = builder.getOrCreateCategory(Text.literal(Messages.get("config.category.color")));

        colors.addEntry(eb.startSelector(Text.literal(Messages.get("config.nickname_color")),
                        new String[]{"Server", "Tier color"},
                        normalize(config.nicknameColorMode, "Server", "Tier color", "Tier color"))
                .setDefaultValue("Tier color")
                .setTooltip(Text.literal(Messages.get("config.nickname_color.tooltip")))
                .setSaveConsumer(v -> config.nicknameColorMode = v)
                .build());

        colors.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.use_tier_color")), config.useTierColor)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.use_tier_color.tooltip")))
                .setSaveConsumer(v -> config.useTierColor = v)
                .build());

        colors.addEntry(eb.startTextField(Text.literal(Messages.get("config.color.guillemet")), config.guillemetColorHex)
                .setDefaultValue("#06b7fe")
                .setTooltip(Text.literal(Messages.get("config.color.guillemet.tooltip")))
                .setSaveConsumer(v -> config.guillemetColorHex = v)
                .build());

        colors.addEntry(eb.startTextField(Text.literal(Messages.get("config.color.sanction_restreint")), config.sanctionRestreintColorHex)
                .setDefaultValue("#610c1d")
                .setSaveConsumer(v -> config.sanctionRestreintColorHex = v)
                .build());

        colors.addEntry(eb.startTextField(Text.literal(Messages.get("config.color.sanction_cheater")), config.sanctionCheaterColorHex)
                .setDefaultValue("#a11f39")
                .setSaveConsumer(v -> config.sanctionCheaterColorHex = v)
                .build());

        colors.addEntry(eb.startTextField(Text.literal(Messages.get("config.color.sanction_blacklist")), config.sanctionBlacklistColorHex)
                .setDefaultValue("#202020")
                .setSaveConsumer(v -> config.sanctionBlacklistColorHex = v)
                .build());

        ConfigCategory icons = builder.getOrCreateCategory(Text.literal(Messages.get("config.category.icons")));

        icons.addEntry(eb.startSelector(Text.literal(Messages.get("config.icon_style")),
                        new String[]{"PVPHQ", "VANILLA", "MCTIERS", "PVPTIERS"}, normalizeIconStyle(config.iconStyle))
                .setDefaultValue("PVPHQ")
                .setTooltip(Text.literal(Messages.get("config.icon_style.tooltip")))
                .setSaveConsumer(v -> config.iconStyle = v)
                .build());

        icons.addEntry(eb.startBooleanToggle(Text.literal(Messages.get("config.vanilla_icon_tier_color")), config.vanillaIconTierColor)
                .setDefaultValue(true)
                .setTooltip(Text.literal(Messages.get("config.vanilla_icon_tier_color.tooltip")))
                .setSaveConsumer(v -> config.vanillaIconTierColor = v)
                .build());

        return builder.build();
    }

    private static String normalize(String value, String... allowed) {
        String fallback = allowed[allowed.length - 1];
        if (value == null) return fallback;
        for (String a : allowed) if (a.equalsIgnoreCase(value)) return a;
        return fallback;
    }

    private static String normalizeIconStyle(String value) {
        if ("MCTIERS".equalsIgnoreCase(value)) return "MCTIERS";
        if ("PVPTIERS".equalsIgnoreCase(value)) return "PVPTIERS";
        if ("VANILLA".equalsIgnoreCase(value) || "CHARACTER".equalsIgnoreCase(value)) return "VANILLA";
        return "PVPHQ";
    }

    private static String normalizeMode(String value) {
        return normalize(value, MODE_SELECTOR_VALUES);
    }
}
