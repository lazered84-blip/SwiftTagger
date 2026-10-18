package com.example.swifttagger.config;

import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.kit.KitId;

public final class TierResolver {
    private TierResolver() {
    }

    public static String modeKeyOf(KitId kitId) {
        if (kitId == null || kitId == KitId.UNKNOWN) {
            return null;
        }
        return kitId.name().toLowerCase();
    }

    public static String resolve(ModConfig config, PlayerProfile profile, KitId detectedKit) {
        if (config == null || profile == null) {
            return null;
        }

        if (profile.sanction != null) {
            return profile.sanction.label;
        }
        String mode = resolveMode(config, profile, detectedKit);
        if (mode != null) {
            return profile.tiers.get(mode);
        }
        return profile.getHighestTier();
    }

    public static String resolveMode(ModConfig config, PlayerProfile profile, KitId detectedKit) {
        if (config == null || profile == null) {
            return null;
        }

        if (profile.sanction != null) {
            return null;
        }

        if ("SELECTED".equalsIgnoreCase(config.displayedTierMode)) {
            String selected = config.selectedGamemode;
            if (selected != null && !selected.isBlank() && !selected.equalsIgnoreCase("AUCUN")) {
                String mode = selected.equalsIgnoreCase("DETECTION")
                        ? modeKeyOf(detectedKit)
                        : selected.toLowerCase();
                if (mode != null && profile.tiers.get(mode) != null) {
                    return mode;
                }
            }

            return profile.getHighestMode();
        }

        return profile.getHighestMode();
    }
}
