package com.example.swifttagger.text;

import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.config.TierColors;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduit le gabarit configurable ModConfig#tierFormat ({tier} {player}
 * {kit}) en un vrai Text coloré. C'est le SEUL endroit du mod qui construit
 * le texte final affiché, pour que chat / tab / nametag restent
 * cohérents entre eux.
 *
 * Depuis ce correctif :
 *  - {icons} est un nouveau placeholder, au même titre que {tier}/{player}/
 *    {kit}/{sep} : il permet de déplacer l'icône du mode n'importe où dans
 *    le gabarit, indépendamment de la position du tier. Si le gabarit ne
 *    contient PAS {icons} (anciens gabarits / rétrocompatibilité), l'icône
 *    reste collée juste avant la valeur du tier, comme avant ;
 *  - le séparateur (ModConfig#separator) est inséré automatiquement du
 *    côté qui vient EN PREMIER entre {player} et {tier} dans le gabarit
 *    utilisé : si {tier} est placé avant {player}, le séparateur suit le
 *    tier (et inversement), au lieu d'être toujours collé après {player}.
 */
public final class TierTextRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{tier}|\\{player}|\\{kit}|\\{sep}|\\{icons}");
    private static final int FALLBACK_TIER_COLOR = 0xAAAAAA;

    private TierTextRenderer() {
    }

    /** Utilise ModConfig#tierFormat comme gabarit, sans icône (rétrocompatibilité). */
    public static MutableText build(ModConfig config, String tier, String kitDisplayName, String playerName) {
        return build(config, config.tierFormat, tier, kitDisplayName, Text.literal(""), Text.literal(playerName));
    }

    /**
     * Variante qui part du Text original du pseudo (couleur/préfixe
     * LuckPerms, groupe, etc.) plutôt que d'un simple String, pour que
     * ModConfig#nicknameColorMode = SERVER (défaut) puisse le préserver tel
     * quel. À utiliser partout où le pseudo vient déjà d'un Text stylé par
     * le serveur (nametag, TAB).
     */
    public static MutableText build(ModConfig config, String tier, String kitDisplayName, Text playerText) {
        return build(config, config.tierFormat, tier, kitDisplayName, Text.literal(""), playerText);
    }

    /**
     * Variante avec icône de mode (section 6) : l'icône déjà stylée
     * (cf. {@link com.example.swifttagger.kit.KitIcons#iconText}) pour le
     * kit résolu est collée juste avant la valeur du tier, quel que soit le
     * gabarit. Utilisée par le nametag, le TAB et le chat pour que l'icône
     * apparaisse partout où le tier est affiché, pas seulement dans
     * l'écran de profil.
     */
    public static MutableText build(ModConfig config, String tier, String kitDisplayName, Text kitIcon, Text playerText) {
        return build(config, config.tierFormat, tier, kitDisplayName, kitIcon, playerText);
    }

    /** Construit uniquement le badge icône+tier, sans pseudo. Utilisé dans le chat
     * lorsque le serveur fournit déjà son propre préfixe LuckPerms/pseudo. */
    public static MutableText buildBadge(ModConfig config, String tier, Text kitIcon, int tierColorRgb) {
        MutableText badge = Text.literal("");
        boolean iconBeforeTier = isIconBeforePlayer(config);
        Text tintedIcon = tintIcon(config, kitIcon, tierColorRgb);
        boolean hasIcon = tintedIcon != null && !tintedIcon.getString().isEmpty();

        if (hasIcon && iconBeforeTier) {
            badge.append(tintedIcon).append(Text.literal(" "));
        }
        badge.append(colored(tier, tierColorRgb));
        if (hasIcon && !iconBeforeTier) {
            badge.append(Text.literal(" ")).append(tintedIcon);
        }
        return badge;
    }

    /** Variante avec gabarit explicite (utilisée pour le badge de chat, qui
     *  ne réutilise qu'une partie du gabarit complet). */
    public static MutableText build(ModConfig config, String template, String tier, String kitDisplayName, String playerName) {
        return build(config, template, tier, kitDisplayName, Text.literal(""), Text.literal(playerName));
    }

    private static MutableText build(ModConfig config, String template, String tier, String kitDisplayName, Text kitIcon, Text playerText) {
        if (template == null || template.isBlank()) {
            template = "{tier} {player}";
        }
        // Le réglage "Position icône + tier" est la source de vérité.
        // Le champ tierFormat peut rester sur son ancien ordre : on reconstruit
        // ici le badge autour du pseudo afin que le changement soit réellement
        // visible dans chat, TAB et nametag.
        template = applyBadgePosition(config, template);
        template = applySeparator(config, template);

        int tierColorRgb = resolveTierColor(config, tier);
        boolean hasIconsToken = template.contains("{icons}");
        boolean hasIcon = kitIcon != null && !kitIcon.getString().isEmpty();

        MutableText templated = Text.literal("");
        Matcher matcher = TOKEN.matcher(template);
        int last = 0;

        while (matcher.find()) {
            if (matcher.start() > last) {
                templated.append(Text.literal(template.substring(last, matcher.start())));
            }

            switch (matcher.group()) {
                case "{tier}" -> {
                    // Rétrocompatibilité : si le gabarit ne contient pas
                    // {icons} explicitement, l'icône reste collée juste
                    // avant le tier, comme avant l'ajout de {icons}.
                    if (!hasIconsToken && hasIcon) {
                        templated.append(tintIcon(config, kitIcon, tierColorRgb)).append(Text.literal(" "));
                    }
                    templated.append(colored(tier, tierColorRgb));
                }
                case "{icons}" -> {
                    if (hasIcon) {
                        templated.append(tintIcon(config, kitIcon, tierColorRgb));
                    }
                }
                case "{kit}" -> templated.append(colored(kitDisplayName, tierColorRgb));
                // SERVER (défaut) : le pseudo garde sa couleur/style d'origine
                // (groupe LuckPerms, préfixe serveur, etc.). Tout autre mode
                // force la couleur choisie dans "Couleur du pseudo", cf.
                // applyNicknameColor.
                case "{player}" -> templated.append(applyNicknameColor(config, playerText, tierColorRgb));
                case "{sep}" -> templated.append(separatorText(config));
                default -> { /* ne devrait jamais arriver, cf. TOKEN */ }
            }

            last = matcher.end();
        }

        if (last < template.length()) {
            templated.append(Text.literal(template.substring(last)));
        }

        return templated;
    }

    /**
     * Applique la couleur du tier à l'icône tout en conservant sa police
     * personnalisée (ou son style d'origine si l'icône est personnalisée).
     */
    private static MutableText tintIcon(ModConfig config, Text icon, int rgb) {
        if (icon == null || icon.getString().isEmpty()) {
            return Text.literal("");
        }
        // VANILLA (le caractère est rendu par la police normale du client,
        // comme un préfixe LuckPerms) : coloré avec le tier uniquement si
        // "Icônes de la couleur du tier" est activé. Les sprites PNG des
        // autres packs (PVPHQ/MCTIERS/PVPTIERS) gardent toujours leurs
        // couleurs d'origine, quel que soit ce réglage.
        if (config != null && "VANILLA".equalsIgnoreCase(config.iconStyle) && config.vanillaIconTierColor) {
            return icon.copy().setStyle(icon.getStyle().withColor(TextColor.fromRgb(rgb)));
        }
        return icon.copy();
    }

    /** Applique la règle CHARACTER/TEXTURE aux écrans qui construisent leurs propres lignes. */
    public static MutableText tintIconForDisplay(ModConfig config, Text icon, int rgb) {
        return tintIcon(config, icon, rgb);
    }

    private static MutableText colored(String value, int rgb) {
        return Text.literal(value == null ? "" : value)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    /**
     * Place le bloc {icons} + {tier} autour de {player}.
     * GAUCHE  = {icons} {tier} {player}
     * DROITE  = {player} {tier} {icons}
     * Les autres tokens/éléments du gabarit sont conservés.
     */
    private static String applyBadgePosition(ModConfig config, String template) {
        if (config == null || template == null) return template;
        if (!template.contains("{player}") || !template.contains("{tier}")) return template;

        String cleaned = template
                .replace("{icons}", "")
                .replace("{tier}", "")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();

        int playerIdx = cleaned.indexOf("{player}");
        if (playerIdx < 0) return template;

        String badge = "GAUCHE".equalsIgnoreCase(config.badgePosition)
                ? "{icons} {tier} {player}"
                : "{player} {tier} {icons}";

        return cleaned.substring(0, playerIdx)
                + badge
                + cleaned.substring(playerIdx + "{player}".length());
    }

    /**
     * Si ModConfig#showSeparator est activé et que le gabarit ne contient pas
     * déjà {sep} explicitement (utilisateur avancé qui a personnalisé son
     * format), insère automatiquement " {sep} " du côté qui vient EN
     * PREMIER entre {tier} et {player} dans CE gabarit précis : le
     * séparateur suit toujours le tier, où qu'il soit placé.
     */
    private static String applySeparator(ModConfig config, String template) {
        if (!config.showSeparator || template.contains("{sep}")) {
            return template;
        }
        int tierIdx = template.indexOf("{tier}");
        int playerIdx = template.indexOf("{player}");

        if (tierIdx >= 0 && (playerIdx < 0 || tierIdx < playerIdx)) {
            return template.replace("{tier}", "{tier} {sep}");
        }
        if (playerIdx >= 0) {
            return template.replace("{player}", "{player} {sep}");
        }
        return "{sep} " + template;
    }

    /** Texte du séparateur (ModConfig#separator), en gris neutre comme le reste de la ponctuation. */
    private static Text separatorText(ModConfig config) {
        String sep = config.separator;
        if (sep == null || sep.isBlank()) {
            sep = "｜";
        }
        return Text.literal(sep).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(FALLBACK_TIER_COLOR)));
    }

    /**
     * true si {tier} apparaît avant {player} dans ModConfig#tierFormat (ou
     * si {player} est absent du gabarit) : utilisé par le TAB pour placer
     * le(s) badge(s) tier du même côté que le gabarit général, et par le
     * séparateur pour rester collé au tier partout (TAB compris).
     */
    public static boolean isTierBeforePlayer(ModConfig config) {
        String template = (config != null) ? config.tierFormat : null;
        if (template == null || template.isBlank()) {
            template = "{tier} {player}";
        }
        int tierIdx = template.indexOf("{tier}");
        int playerIdx = template.indexOf("{player}");
        if (tierIdx < 0) {
            return false;
        }
        return playerIdx < 0 || tierIdx < playerIdx;
    }

    /**
     * Détermine de quel côté du pseudo se trouve l'icône. Pour les anciens
     * gabarits sans {icons}, l'icône historique était collée avant {tier},
     * donc elle est considérée du même côté que le tier.
     */
    public static boolean isIconBeforePlayer(ModConfig config) {
        // Le réglage "Position du badge" déplace toujours l'icône ET le tier
        // ensemble. Il devient la source de vérité pour TAB/chat/nametag.
        if (config != null && config.badgePosition != null) {
            return "GAUCHE".equalsIgnoreCase(config.badgePosition);
        }
        return isTierBeforePlayer(config);
    }

    /** true si le badge complet (icône+tier) doit être à gauche du pseudo. */
    public static boolean isBadgeBeforePlayer(ModConfig config) {
        return isIconBeforePlayer(config);
    }

    /**
     * Couleur d'un tier (case-insensible : l'API peut renvoyer "ht1" ou "HT1").
     *
     * Cas particulier : quand {@code tier} est en réalité un libellé de
     * sanction (Restreint/Cheater/Blacklist, substitué au tier par
     * TierResolver#resolve pour un joueur sanctionné), sa couleur fixe
     * (com.example.swifttagger.api.SanctionType) est utilisée à la place,
     * peu importe ModConfig#useTierColor ou ModConfig#tierColors : une
     * sanction doit toujours rester visible avec sa propre couleur.
     */
    public static int resolveTierColor(ModConfig config, String tier) {
        Integer sanctionColor = com.example.swifttagger.api.SanctionType.colorForLabel(config, tier);
        if (sanctionColor != null) {
            return sanctionColor;
        }
        if (config == null || !config.useTierColor || tier == null) {
            return FALLBACK_TIER_COLOR;
        }
        String hex = config.tierColors != null ? config.tierColors.get(tier.trim().toUpperCase()) : null;
        return TierColors.parseHex(hex, FALLBACK_TIER_COLOR);
    }

    /**
     * Applique ModConfig#nicknameColorMode au pseudo, ou le laisse intact en
     * mode SERVER (défaut). Corrige un bug où ce réglage n'avait plus aucun
     * effet (le pseudo gardait toujours sa couleur d'origine quel que soit
     * le mode choisi) : "Couleur du pseudo" = TIER_COLOR, par exemple,
     * n'affichait jamais rien de différent avant ce correctif.
     * Ne remplace que la couleur, pas le reste du style (gras, clic, etc.).
     */
    private static MutableText applyNicknameColor(ModConfig config, Text playerText, int tierColorRgb) {
        if (config == null || config.nicknameColorMode == null || "SERVER".equalsIgnoreCase(config.nicknameColorMode)) {
            return playerText.copy();
        }
        // Normalise avant comparaison : la valeur affichée dans l'UI est
        // "Tier color" (au lieu de l'ancien "TIER_COLOR"), donc on compare
        // sans tenir compte de la casse ni des espaces/underscores.
        String modeKey = config.nicknameColorMode.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
        int rgb = switch (modeKey) {
            case "TIER_COLOR" -> tierColorRgb;
            default -> -1; // valeur inconnue (config éditée à la main) : on ne touche pas au pseudo
        };
        if (rgb < 0) {
            return playerText.copy();
        }
        return playerText.copy().setStyle(playerText.getStyle().withColor(TextColor.fromRgb(rgb)));
    }
}
