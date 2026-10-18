package com.example.swifttagger.kit;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public final class InventoryScanner {

    private InventoryScanner() {
    }

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

        if (isShulkerBox(stack) && shulkerContainsSpeedOrStrength(stack)) {
            out.add("shulker_speed_or_strength_potions");
        }
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return ItemKeyMapper.keysFor(stack).contains("shulker_box");
    }

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

            return false;
        }
        return false;
    }
}
