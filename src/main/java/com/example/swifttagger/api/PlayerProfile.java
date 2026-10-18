package com.example.swifttagger.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProfile {
    public final UUID uuid;
    public final String name;
    public final Map<String, String> tiers = new HashMap<>();
    public final Map<String, String> peaks = new HashMap<>();
    public Integer rank;
    public Integer points;
    public long fetchedAtMillis;

    public SanctionType sanction;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public boolean hasSanction() {
        return sanction != null;
    }

    public int calculatePoints() {
        int total = 0;
        for (Map.Entry<String, String> entry : tiers.entrySet()) {
            String tier = peaks.getOrDefault(entry.getKey(), entry.getValue());
            total += tierScore(tier);
        }
        return total;
    }

    public int getPointsOrCalculated() {
        return points != null ? points : calculatePoints();
    }

    public String getHighestTier() {
        String best = null;
        int bestScore = -1;
        for (Map.Entry<String, String> entry : tiers.entrySet()) {
            String tier = peaks.getOrDefault(entry.getKey(), entry.getValue());
            int score = tierScore(tier);
            if (score > bestScore) {
                bestScore = score;
                best = tier;
            }
        }
        return best;
    }

    public String getHighestMode() {
        String bestMode = null;
        int bestScore = -1;
        for (Map.Entry<String, String> entry : tiers.entrySet()) {
            String tier = peaks.getOrDefault(entry.getKey(), entry.getValue());
            int score = tierScore(tier);
            if (score > bestScore) {
                bestScore = score;
                bestMode = entry.getKey();
            }
        }
        return bestMode;
    }

    private static int tierScore(String tier) {
        if (tier == null) return 0;
        return switch (tier.trim().toUpperCase()) {
            case "LT5" -> 1;
            case "MT5" -> 2;
            case "HT5" -> 3;
            case "LT4" -> 4;
            case "MT4" -> 5;
            case "HT4" -> 6;
            case "LT3" -> 8;
            case "MT3" -> 10;
            case "HT3" -> 15;
            case "RLT2" -> 20;
            case "LT2" -> 25;
            case "RMT2" -> 25;
            case "MT2" -> 30;
            case "RHT2" -> 30;
            case "HT2" -> 35;
            case "RLT1" -> 40;
            case "LT1" -> 45;
            case "RMT1" -> 45;
            case "MT1" -> 50;
            case "RHT1" -> 50;
            case "HT1" -> 60;
            default -> 0;
        };
    }

    public static int tierScorePublic(String tier) {
        return tierScore(tier);
    }
}
