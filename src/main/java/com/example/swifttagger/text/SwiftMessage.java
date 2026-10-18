package com.example.swifttagger.text;

import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.kit.KitIcons;
import com.example.swifttagger.kit.KitId;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Locale;

public final class SwiftMessage {

    private static final TextColor BLUE = TextColor.fromRgb(0x06b7fe);
    private static final TextColor GREEN = TextColor.fromFormatting(Formatting.GREEN);
    private static final TextColor RED = TextColor.fromFormatting(Formatting.RED);

    private SwiftMessage() {
    }

    public static MutableText success(String message) {
        MutableText icon = Text.literal("✅ ").setStyle(Style.EMPTY.withColor(GREEN).withBold(true));
        return build(icon, GREEN, message);
    }

    public static MutableText error(String message) {
        MutableText icon = Text.literal("❌ ").setStyle(Style.EMPTY.withColor(RED).withBold(true));
        return build(icon, RED, message);
    }

    public static MutableText info(String message) {
        MutableText icon = Text.literal("⚔ ").setStyle(Style.EMPTY.withColor(BLUE));
        return build(icon, BLUE, message);
    }

    public static MutableText withIcon(MutableText icon, String message) {
        MutableText prefixed = Text.literal("");
        if (icon != null) {
            prefixed.append(icon);
        }
        prefixed.append(Text.literal(" "));
        return build(prefixed, GREEN, message);
    }

    public static MutableText forMode(ModConfig config, String modeName, String message) {
        if (modeName == null) {
            return success(message);
        }
        if (modeName.equalsIgnoreCase("AUCUN")) {
            return error(message);
        }
        try {
            KitId kitId = KitId.valueOf(modeName.toUpperCase(Locale.ROOT));
            return forMode(config, kitId, message);
        } catch (IllegalArgumentException e) {
            return success(message);
        }
    }

    public static MutableText forMode(ModConfig config, KitId kitId, String message) {
        if (kitId == null || kitId == KitId.UNKNOWN) {
            return success(message);
        }
        return withIcon(KitIcons.iconText(config, kitId), message);
    }

    private static MutableText build(MutableText icon, TextColor accent, String message) {

        int guillemetRgb = com.example.swifttagger.config.TierColors.parseHex(
                com.example.swifttagger.config.ConfigManager.get().guillemetColorHex, 0x06b7fe);
        TextColor guillemetColor = TextColor.fromRgb(guillemetRgb);
        return Text.literal("")
                .append(icon)
                .append(Text.literal("» ").setStyle(Style.EMPTY.withColor(guillemetColor)))
                .append(Text.literal(message == null ? "" : message)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.WHITE))));
    }
}
