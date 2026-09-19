package com.example.swifttagger.config;

import com.example.swifttagger.api.PlayerProfile;
import com.example.swifttagger.kit.KitId;

/**
 * Décide quel tier (et quel mode, pour l'icône) afficher pour un joueur
 * donné, en fonction de ModConfig#displayedTierMode :
 *
 *  - SELECTED : utilise ModConfig#selectedGamemode, qui peut valoir :
 *      - un mode précis (ex: "SWORD")  -> toujours le tier de ce mode ;
 *      - "DETECTION"                   -> le tier du kit détecté automa-
 *                                          tiquement (armure/inventaire/
 *                                          touche dédiée), comme l'ancien
 *                                          mode ADAPTIVE_HIGHEST ;
 *      - "AUCUN" (ou vide)             -> aucun mode forcé, repli sur le
 *                                          meilleur tier toutes catégories.
 *    Si le mode résolu n'a pas de tier connu pour ce joueur, repli
 *    également sur le meilleur tier toutes catégories.
 *
 *  - ABSOLUTE_HIGHEST (ou toute autre valeur, ex: ancien ADAPTIVE_HIGHEST
 *    d'une config non migrée) : le meilleur tier toutes catégories
 *    confondues, et son icône est celle du mode qui l'a produit (jamais
 *    d'icône vide).
 */
public final class TierResolver {
    private TierResolver() {
    }

    public static String modeKeyOf(KitId kitId) {
        if (kitId == null || kitId == KitId.UNKNOWN) {
            return null;
        }
        return kitId.name().toLowerCase();
    }

    /**
     * @param profile     profil complet du joueur (tiers + peaks)
     * @param detectedKit kit détecté pour ce joueur à cet instant (peut être UNKNOWN)
     * @return le tier à afficher, ou null si rien à afficher
     */
    public static String resolve(ModConfig config, PlayerProfile profile, KitId detectedKit) {
        if (config == null || profile == null) {
            return null;
        }
        // Une sanction active (Restreint/Cheater/Blacklist) remplace
        // TOUJOURS le tier, quel que soit le mode choisi/détecté et même
        // si le joueur n'a aucun tier connu (profile.tiers vide).
        if (profile.sanction != null) {
            return profile.sanction.label;
        }
        String mode = resolveMode(config, profile, detectedKit);
        if (mode != null) {
            return profile.tiers.get(mode);
        }
        return profile.getHighestTier();
    }

    /**
     * Renvoie le mode (clé JSON, ex: "sword") dont le tier a effectivement
     * été utilisé par {@link #resolve}, pour savoir quel émoji de mode
     * afficher à côté du tier (section 6). Ne renvoie jamais null en
     * ABSOLUTE_HIGHEST tant qu'un mode a un tier connu : l'icône du mode le
     * plus élevé est toujours utilisée, pour qu'une icône soit toujours
     * affichée quand un tier l'est.
     */
    public static String resolveMode(ModConfig config, PlayerProfile profile, KitId detectedKit) {
        if (config == null || profile == null) {
            return null;
        }
        // Sanctionné : aucune icône de mode, seul le libellé de sanction
        // (voir #resolve) doit apparaître.
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
            // Mode "AUCUN", vide, ou mode choisi/détecté sans tier connu :
            // repli sur le meilleur tier toutes catégories (et son icône).
            return profile.getHighestMode();
        }

        // ABSOLUTE_HIGHEST (ou valeur inconnue, ex: ancien ADAPTIVE_HIGHEST) :
        // toujours le mode du meilleur tier, pour que son icône s'affiche.
        return profile.getHighestMode();
    }
}
