package com.example.swifttagger.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {

    public boolean enabled = true;

    public boolean autoDetectEnabled = true;

    public double minMatchCoverage = 0.30;

    public int minMatchRawWeight = 3;

    public List<String> kitPriorityOrder = new ArrayList<>(List.of(
            "MACE", "SMP", "NETHERITE_POT", "AXE", "UHC", "DIAMOND_POT", "SWORD", "VANILLA"
    ));

    public String apiBaseUrl = "http://82.47.159.62:3000";

    public boolean showTier = true;

    public String displayedTierMode = "Highest";

    public String selectedGamemode = "SWORD";

    public String tierFormat = "{player} {tier}";

    public String badgePosition = "Droite";

    public boolean useTierColor = true;

    public Map<String, String> tierColors = new LinkedHashMap<>(TierColors.defaults());

    public String nicknameColorMode = "Tier color";

    public int customNicknameColorRgb = 16777215;

    public String guillemetColorHex = "#06b7fe";

    public String sanctionRestreintColorHex = "#610c1d";
    public String sanctionCheaterColorHex = "#a11f39";
    public String sanctionBlacklistColorHex = "#202020";

    public Map<String, String> kitIcons = new LinkedHashMap<>(com.example.swifttagger.kit.KitIcons.defaults());

    public String iconStyle = "PVPHQ";

    public boolean vanillaIconTierColor = true;

    public boolean showSeparator = false;

    public String separator = "|";

    public boolean showOnNametag = true;
    public boolean showOnTab = true;
    public boolean showOnChat = true;

    public int searchKeyCode = 72;
    public int autoDetectKeyCode = 89;
    public int cycleModeKeyCode = 74;
}
