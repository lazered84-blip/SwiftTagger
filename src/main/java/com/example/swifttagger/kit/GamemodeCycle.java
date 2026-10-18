package com.example.swifttagger.kit;

import java.util.Locale;

public final class GamemodeCycle {

    public static final String[] ORDER = {
            "AUCUN",
            "AXE",
            "DETECTION",
            "DIAMOND_POT",
            "MACE",
            "NETHERITE_POT",
            "SMP",
            "SWORD",
            "UHC",
            "VANILLA"
    };

    private GamemodeCycle() {}

    public static String next(String current) {
        String normalized = current == null ? "AUCUN" : current.toUpperCase(Locale.ROOT);
        int index = -1;
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i].equals(normalized)) {
                index = i;
                break;
            }
        }
        int nextIndex = (index + 1) % ORDER.length;
        return ORDER[nextIndex];
    }

    public static String displayName(String mode) {
        if (mode == null || mode.isEmpty()) {
            return "";
        }
        String upper = mode.toUpperCase(Locale.ROOT);
        if (upper.equals("UHC") || upper.equals("SMP")) {
            return upper;
        }
        String spaced = upper.replace('_', ' ');
        return spaced.substring(0, 1) + spaced.substring(1).toLowerCase(Locale.ROOT);
    }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) return "AUCUN";
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (String o : ORDER) {
            if (o.equals(normalized)) return o;
        }
        return normalized;
    }
}
