package com.example.swifttagger.kit;

import java.util.Locale;

/**
 * Ordre de cycle des modes pour la touche "changer de mode" et calcul du nom
 * affiché (dans la barre d'action, l'écran de config et les suggestions de
 * la commande /kit).
 *
 * Règle d'affichage : première lettre en majuscule, reste en minuscule,
 * SAUF pour UHC et SMP qui restent inchangés (toujours en majuscules).
 */
public final class GamemodeCycle {

    /** AUCUN -> AXE -> DETECTION -> DIAMOND_POT -> MACE -> NETHERITE_POT -> SMP -> SWORD -> UHC -> VANILLA -> AUCUN ... */
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

    /** Renvoie le mode suivant dans le cycle après {@code current} (retombe sur AUCUN si inconnu). */
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

    /** Nom affiché : première lettre en majuscule, reste en minuscule ; UHC et SMP inchangés.
     * Les underscores internes (ex: "NETHERITE_POT") sont remplacés par des espaces
     * à l'affichage uniquement — la valeur canonique stockée garde l'underscore. */
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

    /**
     * Convertit une valeur quelconque (nom canonique déjà en majuscules avec
     * underscore, ou nom affiché avec espace/casse quelconque, ex: "Diamond pot"
     * ou "diamond_pot") vers la valeur canonique de {@link #ORDER} (ex:
     * "DIAMOND_POT"). Renvoie la valeur normalisée telle quelle si elle ne
     * correspond à aucune entrée connue (valeur custom d'une config éditée à
     * la main).
     */
    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) return "AUCUN";
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (String o : ORDER) {
            if (o.equals(normalized)) return o;
        }
        return normalized;
    }
}
