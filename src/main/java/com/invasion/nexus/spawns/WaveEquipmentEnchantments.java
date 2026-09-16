package com.invasion.nexus.spawns;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

final class WaveEquipmentEnchantments {
    private WaveEquipmentEnchantments() {
    }

    static void enchantEquipment(Mob mob, int wave, RandomSource random) {
        List<Enchantment> enchantments = BuiltInRegistries.ENCHANTMENT.stream().toList();
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
            List<Enchantment> enchantments) {
        if (stack.isEmpty() || wave <= 0) return;
        int chance = Math.min(wave, 100);
        while (true) {
            var current = EnchantmentHelper.getEnchantments(stack);
            List<Enchantment> candidates = new ArrayList<>();
            for (Enchantment enchantment : enchantments) {
                int level = current.getOrDefault(enchantment, 0);
                if (!enchantment.canEnchant(stack)
                        || level >= enchantment.getMaxLevel()) continue;
                boolean compatible = current.keySet().stream().allMatch(existing ->
                        existing.equals(enchantment) || existing.isCompatibleWith(enchantment));
                if (compatible) candidates.add(enchantment);
            }
            // Check saturation before rolling, including at 100% chance.
            if (candidates.isEmpty() || random.nextInt(100) >= chance) return;
            Enchantment selected = candidates.get(random.nextInt(candidates.size()));
            int level = current.getOrDefault(selected, 0);
            current.put(selected, level == 0 ? selected.getMinLevel() : level + 1);
            // Replace NBT levels; ItemStack.enchant would append duplicate entries on this version.
            EnchantmentHelper.setEnchantments(current, stack);
        }
    }
}
