package com.example.swifttagger.api;

import java.util.Locale;

/**
 * Sanctions actives d'un joueur (Discord/serveur), qui remplacent
 * complètement l'affichage du tier partout où il apparaît (nametag, TAB,
 * chat, /swift tiers, classement) tant qu'elles sont actives : voir
 * PlayerProfile#sanction et TierResolver#resolve.
 *
 * Couleurs fixes, volontairement indépendantes de ModConfig#tierColors et
 * de ModConfig#useTierColor : une sanction reste toujours visible avec la
 * même couleur, quels que soient les réglages d'affichage des tiers.
 */
public enum SanctionType {
    RESTREINT("ʀᴇsᴛʀɪᴄᴛᴇᴅ", 0x610c1d),
    CHEATER("ᴄʜᴇᴀᴛᴇʀ", 0xa11f39),
    BLACKLIST("ʙʟᴀᴄᴋʟɪsᴛ", 0x202020);

    public final String label;
    public final int colorRgb;

    SanctionType(String label, int colorRgb) {
        this.label = label;
        this.colorRgb = colorRgb;
    }

    /** Reconnaît la valeur brute envoyée par l'API (insensible à la casse). Renvoie null si inconnue/absente. */
    public static SanctionType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SanctionType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Couleur effective de cette sanction : celle configurée dans la
     * catégorie "Couleur" de l'écran de config (ModConfig) si présente,
     * sinon la couleur fixe par défaut ci-dessus.
     */
    public int resolveColorRgb(com.example.swifttagger.config.ModConfig config) {
        if (config != null) {
            String hex = switch (this) {
                case RESTREINT -> config.sanctionRestreintColorHex;
                case CHEATER -> config.sanctionCheaterColorHex;
                case BLACKLIST -> config.sanctionBlacklistColorHex;
            };
            return com.example.swifttagger.config.TierColors.parseHex(hex, colorRgb);
        }
        return colorRgb;
    }

    /**
     * Renvoie la couleur effective d'une sanction si {@code text} correspond
     * exactement à l'un des libellés (Restreint/Cheater/Blacklist), sinon
     * null. Utilisé par TierTextRenderer#resolveTierColor pour que le
     * "tier" affiché (en réalité le libellé de sanction une fois substitué
     * par TierResolver) garde toujours sa propre couleur, même si ce texte
     * ne correspond à aucune entrée de ModConfig#tierColors.
     */
    public static Integer colorForLabel(com.example.swifttagger.config.ModConfig config, String text) {
        if (text == null) {
            return null;
        }
        for (SanctionType type : values()) {
            if (type.label.equalsIgnoreCase(text.trim())) {
                return type.resolveColorRgb(config);
            }
        }
        return null;
    }
}
