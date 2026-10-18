package com.example.swifttagger.text;

import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.config.TierColors;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TierTextRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{tier}|\\{player}|\\{kit}|\\{sep}|\\{icons}");
    private static final int FALLBACK_TIER_COLOR = 0xAAAAAA;

    private TierTextRenderer() {
    }

    public static MutableText build(ModConfig config, String tier, String kitDisplayName, String playerName) {
        return build(config, config.tierFormat, tier, kitDisplayName, Text.literal(""), Text.literal(playerName));
    }

    public static MutableText build(ModConfig config, String tier, String kitDisplayName, Text playerText) {
        return build(config, config.tierFormat, tier, kitDisplayName, Text.literal(""), playerText);
    }

    public static MutableText build(ModConfig config, String tier, String kitDisplayName, Text kitIcon, Text playerText) {
        return build(config, config.tierFormat, tier, kitDisplayName, kitIcon, playerText);
    }

    public static MutableText buildBadge(ModConfig config, String tier, Text kitIcon, int tierColorRgb) {
        MutableText badge = Text.literal("");
        boolean iconBeforeTier = isIconBeforePlayer(config);
        Text tintedIcon = tintIcon(config, kitIcon, tierColorRgb);
        boolean hasIcon = tintedIcon != null && !tintedIcon.getString().isEmpty();

        if (hasIcon && iconBeforeTier) {
            badge.append(tintedIcon).append(Text.literal(" "));
        }
        badge.append(colored(tier, tierColorRgb));
        if (hasIcon && !iconBeforeTier) {
            badge.append(Text.literal(" ")).append(tintedIcon);
        }
        return badge;
    }

    public static MutableText build(ModConfig config, String template, String tier, String kitDisplayName, String playerName) {
        return build(config, template, tier, kitDisplayName, Text.literal(""), Text.literal(playerName));
    }

    private static MutableText build(ModConfig config, String template, String tier, String kitDisplayName, Text kitIcon, Text playerText) {
        if (template == null || template.isBlank()) {
            template = "{tier} {player}";
        }

        template = applyBadgePosition(config, template);
        template = applySeparator(config, template);

        int tierColorRgb = resolveTierColor(config, tier);
        boolean hasIconsToken = template.contains("{icons}");
        boolean hasIcon = kitIcon != null && !kitIcon.getString().isEmpty();

        MutableText templated = Text.literal("");
        Matcher matcher = TOKEN.matcher(template);
        int last = 0;

        while (matcher.find()) {
            if (matcher.start() > last) {
                templated.append(Text.literal(template.substring(last, matcher.start())));
            }

            switch (matcher.group()) {
                case "{tier}" -> {

                    if (!hasIconsToken && hasIcon) {
                        templated.append(tintIcon(config, kitIcon, tierColorRgb)).append(Text.literal(" "));
                    }
                    templated.append(colored(tier, tierColorRgb));
                }
                case "{icons}" -> {
                    if (hasIcon) {
                        templated.append(tintIcon(config, kitIcon, tierColorRgb));
                    }
                }
                case "{kit}" -> templated.append(colored(kitDisplayName, tierColorRgb));

                case "{player}" -> templated.append(applyNicknameColor(config, playerText, tierColorRgb));
                case "{sep}" -> templated.append(separatorText(config));
                default -> {  }
            }

            last = matcher.end();
        }

        if (last < template.length()) {
            templated.append(Text.literal(template.substring(last)));
        }

        return templated;
    }

    private static MutableText tintIcon(ModConfig config, Text icon, int rgb) {
        if (icon == null || icon.getString().isEmpty()) {
            return Text.literal("");
        }

        if (config != null && "VANILLA".equalsIgnoreCase(config.iconStyle) && config.vanillaIconTierColor) {
            return icon.copy().setStyle(icon.getStyle().withColor(TextColor.fromRgb(rgb)));
        }
        return icon.copy();
    }

    public static MutableText tintIconForDisplay(ModConfig config, Text icon, int rgb) {
        return tintIcon(config, icon, rgb);
    }

    private static MutableText colored(String value, int rgb) {
        return Text.literal(value == null ? "" : value)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static String applyBadgePosition(ModConfig config, String template) {
        if (config == null || template == null) return template;
        if (!template.contains("{player}") || !template.contains("{tier}")) return template;

        String cleaned = template
                .replace("{icons}", "")
                .replace("{tier}", "")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();

        int playerIdx = cleaned.indexOf("{player}");
        if (playerIdx < 0) return template;

        String badge = "GAUCHE".equalsIgnoreCase(config.badgePosition)
                ? "{icons} {tier} {player}"
                : "{player} {tier} {icons}";

        return cleaned.substring(0, playerIdx)
                + badge
                + cleaned.substring(playerIdx + "{player}".length());
    }

    private static String applySeparator(ModConfig config, String template) {
        if (!config.showSeparator || template.contains("{sep}")) {
            return template;
        }
        int tierIdx = template.indexOf("{tier}");
        int playerIdx = template.indexOf("{player}");

        if (tierIdx >= 0 && (playerIdx < 0 || tierIdx < playerIdx)) {
            return template.replace("{tier}", "{tier} {sep}");
        }
        if (playerIdx >= 0) {
            return template.replace("{player}", "{player} {sep}");
        }
        return "{sep} " + template;
    }

    private static Text separatorText(ModConfig config) {
        String sep = config.separator;
        if (sep == null || sep.isBlank()) {
            sep = "｜";
        }
        return Text.literal(sep).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(FALLBACK_TIER_COLOR)));
    }

    public static boolean isTierBeforePlayer(ModConfig config) {
        String template = (config != null) ? config.tierFormat : null;
        if (template == null || template.isBlank()) {
            template = "{tier} {player}";
        }
        int tierIdx = template.indexOf("{tier}");
        int playerIdx = template.indexOf("{player}");
        if (tierIdx < 0) {
            return false;
        }
        return playerIdx < 0 || tierIdx < playerIdx;
    }

    public static boolean isIconBeforePlayer(ModConfig config) {

        if (config != null && config.badgePosition != null) {
            return "GAUCHE".equalsIgnoreCase(config.badgePosition);
        }
        return isTierBeforePlayer(config);
    }

    public static boolean isBadgeBeforePlayer(ModConfig config) {
        return isIconBeforePlayer(config);
    }

    public static int resolveTierColor(ModConfig config, String tier) {
        Integer sanctionColor = com.example.swifttagger.api.SanctionType.colorForLabel(config, tier);
        if (sanctionColor != null) {
            return sanctionColor;
        }
        if (config == null || !config.useTierColor || tier == null) {
            return FALLBACK_TIER_COLOR;
        }
        String hex = config.tierColors != null ? config.tierColors.get(tier.trim().toUpperCase()) : null;
        return TierColors.parseHex(hex, FALLBACK_TIER_COLOR);
    }

    private static MutableText applyNicknameColor(ModConfig config, Text playerText, int tierColorRgb) {
        if (config == null || config.nicknameColorMode == null || "SERVER".equalsIgnoreCase(config.nicknameColorMode)) {
            return playerText.copy();
        }

        String modeKey = config.nicknameColorMode.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
        int rgb = switch (modeKey) {
            case "TIER_COLOR" -> tierColorRgb;
            default -> -1;
        };
        if (rgb < 0) {
            return playerText.copy();
        }
        return playerText.copy().setStyle(playerText.getStyle().withColor(TextColor.fromRgb(rgb)));
    }
}
