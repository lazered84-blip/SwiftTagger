package com.example.swifttagger.config;

/**
 * Remplace {tier} {player} {kit} dans le gabarit configurable
 * (ModConfig#tierFormat). Volontairement séparé de tout code Minecraft
 * pour rester testable en Java pur. Utilisé pour la version "texte brut"
 * du format (ex: aperçu dans l'écran de config) ; l'affichage réel en jeu
 * (couleurs incluses) passe par {@link com.example.swifttagger.text.TierTextRenderer}.
 */
public final class TierFormat {
    private TierFormat() {
    }

    public static String apply(String template, String tier, String player, String kit) {
        if (template == null || template.isBlank()) {
            template = "{tier} {player}";
        }
        return template
                .replace("{tier}", tier == null ? "" : tier)
                .replace("{player}", player == null ? "" : player)
                .replace("{kit}", kit == null ? "" : kit);
    }
}
