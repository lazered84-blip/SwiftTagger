package com.example.swifttagger.text;

import com.example.swifttagger.config.ModConfig;
import com.example.swifttagger.kit.KitIcons;
import com.example.swifttagger.kit.KitId;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Locale;

/**
 * Construit les préfixes standards utilisés devant les messages envoyés par
 * le mod, désormais quasiment tous dans la barre d'action :
 *
 *  - {@link #success(String)}                 : &a&l✅ &a» &fMessage
 *  - {@link #error(String)}                   : &c&l❌ &c» &fMessage
 *  - {@link #info(String)}                    : &#06b7fe⚔ &#06b7fe» &fMessage
 *  - {@link #withIcon(MutableText, String)}   : icône&r&a» &fMessage — utilisé
 *    à la place de success() dès qu'un message concerne un mode de jeu
 *    précis (détection auto, touche de cycle, /swift kit) : l'icône du mode
 *    remplace le ✅ générique.
 *  - {@link #forMode(ModConfig, String, String)} : choisit automatiquement
 *    entre error() (mode "AUCUN"), withIcon() (mode réel connu) et success()
 *    (valeur sans icône dédiée, ex: "DETECTION").
 *
 * Dans TOUS les cas : seuls l'icône et le « » sont colorés, le texte du
 * message lui-même est TOUJOURS blanc.
 */
public final class SwiftMessage {

    private static final TextColor BLUE = TextColor.fromRgb(0x06b7fe);
    private static final TextColor GREEN = TextColor.fromFormatting(Formatting.GREEN);
    private static final TextColor RED = TextColor.fromFormatting(Formatting.RED);

    private SwiftMessage() {
    }

    /** Message de réussite / validation : &a&l✅ &a» &fMessage */
    public static MutableText success(String message) {
        MutableText icon = Text.literal("✅ ").setStyle(Style.EMPTY.withColor(GREEN).withBold(true));
        return build(icon, GREEN, message);
    }

    /** Message d'erreur / refus : &c&l❌ &c» &fMessage */
    public static MutableText error(String message) {
        MutableText icon = Text.literal("❌ ").setStyle(Style.EMPTY.withColor(RED).withBold(true));
        return build(icon, RED, message);
    }

    /** Autres messages (neutres / informatifs) : &#06b7fe⚔ &#06b7fe» &fMessage */
    public static MutableText info(String message) {
        MutableText icon = Text.literal("⚔ ").setStyle(Style.EMPTY.withColor(BLUE));
        return build(icon, BLUE, message);
    }

    /**
     * Variante avec une icône personnalisée (ex : l'icône du mode de jeu
     * détecté/sélectionné) à la place du ✅ habituel. Le « » reste dans la
     * même famille que success() (vert), le message reste blanc.
     */
    public static MutableText withIcon(MutableText icon, String message) {
        MutableText prefixed = Text.literal("");
        if (icon != null) {
            prefixed.append(icon);
        }
        prefixed.append(Text.literal(" "));
        return build(prefixed, GREEN, message);
    }

    /**
     * Choisit automatiquement le bon style selon le nom de mode (tel que
     * stocké dans ModConfig#selectedGamemode / renvoyé par GamemodeCycle) :
     *  - "AUCUN"                    -> error() (aucune icône n'a de sens)
     *  - un KitId réel (AXE, UHC…)  -> withIcon() avec l'icône du mode
     *  - toute autre valeur (ex: "DETECTION") -> success() (pas d'icône dédiée)
     */
    public static MutableText forMode(ModConfig config, String modeName, String message) {
        if (modeName == null) {
            return success(message);
        }
        if (modeName.equalsIgnoreCase("AUCUN")) {
            return error(message);
        }
        try {
            KitId kitId = KitId.valueOf(modeName.toUpperCase(Locale.ROOT));
            return forMode(config, kitId, message);
        } catch (IllegalArgumentException e) {
            return success(message);
        }
    }

    /** Variante directement par KitId (ex: kit déjà détecté, déjà résolu). */
    public static MutableText forMode(ModConfig config, KitId kitId, String message) {
        if (kitId == null || kitId == KitId.UNKNOWN) {
            return success(message);
        }
        return withIcon(KitIcons.iconText(config, kitId), message);
    }

    private static MutableText build(MutableText icon, TextColor accent, String message) {
        // IMPORTANT : la racine doit être neutre (sans police custom). Un style
        // non précisé sur un enfant hérite de son parent dans l'arbre du texte,
        // donc si l'icône (qui force la police "swifttagger:icons") était la
        // racine, le « » et tout le message hériteraient de cette police —
        // qui ne contient QUE les sprites d'icônes, d'où des carrés vides sur
        // chaque lettre normale. On place donc l'icône comme simple ENFANT
        // d'une racine vide, au même titre que le « » et le message.
        //
        // Le « » est TOUJOURS bleu (ModConfig#guillemetColorHex, #06b7fe par
        // défaut), quelle que soit la couleur de l'icône (verte/rouge/bleue) :
        // seule l'icône change de couleur selon le type de message.
        int guillemetRgb = com.example.swifttagger.config.TierColors.parseHex(
                com.example.swifttagger.config.ConfigManager.get().guillemetColorHex, 0x06b7fe);
        TextColor guillemetColor = TextColor.fromRgb(guillemetRgb);
        return Text.literal("")
                .append(icon)
                .append(Text.literal("» ").setStyle(Style.EMPTY.withColor(guillemetColor)))
                .append(Text.literal(message == null ? "" : message)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.WHITE))));
    }
}
