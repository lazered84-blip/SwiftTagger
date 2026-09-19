package com.example.swifttagger.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration persistée (config/swifttagger.json). Couvre la section 11 de
 * la spec ("Général" / "Tier" / "Emplacements" / "Affichage").
 *
 * NOTE MIGRATION : Gson construit cet objet via son constructeur sans
 * argument, donc les valeurs par défaut ci-dessous s'appliquent telles
 * quelles à tout champ absent d'un ancien fichier swifttagger.json (voir
 * ConfigManager) — pas besoin de migration explicite.
 */
public class ModConfig {

    // ================= Général (section 11) =================

    /** Coupe-circuit global : désactive tout le mod sans le désinstaller (section 9). */
    public boolean enabled = true;

    /** Détection automatique de kit en tâche de fond (section 13), en plus de la touche dédiée. */
    public boolean autoDetectEnabled = true;

    /** Seuil minimal de couverture en dessous duquel le mod répond "Unknown" (section 16). */
    public double minMatchCoverage = 0.30;

    /** Poids brut minimal absolu pour ignorer le bruit (1-2 items isolés). */
    public int minMatchRawWeight = 3;

    /** Ordre de priorité des kits en cas d'égalité totale (section 5 "Égalités"), noms de KitId. */
    public List<String> kitPriorityOrder = new ArrayList<>(List.of(
            "MACE", "SMP", "NETHERITE_POT", "AXE", "UHC", "DIAMOND_POT", "SWORD", "VANILLA"
    ));

    /** URL de base de l'API tiers (jamais hardcodée de façon impossible à modifier, cf. section 6). */
    public String apiBaseUrl = "http://82.47.159.62:3000";

    // ================= Tier (section 6-7-8-12) =================

    /** Afficher le tier du tout (indépendant des emplacements individuels). */
    public boolean showTier = true;

    /** Selected ou Highest (section 6 "Position du tier" / logique d'affichage). */
    public String displayedTierMode = "Highest";

    /**
     * Mode utilisé quand displayedTierMode = SELECTED. Valeurs possibles :
     * un mode précis ("SWORD", "AXE", "SMP", "NETHERITE_POT", "DIAMOND_POT",
     * "VANILLA", "UHC", "MACE"), "AUCUN" (aucun mode forcé, repli sur le
     * meilleur tier), ou "DETECTION" (mode détecté automatiquement via la
     * touche/l'auto-détection, comme l'ancien mode ADAPTIVE_HIGHEST).
     */
    public String selectedGamemode = "SWORD";

    /** Gabarit configurable (section 12) : placeholders {tier} {player} {kit}. */
    public String tierFormat = "{player} {tier}";

    /** Position globale du badge icône+tier par rapport au pseudo : Gauche ou Droite. */
    public String badgePosition = "Droite";

    /** Si false, {tier}/{kit} sont affichés en gris neutre plutôt qu'avec la couleur du tier. */
    public boolean useTierColor = true;

    /** Couleurs par tier (section 6), copiées depuis TierColors#defaults() au premier lancement. */
    public Map<String, String> tierColors = new LinkedHashMap<>(TierColors.defaults());

    /**
     * Server (préserve la couleur/style d'origine du pseudo, ex: groupe
     * LuckPerms) ou Tier color (section 7 : couleur du pseudo indépendante
     * du tier). Server est le seul mode qui ne recolore jamais le pseudo ;
     * Tier color force la couleur du tier, cf. TierTextRenderer#applyNicknameColor.
     *
     * Tier color par défaut : le pseudo doit être de la même couleur que le
     * tier affiché, partout (chat/TAB/nametag), sans que l'utilisateur ait
     * à aller changer ce réglage.
     */
    public String nicknameColorMode = "Tier color";

    /** Conservé pour compatibilité avec d'anciennes configs (mode CUSTOM retiré de l'UI). */
    public int customNicknameColorRgb = 16777215;

    // ================= Couleur (catégorie config dédiée) =================
    // Champs texte (hex "#rrggbb") plutôt que startColorField : ce dernier a
    // besoin de me.shedaniel.math.Color, une classe de Cloth Config absente
    // du classpath de compilation (dépendance transitive non résolue) — voir
    // l'erreur de build "cannot access Color". Un simple champ texte hex,
    // parsé avec TierColors#parseHex, évite complètement ce problème.

    /** Couleur du « » (guillemet) devant chaque message du mod, dans la barre d'action. */
    public String guillemetColorHex = "#06b7fe";

    /** Couleurs des libellés de sanction (section "Couleur"). */
    public String sanctionRestreintColorHex = "#610c1d";
    public String sanctionCheaterColorHex = "#a11f39";
    public String sanctionBlacklistColorHex = "#202020";

    /** Émoji affiché devant chaque mode (section 6), voir KitIcons#defaults(). */
    public Map<String, String> kitIcons = new LinkedHashMap<>(com.example.swifttagger.kit.KitIcons.defaults());

    /**
     * TEXTURE = sprites PNG embarqués du mod (police custom, rendu identique
     * partout, cf. KitIcons). CHARACTER = vrais caractères emoji Unicode
     * (🪓 🔨 ☠ ⚗ ⛨ 🗡 ❤ ✦), rendus par la police normale du client, comme un
     * préfixe LuckPerms. Peuvent apparaître en carré vide ("tofu") selon la
     * police du client/du serveur de resource packs, contrairement à TEXTURE.
     */
    /** PVPHQ = sprites du mod, VANILLA = caractères vanilla. MCTIERS/PVPTIERS sont prêts à être configurés.
     * Le pack sélectionné ici est le seul affiché : pas d'interrupteur séparé par pack, ce serait redondant. */
    public String iconStyle = "PVPHQ";

    /** Uniquement pris en compte si iconStyle = VANILLA : colore le caractère avec la couleur du tier
     * (comme {tier}), au lieu de garder la couleur de texte par défaut. Voir TierTextRenderer#tintIcon. */
    public boolean vanillaIconTierColor = true;

    /** Affiche une barre (ou tout autre caractère) entre le pseudo et le tier, partout où le tier est affiché. */
    public boolean showSeparator = false;

    /** Caractère(s) utilisé(s) comme séparateur si showSeparator est activé (ex: "｜", "•", "-"). */
    public String separator = "|";

    // ================= Emplacements (section 9-10) =================

    public boolean showOnNametag = true;
    public boolean showOnTab = true;
    public boolean showOnChat = true;

    // ================= Touches (section 1/13) =================

    public int searchKeyCode = 72;    // H : recherche le tier du joueur le plus proche
    public int autoDetectKeyCode = 89; // Y : force une détection immédiate + message dans le chat local
    public int cycleModeKeyCode = 74; // J : passe au mode suivant (voir GamemodeCycle), affiché dans la barre d'action
}
