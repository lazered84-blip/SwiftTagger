package com.example.swifttagger.kit;

import com.example.swifttagger.config.ModConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KitIcons {
    private KitIcons() {
    }

    public static final Identifier ICON_FONT = Identifier.of("swifttagger", "icons");
    public static final Identifier MCTIERS_FONT = Identifier.of("swifttagger", "mctiers");
    public static final Identifier PVPTIERS_FONT = Identifier.of("swifttagger", "pvptiers");

    private static final StyleSpriteSource ICON_FONT_SOURCE = new StyleSpriteSource.Font(ICON_FONT);
    private static final StyleSpriteSource MCTIERS_FONT_SOURCE = new StyleSpriteSource.Font(MCTIERS_FONT);
    private static final StyleSpriteSource PVPTIERS_FONT_SOURCE = new StyleSpriteSource.Font(PVPTIERS_FONT);

    private static final Map<KitId, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put(KitId.AXE, "🪓");
        DEFAULTS.put(KitId.MACE, "🔨");
        DEFAULTS.put(KitId.NETHERITE_POT, "☠");
        DEFAULTS.put(KitId.DIAMOND_POT, "⚗");
        DEFAULTS.put(KitId.SMP, "⛨");
        DEFAULTS.put(KitId.SWORD, "🗡");
        DEFAULTS.put(KitId.UHC, "❤");
        DEFAULTS.put(KitId.VANILLA, "✦");
        DEFAULTS.put(KitId.UNKNOWN, "");
    }

    private static final Map<KitId, Integer> VANILLA_COLORS = new LinkedHashMap<>();
    static {
        VANILLA_COLORS.put(KitId.VANILLA, 0xF953F9);
        VANILLA_COLORS.put(KitId.UHC, 0xF55252);
        VANILLA_COLORS.put(KitId.SWORD, 0xA0F7EA);
        VANILLA_COLORS.put(KitId.NETHERITE_POT, 0x7B483E);
        VANILLA_COLORS.put(KitId.SMP, 0xE6C743);
        VANILLA_COLORS.put(KitId.DIAMOND_POT, 0xF90000);
        VANILLA_COLORS.put(KitId.AXE, 0x53F953);
        VANILLA_COLORS.put(KitId.MACE, 0xD3D3D3);
    }

    public static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Map.Entry<KitId, String> entry : DEFAULTS.entrySet()) {
            m.put(entry.getKey().name(), entry.getValue());
        }
        return m;
    }

    public static String defaultOf(KitId kitId) {
        if (kitId == null) return "";
        return DEFAULTS.getOrDefault(kitId, "");
    }

    public static String of(Map<String, String> configuredIcons, KitId kitId) {
        if (kitId == null) return "";
        if (configuredIcons != null) {
            String configured = configuredIcons.get(kitId.name());
            if (configured != null && !configured.isEmpty()) {
                return configured;
            }
        }
        return defaultOf(kitId);
    }

    public static String of(Map<String, String> configuredIcons, String modeKey) {
        return of(configuredIcons, KitId.fromModeKey(modeKey));
    }

    public static MutableText iconText(ModConfig config, KitId kitId) {
        Map<String, String> configuredIcons = config != null ? config.kitIcons : null;
        String icon = of(configuredIcons, kitId);
        if (icon.isEmpty()) return Text.literal("");

        String def = defaultOf(kitId);
        boolean isDefaultIcon = !def.isEmpty() && icon.equals(def);
        if (!isDefaultIcon || config == null) return Text.literal(icon);

        String style = config.iconStyle == null ? "PVPHQ" : config.iconStyle.toUpperCase(java.util.Locale.ROOT);
        return switch (style) {
            case "MCTIERS" -> Text.literal(icon).setStyle(Style.EMPTY.withFont(MCTIERS_FONT_SOURCE));
            case "PVPTIERS" -> Text.literal(icon).setStyle(Style.EMPTY.withFont(PVPTIERS_FONT_SOURCE));
            case "VANILLA", "CHARACTER" -> {
                Integer fixedColor = VANILLA_COLORS.get(kitId);
                yield fixedColor != null
                        ? Text.literal(icon).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(fixedColor)))
                        : Text.literal(icon);
            }
            default -> Text.literal(icon).setStyle(Style.EMPTY.withFont(ICON_FONT_SOURCE));
        };
    }

    public static MutableText iconText(ModConfig config, String modeKey) {
        return iconText(config, KitId.fromModeKey(modeKey));
    }
}
