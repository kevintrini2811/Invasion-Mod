package com.invasion.entity;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invasion.nexus.NexusAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class VanillaMobSpawnReplacementTest {
    @Test
    @SuppressWarnings("unchecked")
    void conversionTransfersStateAndPreservesLifecycleOrder() {
        ServerLevel world = mock(ServerLevel.class);
        Zombie source = mock(Zombie.class);
        EntityIMZombie converted = mock(EntityIMZombie.class);
        EntityType<EntityIMZombie> targetType = mock(EntityType.class);
        NexusAccess nexus = mock(NexusAccess.class);
        AttributeInstance maxHealth = mock(AttributeInstance.class);
        Vec3 movement = new Vec3(0.25D, -0.1D, 0.5D);

        when(source.level()).thenReturn(world);
        when(source.getX()).thenReturn(12.5D);
        when(source.getY()).thenReturn(64.0D);
        when(source.getZ()).thenReturn(-8.5D);
        when(source.getYRot()).thenReturn(90.0F);
        when(source.getXRot()).thenReturn(-15.0F);
        when(source.getDeltaMovement()).thenReturn(movement);
        when(source.isBaby()).thenReturn(true);
        when(source.isNoAi()).thenReturn(true);
        when(source.isPersistenceRequired()).thenReturn(true);
        when(source.getMaxHealth()).thenReturn(40.0F);
        when(targetType.create(world, EntitySpawnReason.CONVERSION))
                .thenReturn(converted);
        when(converted.getAttribute(Attributes.MAX_HEALTH))
                .thenReturn(maxHealth);
        when(world.addFreshEntity(converted)).thenReturn(true);

        VanillaMobSpawnReplacement.convert(source, targetType, nexus);

        assertNotSame(source, converted);
        verify(converted).snapTo(12.5D, 64.0D, -8.5D, 90.0F, -15.0F);
        verify(converted).setDeltaMovement(movement);
        verify(converted).setBaby(true);
        verify(converted).setNoAi(true);
        verify(converted).setPersistenceRequired();
        verify(maxHealth).setBaseValue(12.0D);
        verify(converted).setHealth(12.0F);
        verify(converted).setCountsTowardMobCap(true);

        InOrder lifecycle = inOrder(source, world, converted);
        lifecycle.verify(world).addFreshEntity(converted);
        lifecycle.verify(source).stopRiding();
        lifecycle.verify(source).discard();
        lifecycle.verify(converted).setNexus(nexus);
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedSpawnPreservesSourceAndLeavesReplacementUnbound() {
        ServerLevel world = mock(ServerLevel.class);
        Zombie source = mock(Zombie.class);
        Entity vehicle = mock(Entity.class);
        EntityIMZombie converted = mock(EntityIMZombie.class);
        EntityType<EntityIMZombie> targetType = mock(EntityType.class);
        NexusAccess nexus = mock(NexusAccess.class);
        AttributeInstance maxHealth = mock(AttributeInstance.class);

        when(source.level()).thenReturn(world);
        when(source.getVehicle()).thenReturn(vehicle);
        when(source.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(source.getMaxHealth()).thenReturn(20.0F);
        when(targetType.create(world, EntitySpawnReason.CONVERSION))
                .thenReturn(converted);
        when(converted.getAttribute(Attributes.MAX_HEALTH))
                .thenReturn(maxHealth);
        when(world.addFreshEntity(converted)).thenReturn(false);

        VanillaMobSpawnReplacement.convert(source, targetType, nexus);

        verify(source, never()).stopRiding();
        verify(source, never()).discard();
        verify(converted, never()).setNexus(any());
        verify(converted, never()).startRiding(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingReplacementDoesNotRemoveSource() {
        ServerLevel world = mock(ServerLevel.class);
        Zombie source = mock(Zombie.class);
        EntityType<EntityIMZombie> targetType = mock(EntityType.class);
        NexusAccess nexus = mock(NexusAccess.class);

        when(source.level()).thenReturn(world);
        when(targetType.create(world, EntitySpawnReason.CONVERSION))
                .thenReturn(null);

        VanillaMobSpawnReplacement.convert(source, targetType, nexus);

        verify(source, never()).stopRiding();
        verify(source, never()).discard();
        verify(world, never()).addFreshEntity(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void drownedConversionCopiesEquipmentWithoutSharingStacks() {
        ServerLevel world = mock(ServerLevel.class);
        Drowned source = mock(Drowned.class);
        IMDrownedEntity converted = mock(IMDrownedEntity.class);
        EntityType<IMDrownedEntity> targetType = mock(EntityType.class);
        NexusAccess nexus = mock(NexusAccess.class);
        AttributeInstance maxHealth = mock(AttributeInstance.class);
        ItemStack sourceWeapon = mock(ItemStack.class);
        ItemStack copiedWeapon = mock(ItemStack.class);

        when(source.level()).thenReturn(world);
        when(source.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(source.getMaxHealth()).thenReturn(30.0F);
        when(source.getItemBySlot(any())).thenReturn(ItemStack.EMPTY);
        when(source.getItemBySlot(EquipmentSlot.MAINHAND))
                .thenReturn(sourceWeapon);
        when(sourceWeapon.copy()).thenReturn(copiedWeapon);
        when(targetType.create(world, EntitySpawnReason.CONVERSION))
                .thenReturn(converted);
        when(converted.getAttribute(Attributes.MAX_HEALTH))
                .thenReturn(maxHealth);
        when(world.addFreshEntity(converted)).thenReturn(true);

        VanillaMobSpawnReplacement.convert(source, targetType, nexus);

        verify(converted).setItemSlot(EquipmentSlot.MAINHAND, copiedWeapon);
        verify(converted, never())
                .setItemSlot(EquipmentSlot.MAINHAND, sourceWeapon);
        verify(converted).setNexus(nexus);
    }
}
