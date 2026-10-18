package com.example.swifttagger.config;

public final class TierFormat {
    private TierFormat() {
    }

    public static String apply(String template, String tier, String player, String kit) {
        if (template == null || template.isBlank()) {
            template = "{tier} {player}";
        }
        return template
                .replace("{tier}", tier == null ? "" : tier)
                .replace("{player}", player == null ? "" : player)
                .replace("{kit}", kit == null ? "" : kit);
    }
}
