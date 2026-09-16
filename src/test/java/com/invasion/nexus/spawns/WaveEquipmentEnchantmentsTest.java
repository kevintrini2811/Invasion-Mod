package com.invasion.nexus.spawns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WaveEquipmentEnchantmentsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private final ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
    private final RandomSource random = mock(RandomSource.class);

    @Test
    void successStartsAtBaseLevelThenFailureStops() {
        when(random.nextInt(100)).thenReturn(24, 25);
        WaveEquipmentEnchantments.enchant(sword, 25, random, List.of(Enchantments.SHARPNESS));
        assertEquals(1, EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SHARPNESS, sword));
        verify(random, times(2)).nextInt(100);
    }

    @Test
    void failedFirstRollLeavesItemUnenchanted() {
        when(random.nextInt(100)).thenReturn(25);
        WaveEquipmentEnchantments.enchant(sword, 25, random, List.of(Enchantments.SHARPNESS));
        assertFalse(sword.isEnchanted());
        verify(random, times(1)).nextInt(100);
    }

    @Test
    void fullChanceAddsAndUpgradesUntilSaturatedWithoutExtraRoll() {
        WaveEquipmentEnchantments.enchant(sword, 150, random,
                List.of(Enchantments.SHARPNESS, Enchantments.UNBREAKING));
        assertEquals(5, EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SHARPNESS, sword));
        assertEquals(3, EnchantmentHelper.getTagEnchantmentLevel(Enchantments.UNBREAKING, sword));
        assertEquals(2, sword.getEnchantmentTags().size());
        verify(random, times(8)).nextInt(100);
    }

    @Test
    void incompatibleEnchantmentsAreNeverAdded() {
        WaveEquipmentEnchantments.enchant(sword, 100, random,
                List.of(Enchantments.SHARPNESS, Enchantments.SMITE));
        assertEquals(5, EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SHARPNESS, sword));
        assertEquals(0, EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SMITE, sword));
        verify(random, times(5)).nextInt(100);
    }

    @Test
    void existingEnchantmentIsUpgradedWithoutResetting() {
        sword.enchant(Enchantments.SHARPNESS, 4);
        WaveEquipmentEnchantments.enchant(sword, 100, random, List.of(Enchantments.SHARPNESS));
        assertEquals(5, EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SHARPNESS, sword));
        assertEquals(1, sword.getEnchantmentTags().size());
        verify(random, times(1)).nextInt(100);
        clearInvocations(random);
        WaveEquipmentEnchantments.enchant(sword, 100, random, List.of(Enchantments.SHARPNESS));
        verifyNoInteractions(random);
    }

    @Test
    void emptyUnsupportedAndZeroWaveItemsDoNotRoll() {
        var enchantments = List.of(Enchantments.SHARPNESS);
        WaveEquipmentEnchantments.enchant(ItemStack.EMPTY, 100, random, enchantments);
        WaveEquipmentEnchantments.enchant(new ItemStack(Items.DIRT), 100, random, enchantments);
        WaveEquipmentEnchantments.enchant(sword, 0, random, enchantments);
        verifyNoInteractions(random);
    }
}
