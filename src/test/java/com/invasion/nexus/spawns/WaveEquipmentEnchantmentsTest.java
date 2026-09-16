package com.invasion.nexus.spawns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeEach;
import net.minecraft.world.item.enchantment.Enchantment;
import org.junit.jupiter.api.Test;

class WaveEquipmentEnchantmentsTest {
    private final ItemStack sword = mock(ItemStack.class);
    private final ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    private final RandomSource random = mock(RandomSource.class);

    @BeforeEach
    void setUp() {
        when(sword.getTagEnchantments()).thenAnswer(invocation -> enchantments.toImmutable());
        when(sword.supportsEnchantment(any())).thenReturn(true);
        when(sword.isEnchanted()).thenAnswer(invocation -> !enchantments.toImmutable().isEmpty());
        doAnswer(invocation -> {
            enchantments.upgrade(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(sword).enchant(any(), anyInt());
    }

    private Holder<Enchantment> enchantment(int maxLevel, HolderSet<Enchantment> exclusive) {
        return Holder.direct(new Enchantment(Component.literal("Test"),
                Enchantment.definition(HolderSet.empty(),
                        1, maxLevel, Enchantment.constantCost(1), Enchantment.constantCost(1),
                        1, EquipmentSlotGroup.MAINHAND), exclusive, DataComponentMap.EMPTY));
    }

    @Test
    void successStartsAtBaseLevelThenFailureStops() {
        var enchantment = enchantment(3, HolderSet.empty());
        when(random.nextInt(100)).thenReturn(24, 25);
        WaveEquipmentEnchantments.enchant(sword, 25, random, List.of(enchantment));
        assertEquals(1, sword.getTagEnchantments().getLevel(enchantment));
        verify(random, times(2)).nextInt(100);
    }

    @Test
    void failedFirstRollLeavesItemUnenchanted() {
        when(random.nextInt(100)).thenReturn(25);
        WaveEquipmentEnchantments.enchant(sword, 25, random,
                List.of(enchantment(3, HolderSet.empty())));
        assertFalse(sword.isEnchanted());
        verify(random, times(1)).nextInt(100);
    }

    @Test
    void fullChanceAddsAndUpgradesUntilSaturatedWithoutExtraRoll() {
        var first = enchantment(3, HolderSet.empty());
        var second = enchantment(2, HolderSet.empty());
        WaveEquipmentEnchantments.enchant(sword, 150, random, List.of(first, second));
        assertEquals(3, sword.getTagEnchantments().getLevel(first));
        assertEquals(2, sword.getTagEnchantments().getLevel(second));
        verify(random, times(5)).nextInt(100);
    }

    @Test
    void incompatibleEnchantmentsAreNeverAdded() {
        var first = enchantment(2, HolderSet.empty());
        var incompatible = enchantment(3, HolderSet.direct(first));
        WaveEquipmentEnchantments.enchant(sword, 100, random, List.of(first, incompatible));
        assertEquals(2, sword.getTagEnchantments().getLevel(first));
        assertEquals(0, sword.getTagEnchantments().getLevel(incompatible));
        verify(random, times(2)).nextInt(100);
    }

    @Test
    void existingEnchantmentIsUpgradedWithoutResetting() {
        var enchantment = enchantment(3, HolderSet.empty());
        sword.enchant(enchantment, 2);
        WaveEquipmentEnchantments.enchant(sword, 100, random, List.of(enchantment));
        assertEquals(3, sword.getTagEnchantments().getLevel(enchantment));
        verify(random, times(1)).nextInt(100);
        clearInvocations(random);
        WaveEquipmentEnchantments.enchant(sword, 100, random, List.of(enchantment));
        verifyNoInteractions(random);
    }

    @Test
    void emptyUnsupportedAndZeroWaveItemsDoNotRoll() {
        var enchantments = List.of(enchantment(3, HolderSet.empty()));
        WaveEquipmentEnchantments.enchant(ItemStack.EMPTY, 100, random, enchantments);
        ItemStack unsupported = mock(ItemStack.class);
        when(unsupported.getTagEnchantments()).thenReturn(ItemEnchantments.EMPTY);
        WaveEquipmentEnchantments.enchant(unsupported, 100, random, enchantments);
        WaveEquipmentEnchantments.enchant(sword, 0, random, enchantments);
        verifyNoInteractions(random);
    }
}
