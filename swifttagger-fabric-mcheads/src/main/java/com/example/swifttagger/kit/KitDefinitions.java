package com.example.swifttagger.kit;

import java.util.List;

import static com.example.swifttagger.kit.ItemWeight.optional;
import static com.example.swifttagger.kit.ItemWeight.required;

/**
 * Table de définition des 8 kits, telle que décrite dans la spec
 * ("Mod Minecraft — Tier Tagger / Kit Detector").
 *
 * IMPORTANT sur la pondération (voir KitDetector pour la justification) :
 * les pièces d'armure diamant/netherite sont volontairement pondérées
 * FAIBLE, car elles sont communes à presque tous les kits (Sword, Axe, UHC,
 * Diamond Pot partagent tous "armure diamant"). Ce qui distingue vraiment
 * un kit, ce sont ses items d'arme/utilitaire spécifiques (potions, seau,
 * pioche, mace, élytre, crystal...), qui reçoivent un poids ÉLEVÉ.
 * C'est ce qui permet au détecteur de choisir "Diamond Pot" plutôt que
 * "Sword" quand un joueur a une armure diamant + épée + potions (cf. tests
 * 3, 4, 5, 7, 9 de la section 18 de la spec).
 *
 * Alias : gérés uniquement via {@code aliases}, jamais comme kits séparés
 * (section 15 de la spec).
 */
public final class KitDefinitions {

    private KitDefinitions() {
    }

    // Poids "élevé / moyen / faible" repris de la section 5 de la spec.
    private static final int HIGH = ItemWeight.Weight.HIGH;
    private static final int MEDIUM = ItemWeight.Weight.MEDIUM;
    private static final int LOW = ItemWeight.Weight.LOW;

    public static final KitDefinition SWORD = new KitDefinition(
            KitId.SWORD,
            "Sword",
            List.of(),
            List.of(
                    required("diamond_helmet", LOW),
                    required("diamond_chestplate", LOW),
                    required("diamond_leggings", LOW),
                    required("diamond_boots", LOW),
                    required("diamond_sword", MEDIUM)
            ),
            List.of(
                    optional("wooden_sword", LOW),
                    optional("stone_sword", LOW),
                    optional("golden_sword", LOW)
            ),
            7
    );

    public static final KitDefinition AXE = new KitDefinition(
            KitId.AXE,
            "Axe",
            List.of(),
            List.of(
                    required("diamond_helmet", LOW),
                    required("diamond_chestplate", LOW),
                    required("diamond_leggings", LOW),
                    required("diamond_boots", LOW),
                    required("shield", MEDIUM),
                    required("diamond_sword", MEDIUM),
                    required("diamond_axe", HIGH),
                    required("bow", HIGH),
                    required("crossbow", HIGH),
                    required("arrow", LOW)
            ),
            List.of(),
            4
    );

    public static final KitDefinition SMP = new KitDefinition(
            KitId.SMP,
            "SMP",
            List.of(),
            List.of(
                    required("netherite_helmet", LOW),
                    required("netherite_chestplate", LOW),
                    required("netherite_leggings", LOW),
                    required("netherite_boots", LOW),
                    required("netherite_sword", MEDIUM),
                    required("netherite_axe", HIGH),
                    required("shield", MEDIUM),
                    required("golden_apple", LOW),
                    required("ender_pearl", HIGH),
                    required("experience_bottle", LOW),
                    required("potion:fire_resistance", HIGH),
                    required("potion:speed", LOW),
                    required("potion:strength", LOW),
                    required("totem", MEDIUM)
            ),
            List.of(),
            2
    );

    public static final KitDefinition NETHERITE_POT = new KitDefinition(
            KitId.NETHERITE_POT,
            "Netherite Pot",
            List.of("Neth Pot", "Netherite Potion"),
            List.of(
                    required("netherite_helmet", LOW),
                    required("netherite_chestplate", LOW),
                    required("netherite_leggings", LOW),
                    required("netherite_boots", LOW),
                    required("netherite_sword", MEDIUM),
                    required("experience_bottle", LOW),
                    required("golden_apple", LOW),
                    required("potion:speed", LOW),
                    required("potion:strength", LOW),
                    required("potion:healing", HIGH),
                    required("totem", MEDIUM)
            ),
            List.of(),
            3
    );

    public static final KitDefinition DIAMOND_POT = new KitDefinition(
            KitId.DIAMOND_POT,
            "Diamond Pot",
            List.of("Dia Pot", "Pot", "Diamond Potion"),
            List.of(
                    required("diamond_helmet", LOW),
                    required("diamond_chestplate", LOW),
                    required("diamond_leggings", LOW),
                    required("diamond_boots", LOW),
                    required("diamond_sword", MEDIUM),
                    required("steak", HIGH),
                    required("potion:speed", LOW),
                    required("potion:strength", LOW),
                    required("potion:regeneration", HIGH),
                    required("potion:healing", HIGH)
            ),
            List.of(),
            6
    );

    public static final KitDefinition VANILLA = new KitDefinition(
            KitId.VANILLA,
            "Vanilla",
            List.of(),
            List.of(
                    required("end_crystal", HIGH),
                    required("respawn_anchor", HIGH),
                    required("totem", MEDIUM),
                    required("crossbow", MEDIUM),
                    required("shield", MEDIUM),
                    required("ender_pearl", MEDIUM),
                    required("golden_apple", MEDIUM),
                    required("netherite_armor_any", HIGH),
                    required("obsidian", MEDIUM),
                    required("glowstone", MEDIUM)
            ),
            List.of(),
            8
    );

    public static final KitDefinition UHC = new KitDefinition(
            KitId.UHC,
            "UHC",
            List.of(),
            List.of(
                    required("diamond_helmet", LOW),
                    required("diamond_chestplate", LOW),
                    required("diamond_leggings", LOW),
                    required("diamond_boots", LOW),
                    required("shield", MEDIUM),
                    required("diamond_sword", MEDIUM),
                    required("diamond_axe", HIGH),
                    required("bow", HIGH),
                    required("crossbow", HIGH),
                    required("arrow", LOW),
                    required("water_bucket", HIGH),
                    required("lava_bucket", HIGH),
                    required("cobweb", HIGH),
                    required("blocks_generic", LOW),
                    required("diamond_pickaxe", HIGH),
                    required("golden_apple", LOW)
            ),
            List.of(),
            5
    );

    public static final KitDefinition MACE = new KitDefinition(
            KitId.MACE,
            "Mace",
            List.of(),
            List.of(
                    required("netherite_helmet", LOW),
                    required("netherite_chestplate", LOW),
                    required("netherite_leggings", LOW),
                    required("netherite_boots", LOW),
                    required("shield", MEDIUM),
                    required("netherite_sword", MEDIUM),
                    required("netherite_axe", MEDIUM),
                    required("mace", HIGH),
                    required("elytra", HIGH),
                    required("golden_apple", LOW),
                    required("wind_charge", MEDIUM),
                    required("ender_pearl", LOW),
                    required("potion:speed", LOW),
                    required("potion:strength", LOW),
                    required("totem", LOW)
            ),
            List.of(
                    optional("shulker_box", LOW),
                    // Cf. section 4 "Règle Shulker" : une shulker contenant des
                    // Speed II / Strength II doit augmenter le score Mace sans
                    // exiger un nombre exact de potions.
                    optional("shulker_speed_or_strength_potions", MEDIUM)
            ),
            1
    );

    public static final List<KitDefinition> ALL = List.of(
            MACE, SMP, NETHERITE_POT, AXE, UHC, DIAMOND_POT, SWORD, VANILLA
    );

    public static KitDefinition byId(KitId id) {
        for (KitDefinition def : ALL) {
            if (def.id() == id) {
                return def;
            }
        }
        return null;
    }
}
