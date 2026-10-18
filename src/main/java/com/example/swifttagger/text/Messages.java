package com.example.swifttagger.text;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Messages {
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    static {
        put("screen.tiers_of", "Tiers de {player}");
        put("search.player", "PLAYER: {player}");
        put("button.reload", "Recharger");
        put("button.namemc", "NameMC");
        put("profile.searching", "Recherche du profil...");
        put("profile.not_found", "Joueur non classé ou introuvable");
        put("profile.rank", "Classement : {rank}");
        put("profile.points", "Points : {points}");

        put("profile.rank_label", "Classement : ");
        put("profile.points_label", "Points : ");
        put("profile.tiers", "Tiers");
        put("profile.peak", "  Peak : {tier}");
        put("profile.no_tier", "Aucun tier connu pour ce joueur.");
        put("chat.unranked", "Unranked");
        put("chat.rank", "Rank");
        put("chat.rank_na", "Classement : N/A");
        put("chat.separator", "⎯⎯⎯⎯⎯⎯⎯⎯⎯");
        put("chat.peak_open", " (peak : ");
        put("chat.peak_close", ")");
        put("chat.points", "Points : {points}");
        put("chat.ranking", "Classement : #{rank}");
        put("search.loading", "Chargement...");
        put("search.top", "TOP {rank} ({points} points)");
        put("search.tiers", "TIERS:");
        put("search.retired_tiers", "RETIRED TIERS:");
        put("config.title", "Configuration SwiftTagger");
        put("config.category.general", "Général");
        put("config.category.display", "Affichage");
        put("config.category.icons", "Icônes");
        put("config.category.color", "Couleur");
        put("config.color.guillemet", "Couleur du guillemet »");
        put("config.color.guillemet.tooltip", "Couleur du « » affiché devant chaque message du mod dans la barre d'action.");
        put("config.color.sanction_restreint", "Couleur Restreint");
        put("config.color.sanction_cheater", "Couleur Cheater");
        put("config.color.sanction_blacklist", "Couleur Blacklist");

        put("config.enabled", "Statut du tiertagger");
        put("config.enabled.tooltip", "Active ou désactive complètement le mod. Aucun tier n'est alors affiché nulle part.");
        put("config.show_tier", "Afficher les tiers");
        put("config.show_tier.tooltip", "Affiche ou masque le tier à côté du pseudo.");
        put("config.badge_position", "Position icône + tier");
        put("config.badge_position.tooltip", "Place l'icône et le tier avant (Gauche) ou après (Droite) le pseudo.");
        put("config.show_separator", "Séparateur");
        put("config.show_separator.tooltip", "Affiche un ｜ entre le tier et le pseudo.");
        put("config.show_chat", "Afficher dans le chat");
        put("config.show_chat.tooltip", "Affiche le tier dans les messages du chat.");
        put("config.show_tab", "Afficher dans le TAB");
        put("config.show_tab.tooltip", "Affiche le tier dans la liste des joueurs (TAB).");
        put("config.show_nametag", "Afficher au-dessus des joueurs");
        put("config.show_nametag.tooltip", "Affiche le tier au-dessus de la tête des joueurs, dans le monde.");
        put("config.nickname_color", "Couleur du pseudo");
        put("config.nickname_color.tooltip", "Server garde la couleur d'origine, Tier color reprend la couleur du tier.");
        put("config.use_tier_color", "Couleur du tier");
        put("config.use_tier_color.tooltip", "Si désactivé, le tier s'affiche en gris neutre au lieu de sa couleur habituelle.");
        put("config.icon_style", "Pack d'icônes");
        put("config.icon_style.tooltip", "Choisit le pack d'icônes utilisé pour représenter les kits détectés. Un seul pack est actif à la fois.");
        put("config.vanilla_icon_tier_color", "Icônes de la couleur du tier");
        put("config.vanilla_icon_tier_color.tooltip", "Colore l'icône avec la couleur du tier, comme le texte du tier. Ne fonctionne qu'avec le pack d'icônes Vanilla.");
        put("config.auto_detect", "Détection automatique de kit");
        put("config.auto_detect.tooltip", "Détecte automatiquement le kit selon l'inventaire, sans avoir à appuyer sur une touche.");
        put("config.displayed_tier_mode", "Mode d'affichage");
        put("config.displayed_tier_mode.tooltip", "Selected affiche toujours le mode choisi ci-dessous. Highest affiche le meilleur tier, tous modes confondus.");
        put("config.selected_gamemode", "Mode");
        put("config.selected_gamemode.tooltip", "Mode de jeu affiché lorsque le mode d'affichage est réglé sur Selected.");

        put("action.no_nearby", "Aucun joueur aux alentours.");
        put("action.gamemode_unknown", "Gamemode : Unknown");
        put("action.gamemode_tier", "Gamemode : {mode}  |  Tier : {tier}");
        put("action.gamemode_unranked", "Gamemode : {mode}  |  Tier : Unranked");

        put("leaderboard.title", "Classement");
        put("leaderboard.loading", "Chargement du classement...");
        put("leaderboard.empty", "Aucun joueur classé.");
        put("leaderboard.error", "Impossible de charger le classement.");
        put("leaderboard.total", "Joueurs classés : {count}");
        put("leaderboard.header_rank", "#");
        put("leaderboard.header_player", "Joueur");
        put("leaderboard.header_points", "Points");
        put("leaderboard.click_hint", "Clique sur un joueur pour ouvrir son profil");
        put("command.leaderboard_open", "Ouverture du classement...");

        put("command.open_profile", "Ouverture du profil de {player}...");
        put("command.open_own_profile", "Ouverture de ton profil {player}...");
        put("command.config_reloaded", "Configuration rechargée.");
        put("command.enabled", "Activé.");
        put("command.disabled", "Désactivé.");
        put("command.mode_unknown", "Mode inconnu : {mode}. Valeurs possibles : {values}");
        put("command.mode_selected", "Mode sélectionné : {mode}");
        put("command.player_argument", "joueur");
        put("command.mode_argument", "mode");

        put("key.search_nearest", "Chercher un tier");
        put("key.detect_kit", "Détecter le kit");
        put("key.category", "SwiftTagger");
    }

    private Messages() {}

    private static void put(String key, String value) { VALUES.put(key, value); }

    public static String get(String key) {
        return VALUES.getOrDefault(key, key);
    }

    public static String get(String key, Object... replacements) {
        String result = get(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        return result;
    }
}
