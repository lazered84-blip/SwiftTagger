package com.example.swifttagger.api;

import java.util.Locale;

public enum SanctionType {
    RESTREINT("ʀᴇsᴛʀɪᴄᴛᴇᴅ", 0x610c1d),
    CHEATER("ᴄʜᴇᴀᴛᴇʀ", 0xa11f39),
    BLACKLIST("ʙʟᴀᴄᴋʟɪsᴛ", 0x202020);

    public final String label;
    public final int colorRgb;

    SanctionType(String label, int colorRgb) {
        this.label = label;
        this.colorRgb = colorRgb;
    }

    public static SanctionType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SanctionType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int resolveColorRgb(com.example.swifttagger.config.ModConfig config) {
        if (config != null) {
            String hex = switch (this) {
                case RESTREINT -> config.sanctionRestreintColorHex;
                case CHEATER -> config.sanctionCheaterColorHex;
                case BLACKLIST -> config.sanctionBlacklistColorHex;
            };
            return com.example.swifttagger.config.TierColors.parseHex(hex, colorRgb);
        }
        return colorRgb;
    }

    public static Integer colorForLabel(com.example.swifttagger.config.ModConfig config, String text) {
        if (text == null) {
            return null;
        }
        for (SanctionType type : values()) {
            if (type.label.equalsIgnoreCase(text.trim())) {
                return type.resolveColorRgb(config);
            }
        }
        return null;
    }
}
