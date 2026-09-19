package com.example.swifttagger.kit;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Construit l'ensemble des clés "présentes" pour un joueur.
 *
 * LIMITE TECHNIQUE IMPORTANTE (protocole Minecraft) : le contenu complet de
 * l'inventaire (les 36 slots du sac) d'un AUTRE joueur n'est JAMAIS
 * synchronisé au client par le serveur vanilla. Un client ne voit que :
 *   - les 4 pièces d'armure de l'autre joueur,
 *   - son item en main principale et main secondaire.
 * Il est donc IMPOSSIBLE de détecter un kit à 100% de fiabilité sur un
 * joueur distant avec ces seules informations (ex: on ne peut pas savoir
 * s'il a un seau d'eau ET un seau de lave dans son sac s'il ne tient que
 * l'un des deux en main). La détection sur autrui reste donc un
 * "meilleur effort" basé sur l'équipement visible, ce qui est cohérent avec
 * le système de score par proximité demandé par la spec (section 2) : moins
 * d'items visibles = score plus bas, éventuellement sous le seuil = Unknown.
 *
 * Pour le JOUEUR LOCAL, on scanne le sac complet (36 slots) + armure +
 * main secondaire, ce qui donne une détection fiable pour l'auto-détection
 * (touche dédiée / mise à jour automatique sur changement d'inventaire).
 */
public final class InventoryScanner {

    private InventoryScanner() {
    }

    /** Joueur local : inventaire complet + armure + offhand. */
    public static Set<String> scanFull(PlayerEntity player) {
        Set<String> keys = new HashSet<>();
        if (player == null) {
            return keys;
        }

        net.minecraft.inventory.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            collect(inv.getStack(i), keys);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                collect(player.getEquippedStack(slot), keys);
            }
        }

        return keys;
    }

    /** Joueur distant : uniquement ce que le protocole synchronise (armure + mains). */
    public static Set<String> scanVisibleEquipment(PlayerEntity player) {
        Set<String> keys = new HashSet<>();
        if (player == null) {
            return keys;
        }

        collect(player.getEquippedStack(EquipmentSlot.HEAD), keys);
        collect(player.getEquippedStack(EquipmentSlot.CHEST), keys);
        collect(player.getEquippedStack(EquipmentSlot.LEGS), keys);
        collect(player.getEquippedStack(EquipmentSlot.FEET), keys);
        collect(player.getMainHandStack(), keys);
        collect(player.getOffHandStack(), keys);

        return keys;
    }

    private static void collect(ItemStack stack, Set<String> out) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        out.addAll(ItemKeyMapper.keysFor(stack));

        // Règle Shulker (section 4, kit Mace) : si le stack est une shulker
        // box, on regarde son contenu pour la présence de Speed/Strength,
        // sans exiger un nombre exact de potions.
        if (isShulkerBox(stack) && shulkerContainsSpeedOrStrength(stack)) {
            out.add("shulker_speed_or_strength_potions");
        }
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return ItemKeyMapper.keysFor(stack).contains("shulker_box");
    }

    /**
     * Lit le contenu NBT/component d'une shulker box pour y chercher des
     * potions Speed ou Strength (peu importe le niveau, section 4).
     *
     * Best-effort : utilise le component "container" (1.20.5+) s'il existe.
     * Si l'API a changé entre versions, cette méthode échoue silencieusement
     * (ne doit jamais crash le scan global, cf. section 16 "Robustesse").
     */
    private static boolean shulkerContainsSpeedOrStrength(ItemStack shulkerStack) {
        try {
            var containerComponent = shulkerStack.get(DataComponentTypes.CONTAINER);
            if (containerComponent == null) {
                return false;
            }
            for (ItemStack inner : containerComponent.stream().toList()) {
                Set<String> innerKeys = ItemKeyMapper.keysFor(inner);
                if (innerKeys.contains("potion:speed") || innerKeys.contains("potion:strength")) {
                    return true;
                }
            }
        } catch (Throwable t) {
            // Ne jamais faire planter la détection pour un contenu de
            // shulker illisible (cf. robustesse, section 16).
            return false;
        }
        return false;
    }
}
