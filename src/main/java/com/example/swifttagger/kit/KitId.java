package com.example.swifttagger.kit;

public enum KitId {
    SWORD,
    AXE,
    SMP,
    NETHERITE_POT,
    DIAMOND_POT,
    VANILLA,
    UHC,
    MACE,
    UNKNOWN;

    public static KitId fromModeKey(String modeKey) {
        if (modeKey == null) {
            return UNKNOWN;
        }
        try {
            return KitId.valueOf(modeKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
