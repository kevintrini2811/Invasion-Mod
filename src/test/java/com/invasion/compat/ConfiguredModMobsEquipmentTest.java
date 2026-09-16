package com.invasion.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.common.Tags;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfiguredModMobsEquipmentTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private Mob mob;
    private ItemStack shield;
    private ItemStack weapon;

    @BeforeEach
    void setUp() {
        mob = mock(Mob.class);
        shield = mock(ItemStack.class);
        weapon = mock(ItemStack.class);
        SwordItem sword = mock(SwordItem.class);
        when(mob.getMainHandItem()).thenReturn(weapon);
        when(mob.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        when(weapon.getItem()).thenReturn(sword);
    }

    @Test
    void meleeMobAcceptsShieldItem() {
        ShieldItem shieldItem = mock(ShieldItem.class);
        when(shield.getItem()).thenReturn(shieldItem);
        assertTrue(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }

    @Test
    void meleeMobAcceptsTaggedModShield() {
        when(shield.is(Tags.Items.TOOLS_SHIELDS)).thenReturn(true);
        assertTrue(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }

    @Test
    void shieldRequiresWeaponsPermission() {
        when(shield.is(Tags.Items.TOOLS_SHIELDS)).thenReturn(true);
        assertFalse(ConfiguredModMobs.isAllowedEquipment(mob, shield, true, false));
    }

    @Test
    void shieldDoesNotReplaceOccupiedOffhand() {
        when(shield.is(Tags.Items.TOOLS_SHIELDS)).thenReturn(true);
        ItemStack occupied = mock(ItemStack.class);
        when(mob.getOffhandItem()).thenReturn(occupied);
        assertFalse(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }

    @Test
    void rangedMobDoesNotPickUpShield() {
        when(shield.is(Tags.Items.TOOLS_SHIELDS)).thenReturn(true);
        BowItem bow = mock(BowItem.class);
        when(weapon.getItem()).thenReturn(bow);
        assertFalse(ConfiguredModMobs.isAllowedEquipment(mob, shield, false, true));
    }
}
