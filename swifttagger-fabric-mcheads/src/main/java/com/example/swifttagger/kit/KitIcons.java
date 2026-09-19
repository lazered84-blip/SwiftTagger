package com.example.swifttagger.kit;

import com.example.swifttagger.config.ModConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Icone associee a chaque mode (section 6 de la spec).
 *
 * IMPORTANT (correctif) : les emojis Unicode "classiques" (axe/marteau/crane/
 * fiole/bouclier/dague/coeur/etoile) ne s'affichent PAS de facon fiable dans
 * Minecraft : la police par defaut du client (unifont) ne couvre pas tous
 * ces points de code, ce qui donne le carre vide ("tofu") observe en jeu,
 * alors que le texte brut est pourtant correct cote code.
 *
 * Pour que les icones s'affichent VRAIMENT chez tout le monde, le mod
 * embarque sa propre police bitmap (voir
 * assets/swifttagger/font/icons.json + textures/font/icon_*.png) : chaque
 * mode par defaut est mappe sur un caractere de la zone Private Use Area
 * (les points de code Unicode des emojis demandés) qui pointe vers un petit sprite dessine pour ce mode,
 * garanti present dans le jar du mod donc toujours rendu correctement, quel
 * que soit le pack de ressources ou le systeme du joueur.
 *
 * Un joueur peut toujours remplacer une icone dans config/swifttagger.json
 * par n'importe quel texte/emoji classique : dans ce cas on ne force plus la
 * police custom (voir {@link #iconText}), pour ne pas afficher un glyphe du
 * mod a la place du caractere choisi.
 */
public final class KitIcons {
    private KitIcons() {
    }

    /** Police embarquee par le mod, cf. assets/swifttagger/font/icons.json. */
    public static final Identifier ICON_FONT = Identifier.of("swifttagger", "icons");
    public static final Identifier MCTIERS_FONT = Identifier.of("swifttagger", "mctiers");
    public static final Identifier PVPTIERS_FONT = Identifier.of("swifttagger", "pvptiers");

    /**
     * Depuis Minecraft 1.21.11, Style#withFont attend un StyleSpriteSource
     * (et non plus un simple Identifier) : on enveloppe donc une seule fois
     * ICON_FONT dans un StyleSpriteSource.Font, reutilise partout ou
     * l'icone par defaut du mod doit etre appliquee.
     */
    private static final StyleSpriteSource ICON_FONT_SOURCE = new StyleSpriteSource.Font(ICON_FONT);
    private static final StyleSpriteSource MCTIERS_FONT_SOURCE = new StyleSpriteSource.Font(MCTIERS_FONT);
    private static final StyleSpriteSource PVPTIERS_FONT_SOURCE = new StyleSpriteSource.Font(PVPTIERS_FONT);

    private static final Map<KitId, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put(KitId.AXE, "🪓");           // sprite icon_axe.png
        DEFAULTS.put(KitId.MACE, "🔨");           // sprite icon_mace.png
        DEFAULTS.put(KitId.NETHERITE_POT, "☠");  // sprite icon_netherite_pot.png
        DEFAULTS.put(KitId.DIAMOND_POT, "⚗");    // sprite icon_diamond_pot.png
        DEFAULTS.put(KitId.SMP, "⛨");            // sprite icon_smp.png
        DEFAULTS.put(KitId.SWORD, "🗡");          // sprite icon_sword.png
        DEFAULTS.put(KitId.UHC, "❤");            // sprite icon_uhc.png
        DEFAULTS.put(KitId.VANILLA, "✦");        // sprite icon_vanilla.png
        DEFAULTS.put(KitId.UNKNOWN, "");
    }

    /**
     * Couleurs fixes des icônes pour le pack VANILLA (caractère Unicode natif,
     * sans sprite) : demandées explicitement pour que chaque icône garde
     * TOUJOURS cette couleur exacte dans la barre d'action (touche, /swift kit,
     * détection auto), indépendamment de la couleur du tier. Écrasées par la
     * couleur du tier ailleurs (nametag/TAB/chat) uniquement si
     * ModConfig#vanillaIconTierColor est activé, cf. TierTextRenderer#tintIcon.
     */
    private static final Map<KitId, Integer> VANILLA_COLORS = new LinkedHashMap<>();
    static {
        VANILLA_COLORS.put(KitId.VANILLA, 0xF953F9);
        VANILLA_COLORS.put(KitId.UHC, 0xF55252);
        VANILLA_COLORS.put(KitId.SWORD, 0xA0F7EA);
        VANILLA_COLORS.put(KitId.NETHERITE_POT, 0x7B483E);
        VANILLA_COLORS.put(KitId.SMP, 0xE6C743);
        VANILLA_COLORS.put(KitId.DIAMOND_POT, 0xF90000);
        VANILLA_COLORS.put(KitId.AXE, 0x53F953);
        VANILLA_COLORS.put(KitId.MACE, 0xD3D3D3);
    }

    /** Table par defaut, cle = nom de KitId (ex: "SWORD"), telle que copiee dans ModConfig#kitIcons. */
    public static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Map.Entry<KitId, String> entry : DEFAULTS.entrySet()) {
            m.put(entry.getKey().name(), entry.getValue());
        }
        return m;
    }

    /** Icone par defaut (code en dur), utilisee si la config n'a pas (encore) l'entree. */
    public static String defaultOf(KitId kitId) {
        if (kitId == null) return "";
        return DEFAULTS.getOrDefault(kitId, "");
    }

    /** Icone configuree pour ce kit (chaine brute), retombe sur la valeur par defaut si absente/vide. */
    public static String of(Map<String, String> configuredIcons, KitId kitId) {
        if (kitId == null) return "";
        if (configuredIcons != null) {
            String configured = configuredIcons.get(kitId.name());
            if (configured != null && !configured.isEmpty()) {
                return configured;
            }
        }
        return defaultOf(kitId);
    }

    /** Variante par nom de mode en minuscules (ex: "sword"), utilisee quand on n'a que la cle JSON de l'API. */
    public static String of(Map<String, String> configuredIcons, String modeKey) {
        return of(configuredIcons, KitId.fromModeKey(modeKey));
    }

    /**
     * Variante "prete a afficher" : renvoie l'icone sous forme de Text deja
     * stylee, selon ModConfig#iconStyle :
     *  - TEXTURE (par defaut) : si l'icone est encore celle par defaut du
     *    mod, on force la police embarquee {@link #ICON_FONT} (sprite
     *    PNG garanti present dans le jar) avec une teinte blanche neutre
     *    (0xFFFFFF) pour que le sprite garde ses couleurs d'origine.
     *  - CHARACTER : le vrai caractere emoji Unicode est utilise tel quel,
     *    sans forcer de police, exactement comme un prefixe LuckPerms. Peut
     *    s'afficher en carre vide ("tofu") si la police du client ne couvre
     *    pas ce point de code, contrairement a TEXTURE.
     * Si le joueur a personnalise l'icone dans sa config avec un texte ou un
     * emoji classique (different de la valeur par defaut), on laisse dans
     * tous les cas la police par defaut du client faire son travail.
     */
    public static MutableText iconText(ModConfig config, KitId kitId) {
        Map<String, String> configuredIcons = config != null ? config.kitIcons : null;
        String icon = of(configuredIcons, kitId);
        if (icon.isEmpty()) return Text.literal("");

        String def = defaultOf(kitId);
        boolean isDefaultIcon = !def.isEmpty() && icon.equals(def);
        if (!isDefaultIcon || config == null) return Text.literal(icon);

        String style = config.iconStyle == null ? "PVPHQ" : config.iconStyle.toUpperCase(java.util.Locale.ROOT);
        return switch (style) {
            case "MCTIERS" -> Text.literal(icon).setStyle(Style.EMPTY.withFont(MCTIERS_FONT_SOURCE));
            case "PVPTIERS" -> Text.literal(icon).setStyle(Style.EMPTY.withFont(PVPTIERS_FONT_SOURCE));
            case "VANILLA", "CHARACTER" -> {
                Integer fixedColor = VANILLA_COLORS.get(kitId);
                yield fixedColor != null
                        ? Text.literal(icon).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(fixedColor)))
                        : Text.literal(icon);
            }
            default -> Text.literal(icon).setStyle(Style.EMPTY.withFont(ICON_FONT_SOURCE));
        };
    }

    /** Variante par nom de mode en minuscules (ex: "sword"). */
    public static MutableText iconText(ModConfig config, String modeKey) {
        return iconText(config, KitId.fromModeKey(modeKey));
    }
}
