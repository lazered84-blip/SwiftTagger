package com.example.swifttagger.config;

import com.example.swifttagger.kit.KitId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Charge/sauvegarde config/swifttagger.json. C'est la SEULE classe qui touche
 * au disque pour la config (section 11 : "vraie configuration accessible
 * facilement").
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("swifttagger.json");
    /** Ancien nom de fichier (mod "OurTiers") : migré automatiquement une seule fois si trouvé. */
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("ourtiers.json");

    private static ModConfig config;

    public static ModConfig get() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void load() {
        Path sourcePath = Files.exists(CONFIG_PATH) ? CONFIG_PATH : legacyPathIfPresent();

        if (sourcePath != null) {
            try {
                String json = Files.readString(sourcePath);
                ModConfig parsed = GSON.fromJson(json, ModConfig.class);
                // Fichier vide/corrompu -> fromJson peut renvoyer null sans lever d'exception.
                config = parsed != null ? parsed : new ModConfig();
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                System.err.println("[SwiftTagger] Impossible de lire la config, valeurs par défaut utilisées : " + e.getMessage());
                config = new ModConfig();
            }
        } else {
            config = new ModConfig();
        }
        migrateIconDefaults();
        migrateSeparatorDefault();
        // Toujours réécrire : comble les nouveaux champs manquants d'un ancien
        // fichier et crée le fichier (sous son nouveau nom) au tout premier lancement.
        save();
    }

    /** Renvoie config/ourtiers.json s'il existe encore et que swifttagger.json n'a pas déjà été créé. */
    private static Path legacyPathIfPresent() {
        return Files.exists(LEGACY_CONFIG_PATH) ? LEGACY_CONFIG_PATH : null;
    }

    public static void save() {
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            System.err.println("[SwiftTagger] Impossible de sauvegarder la config : " + e.getMessage());
        }
    }


    /**
     * Migre les anciennes icônes PUA vers les emojis demandés par l'utilisateur.
     * Une icône personnalisée est conservée.
     */
    private static void migrateIconDefaults() {
        if (config.kitIcons == null) {
            config.kitIcons = new java.util.LinkedHashMap<>();
        }
        for (KitId kitId : KitId.values()) {
            if (kitId == KitId.UNKNOWN) continue;
            String value = config.kitIcons.get(kitId.name());
            if (value == null || value.isEmpty() || isLegacyIcon(value) || isOldDefaultEmoji(kitId, value)) {
                config.kitIcons.put(kitId.name(), com.example.swifttagger.kit.KitIcons.defaultOf(kitId));
            }
        }
    }

    private static boolean isLegacyIcon(String value) {
        if (value == null || value.isEmpty()) return false;
        int cp = value.codePointAt(0);
        return cp >= 0xF801 && cp <= 0xF808;
    }


    private static boolean isOldDefaultEmoji(KitId kitId, String value) {
        return switch (kitId) {
            case NETHERITE_POT -> "☠️".equals(value);
            case DIAMOND_POT -> "⚗️".equals(value);
            case SWORD -> "🗡️".equals(value);
            case UHC -> "❤️".equals(value);
            default -> false;
        };
    }
    /**
     * Normalise le séparateur par défaut sur "|" (barre ASCII), afin que la
     * barre ajoutée par le mod ait exactement le même glyphe que la barre
     * utilisée dans le pseudo/préfixe serveur. Une valeur personnalisée
     * différente est conservée.
     */
    private static void migrateSeparatorDefault() {
        if (config.separator == null || config.separator.isBlank() || "｜".equals(config.separator)) {
            config.separator = "|";
        }
    }

    /** Convertit ModConfig#kitPriorityOrder (noms) en List<KitId>, en ignorant les entrées invalides. */
    public static List<KitId> kitPriorityAsIds() {
        List<KitId> result = new ArrayList<>();
        if (config == null || config.kitPriorityOrder == null) {
            return result;
        }
        for (String name : config.kitPriorityOrder) {
            try {
                result.add(KitId.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Nom de kit inconnu (config éditée à la main) : on l'ignore
                // plutôt que de faire planter le chargement (section 16).
            }
        }
        return result;
    }
}
