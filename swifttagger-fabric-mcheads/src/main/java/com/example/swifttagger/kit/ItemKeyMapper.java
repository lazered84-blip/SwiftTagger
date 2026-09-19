package com.example.swifttagger.kit;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Traduit un ItemStack en une (ou plusieurs) clé(s) canonique(s) utilisée(s)
 * par {@link KitDetector}. C'est ICI, et seulement ici, que les règles de la
 * section 3 de la spec sont appliquées :
 *  - enchantements : jamais lus ;
 *  - quantité : jamais lue (on ne regarde que "présent ou pas" dans
 *    InventoryScanner, pas le nombre d'items) ;
 *  - durabilité : jamais lue ;
 *  - potions : seul le type d'effet compte, pas le niveau ni la durée.
 */
public final class ItemKeyMapper {

    private ItemKeyMapper() {
    }

    /**
     * Renvoie l'ensemble des clés apportées par ce stack (généralement une
     * seule, parfois plusieurs pour les "clés génériques" comme
     * netherite_armor_any / blocks_generic).
     */
    public static Set<String> keysFor(ItemStack stack) {
        Set<String> keys = new HashSet<>();
        if (stack == null || stack.isEmpty()) {
            return keys;
        }

        Item item = stack.getItem();

        // ------------------------------------------------------------
        // Potions : uniquement le type d'effet (speed/strength/heal/...),
        // jamais le niveau (II, III...) ni la durée.
        // ------------------------------------------------------------
        if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            String effect = potionEffectKey(stack);
            if (effect != null) {
                keys.add("potion:" + effect);
            }
            return keys;
        }

        String direct = directKey(item);
        if (direct != null) {
            keys.add(direct);
        }

        // Clés génériques additionnelles (contribuent au score sans exiger
        // une pièce précise).
        if (isNetheriteArmorPiece(item)) {
            keys.add("netherite_armor_any");
        }
        if (item == Items.COBBLESTONE || item == Items.OAK_PLANKS || item == Items.SPRUCE_PLANKS
                || item == Items.BIRCH_PLANKS || item == Items.JUNGLE_PLANKS || item == Items.ACACIA_PLANKS
                || item == Items.DARK_OAK_PLANKS || item == Items.MANGROVE_PLANKS || item == Items.CHERRY_PLANKS
                || item == Items.CRIMSON_PLANKS || item == Items.WARPED_PLANKS || item == Items.BAMBOO_PLANKS) {
            keys.add("blocks_generic");
        }

        return keys;
    }

    private static boolean isNetheriteArmorPiece(Item item) {
        return item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
                || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS;
    }

    private static String directKey(Item item) {
        if (item == Items.DIAMOND_HELMET) return "diamond_helmet";
        if (item == Items.DIAMOND_CHESTPLATE) return "diamond_chestplate";
        if (item == Items.DIAMOND_LEGGINGS) return "diamond_leggings";
        if (item == Items.DIAMOND_BOOTS) return "diamond_boots";
        if (item == Items.DIAMOND_SWORD) return "diamond_sword";
        if (item == Items.DIAMOND_AXE) return "diamond_axe";
        if (item == Items.DIAMOND_PICKAXE) return "diamond_pickaxe";

        if (item == Items.NETHERITE_HELMET) return "netherite_helmet";
        if (item == Items.NETHERITE_CHESTPLATE) return "netherite_chestplate";
        if (item == Items.NETHERITE_LEGGINGS) return "netherite_leggings";
        if (item == Items.NETHERITE_BOOTS) return "netherite_boots";
        if (item == Items.NETHERITE_SWORD) return "netherite_sword";
        if (item == Items.NETHERITE_AXE) return "netherite_axe";

        if (item == Items.WOODEN_SWORD) return "wooden_sword";
        if (item == Items.STONE_SWORD) return "stone_sword";
        if (item == Items.GOLDEN_SWORD) return "golden_sword";

        if (item == Items.SHIELD) return "shield";
        if (item == Items.BOW) return "bow";
        if (item == Items.CROSSBOW) return "crossbow";
        if (item == Items.ARROW || item == Items.SPECTRAL_ARROW || item == Items.TIPPED_ARROW) return "arrow";
        if (item == Items.MACE) return "mace";
        if (item == Items.ELYTRA) return "elytra";

        if (item == Items.END_CRYSTAL) return "end_crystal";
        if (item == Items.RESPAWN_ANCHOR) return "respawn_anchor";
        if (item == Items.TOTEM_OF_UNDYING) return "totem";
        if (item == Items.ENDER_PEARL) return "ender_pearl";
        if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) return "golden_apple";
        if (item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN) return "obsidian";
        if (item == Items.GLOWSTONE) return "glowstone";
        if (item == Items.EXPERIENCE_BOTTLE) return "experience_bottle";
        if (item == Items.WIND_CHARGE) return "wind_charge";
        if (item == Items.COOKED_BEEF) return "steak";
        if (item == Items.WATER_BUCKET) return "water_bucket";
        if (item == Items.LAVA_BUCKET) return "lava_bucket";
        if (item == Items.COBWEB) return "cobweb";

        if (isShulkerBox(item)) return "shulker_box";

        return null;
    }

    private static boolean isShulkerBox(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return id != null && id.getPath().endsWith("shulker_box");
    }

    /**
     * Extrait uniquement le "type" d'effet d'une potion (speed, strength,
     * healing, regeneration, fire_resistance...), en ignorant amplifier et
     * durée. Renvoie null si le stack n'a pas de PotionContentsComponent
     * exploitable (potion "vide"/eau).
     */
    private static String potionEffectKey(ItemStack stack) {
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) {
            return null;
        }

        Optional<RegistryEntry<Potion>> potionEntry = contents.potion();
        if (potionEntry.isPresent()) {
            Identifier id = potionEntry.get().getKey().map(k -> k.getValue()).orElse(null);
            String mapped = mapPotionId(id);
            if (mapped != null) {
                return mapped;
            }
        }

        // Potions à effets custom (sans "potion" de base reconnue) : on
        // regarde le premier effet listé, encore une fois sans son niveau.
        if (!contents.customEffects().isEmpty()) {
            var effectInstance = contents.customEffects().get(0);
            Identifier effectId = Registries.STATUS_EFFECT.getId(effectInstance.getEffectType().value());
            return mapEffectId(effectId);
        }

        return null;
    }

    private static String mapPotionId(Identifier id) {
        if (id == null) return null;
        String path = id.getPath();
        if (path.contains("swiftness") || path.contains("speed")) return "speed";
        if (path.contains("strength")) return "strength";
        if (path.contains("healing")) return "healing";
        if (path.contains("regeneration")) return "regeneration";
        if (path.contains("fire_resistance")) return "fire_resistance";
        return null;
    }

    private static String mapEffectId(Identifier id) {
        if (id == null) return null;
        String path = id.getPath();
        if (path.equals("speed")) return "speed";
        if (path.equals("strength")) return "strength";
        if (path.equals("instant_health") || path.equals("healing")) return "healing";
        if (path.equals("regeneration")) return "regeneration";
        if (path.equals("fire_resistance")) return "fire_resistance";
        return null;
    }
}
