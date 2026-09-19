package com.example.swifttagger.kit;
/**
 * Résultat d'une détection : le kit choisi (ou UNKNOWN), son score de
 * correspondance (0..1, "coverage" = éléments trouvés / éléments attendus),
 * et le poids brut correspondant (utilisé en interne pour départager
 * plusieurs kits candidats, voir KitDetector).
 */
public record KitResult(KitId kitId, double coverage, int rawWeight) {
    public static final KitResult UNKNOWN = new KitResult(KitId.UNKNOWN, 0.0, 0);
    public boolean isUnknown() {
        return kitId == KitId.UNKNOWN;
    }
}
