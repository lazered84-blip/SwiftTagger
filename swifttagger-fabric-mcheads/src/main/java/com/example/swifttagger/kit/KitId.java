package com.example.swifttagger.kit;
/**
 * Identifiants internes des kits/gamemodes détectables.
 * Les alias (ex: "Neth Pot", "Netherite Potion" -> NETHERITE_POT) sont gérés
 * dans {@link KitDefinition#aliases()}, pas ici : il n'y a qu'UN SEUL id
 * interne par kit réel, jamais plusieurs kits pour de simples alias.
 */
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

    /**
     * Convertit une clé de mode telle que renvoyée par l'API (ex: "sword",
     * "netherite_pot") en KitId. Centralisé ici pour éviter la même
     * conversion dupliquée dans KitIcons, PlayerSearchScreen, TabListMixin...
     */
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
