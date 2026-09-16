package com.invasion.nexus.spawns;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

final class WaveEquipmentEnchantments {
    private WaveEquipmentEnchantments() {
    }

    static void enchantEquipment(Mob mob, int wave, RandomSource random) {
        List<Holder<Enchantment>> enchantments = mob.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).holders()
                .map(holder -> (Holder<Enchantment>) holder).toList();
        // Riders finish spawning with their own equipment as well.
        mob.getSelfAndPassengers().forEach(entity -> {
            if (entity instanceof Mob equippedMob) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    enchant(equippedMob.getItemBySlot(slot), wave, random, enchantments);
                }
            }
        });
    }

    static void enchant(ItemStack stack, int wave, RandomSource random,
            List<Holder<Enchantment>> enchantments) {
        if (stack.isEmpty() || wave <= 0) return;
        int chance = Math.min(wave, 100);
        while (true) {
            var current = stack.getTagEnchantments();
            List<Holder<Enchantment>> candidates = new ArrayList<>();
            for (Holder<Enchantment> enchantment : enchantments) {
                int level = current.getLevel(enchantment);
                if (!stack.supportsEnchantment(enchantment)
                        || level >= enchantment.value().getMaxLevel()) continue;
                boolean compatible = current.keySet().stream().allMatch(existing ->
                        existing.equals(enchantment) || Enchantment.areCompatible(existing, enchantment));
                if (compatible) candidates.add(enchantment);
            }
            // Check saturation before rolling, including at 100% chance.
            if (candidates.isEmpty() || random.nextInt(100) >= chance) return;
            Holder<Enchantment> selected = candidates.get(random.nextInt(candidates.size()));
            int level = current.getLevel(selected);
            stack.enchant(selected, level == 0 ? selected.value().getMinLevel() : level + 1);
        }
    }
}
