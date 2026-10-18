package com.example.swifttagger.kit;

public record KitResult(KitId kitId, double coverage, int rawWeight) {
    public static final KitResult UNKNOWN = new KitResult(KitId.UNKNOWN, 0.0, 0);
    public boolean isUnknown() {
        return kitId == KitId.UNKNOWN;
    }
}
