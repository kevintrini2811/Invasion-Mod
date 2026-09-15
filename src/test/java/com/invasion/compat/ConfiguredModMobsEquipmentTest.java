package com.invasion.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.neoforge.common.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfiguredModMobsEquipmentTest {
    private Mob mob;
    private ItemStack shield;
    private ItemStack weapon;

    @BeforeEach
    void setUp() {
        mob = mock(Mob.class);
        shield = mock(ItemStack.class);
        weapon = mock(ItemStack.class);
        when(mob.getEquipmentSlotForItem(shield)).thenReturn(EquipmentSlot.OFFHAND);
        when(mob.getMainHandItem()).thenReturn(weapon);
        when(mob.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        when(weapon.is(Tags.Items.MELEE_WEAPON_TOOLS)).thenReturn(true);
    }

    @Test
    void meleeMobAcceptsShieldItem() {
        when(shield.getItem()).thenReturn(mock(ShieldItem.class));
        assertTrue(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }

    @Test
    void meleeMobAcceptsTaggedModShield() {
        when(shield.is(Tags.Items.TOOLS_SHIELD)).thenReturn(true);
        assertTrue(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }

    @Test
    void shieldRequiresWeaponsPermission() {
        when(shield.is(Tags.Items.TOOLS_SHIELD)).thenReturn(true);
        assertFalse(ConfiguredModMobs.isAllowedEquipment(mob, shield, true, false));
    }

    @Test
    void shieldDoesNotReplaceOccupiedOffhand() {
        when(shield.is(Tags.Items.TOOLS_SHIELD)).thenReturn(true);
        when(mob.getOffhandItem()).thenReturn(mock(ItemStack.class));
        assertFalse(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }

    @Test
    void rangedMobDoesNotPickUpShield() {
        when(shield.is(Tags.Items.TOOLS_SHIELD)).thenReturn(true);
        when(weapon.is(Tags.Items.MELEE_WEAPON_TOOLS)).thenReturn(false);
        when(weapon.is(Tags.Items.RANGED_WEAPON_TOOLS)).thenReturn(true);
        assertFalse(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }
}
