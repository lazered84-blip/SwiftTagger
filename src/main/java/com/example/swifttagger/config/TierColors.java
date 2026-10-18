package com.example.swifttagger.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TierColors {
    private TierColors() {
    }

    public static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("HT1", "#e5b839");
        m.put("RHT1", "#d6a9eb");
        m.put("MT1", "#e1c375");
        m.put("RMT1", "#d6a9eb");
        m.put("LT1", "#d2b154");
        m.put("RLT1", "#d6a9eb");
        m.put("HT2", "#c2d0e4");
        m.put("RHT2", "#d6a9eb");
        m.put("MT2", "#b0b5c7");
        m.put("RMT2", "#d6a9eb");
        m.put("LT2", "#9ea5b0");
        m.put("RLT2", "#d6a9eb");
        m.put("HT3", "#f59d59");
        m.put("MT3", "#b9693e");
        m.put("LT3", "#985a35");
        m.put("HT4", "#a2a8ac");
        m.put("MT4", "#80868b");
        m.put("LT4", "#63676b");
        m.put("HT5", "#8f6b41");
        m.put("MT5", "#664e36");
        m.put("LT5", "#493725");
        return m;
    }

    public static int parseHex(String hex, int fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        try {
            String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseInt(cleaned, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
