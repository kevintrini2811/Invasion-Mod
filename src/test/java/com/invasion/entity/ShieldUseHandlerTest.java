package com.invasion.entity;

import static org.mockito.Mockito.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShieldUseHandlerTest {
    private Mob mob;
    private ServerLevel level;
    private ItemStack shield;

    @BeforeEach
    void setUp() {
        mob = mock(Mob.class);
        level = mock(ServerLevel.class);
        ItemStack weapon = mock(ItemStack.class);
        shield = mock(ItemStack.class);
        CompoundTag data = new CompoundTag();
        data.putInt("invmodWaveNumber", 10);
        when(mob.getPersistentData()).thenReturn(data);
        when(mob.level()).thenReturn(level);
        when(mob.isAlive()).thenReturn(true);
        when(mob.getMainHandItem()).thenReturn(weapon);
        when(mob.getOffhandItem()).thenReturn(shield);
        when(weapon.is(Tags.Items.MELEE_WEAPON_TOOLS)).thenReturn(true);
        when(shield.is(Tags.Items.TOOLS_SHIELD)).thenReturn(true);
        when(level.getGameTime()).thenReturn(100L);
    }

    private LivingEntity visibleTarget() {
        LivingEntity target = mock(LivingEntity.class);
        when(target.isAlive()).thenReturn(true);
        when(target.getEyePosition()).thenReturn(new Vec3(30, 64, 0));
        when(mob.getTarget()).thenReturn(target);
        when(mob.hasLineOfSight(target)).thenReturn(true);
        when(mob.distanceToSqr(target)).thenReturn(900.0);
        return target;
    }

    private void usingShield() {
        when(mob.isUsingItem()).thenReturn(true);
        when(mob.getUsedItemHand()).thenReturn(InteractionHand.OFF_HAND);
        when(mob.getUseItem()).thenReturn(shield);
    }

    private void arrowHit() {
        LivingIncomingDamageEvent event = mock(LivingIncomingDamageEvent.class);
        DamageSource source = mock(DamageSource.class);
        when(event.getEntity()).thenReturn(mob);
        when(event.getSource()).thenReturn(source);
        when(source.is(DamageTypeTags.IS_PROJECTILE)).thenReturn(true);
        when(source.getDirectEntity()).thenReturn(mock(AbstractArrow.class));
        when(source.getSourcePosition()).thenReturn(new Vec3(8, 64, 0));
        ShieldUseHandler.onIncomingDamage(event);
    }

    @Test
    void visibleTargetBeyondSixteenBlocksRaisesModShield() {
        visibleTarget();
        ShieldUseHandler.update(mob);
        verify(mob).startUsingItem(InteractionHand.OFF_HAND);
    }

    @Test
    void lostSightLowersShieldWithoutArrowReaction() {
        LivingEntity target = visibleTarget();
        usingShield();
        when(mob.hasLineOfSight(target)).thenReturn(false);
        ShieldUseHandler.update(mob);
        verify(mob).stopUsingItem();
    }

    @Test
    void nexusProxyDoesNotRaiseShield() {
        SpawnProxyEntity proxy = mock(SpawnProxyEntity.class);
        when(proxy.isAlive()).thenReturn(true);
        when(mob.getTarget()).thenReturn(proxy);
        when(mob.hasLineOfSight(proxy)).thenReturn(true);
        ShieldUseHandler.update(mob);
        verify(mob, never()).startUsingItem(any());
    }

    @Test
    void arrowReactionExpiresAfterFifteenSeconds() {
        arrowHit();
        when(level.getGameTime()).thenReturn(399L);
        ShieldUseHandler.update(mob);
        verify(mob).startUsingItem(InteractionHand.OFF_HAND);
        usingShield();
        when(level.getGameTime()).thenReturn(400L);
        ShieldUseHandler.update(mob);
        verify(mob).stopUsingItem();
    }

    @Test
    void visibleTargetReplacesArrowReaction() {
        arrowHit();
        visibleTarget();
        ShieldUseHandler.update(mob);
        usingShield();
        when(mob.getTarget()).thenReturn(null);
        ShieldUseHandler.update(mob);
        verify(mob).stopUsingItem();
    }

    @Test
    void attackLowersShieldImmediatelyThenResumesBlocking() {
        visibleTarget();
        usingShield();
        ShieldUseHandler.onAttack(mob, InteractionHand.MAIN_HAND);
        verify(mob).stopUsingItem();
        when(mob.isUsingItem()).thenReturn(false);
        when(level.getGameTime()).thenReturn(104L);
        ShieldUseHandler.update(mob);
        verify(mob, never()).startUsingItem(any());
        when(level.getGameTime()).thenReturn(105L);
        ShieldUseHandler.update(mob);
        verify(mob).startUsingItem(InteractionHand.OFF_HAND);
    }

    @Test
    void mainHandItemUseIsNotInterrupted() {
        when(mob.isUsingItem()).thenReturn(true);
        when(mob.getUsedItemHand()).thenReturn(InteractionHand.MAIN_HAND);
        ShieldUseHandler.update(mob);
        verify(mob, never()).stopUsingItem();
    }
}
