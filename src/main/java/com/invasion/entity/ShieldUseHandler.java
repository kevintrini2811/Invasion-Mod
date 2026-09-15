package com.invasion.entity;

import com.invasion.nexus.Combatant;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Makes shield-equipped invasion mobs actively block during combat. */
public final class ShieldUseHandler {
    private static final Map<Mob, Defense> DEFENSE = new WeakHashMap<>();
    private static final int ARROW_DEFENSE_TICKS = 15 * 20;
    private static final int ATTACK_PAUSE_TICKS = 5;

    private ShieldUseHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(ShieldUseHandler::tick);
        NeoForge.EVENT_BUS.addListener(ShieldUseHandler::onIncomingDamage);
    }

    private static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !isShieldMob(mob)) {
            return;
        }
        update(mob);
    }

    static void update(Mob mob) {
        Defense defense = DEFENSE.get(mob);
        LivingEntity target = mob.getTarget();
        boolean visibleTarget = target != null && target.isAlive()
                && !(target instanceof SpawnProxyEntity) && mob.hasLineOfSight(target);
        long now = mob.level().getGameTime();
        if (visibleTarget && defense != null) {
            // A visible combat target replaces the reaction to the last arrow.
            defense.arrowUntil = 0;
        }
        boolean arrowDefense = defense != null && now < defense.arrowUntil;
        if (defense != null && now >= defense.attackUntil && !arrowDefense) {
            DEFENSE.remove(mob);
            defense = null;
        }
        boolean attacking = mob.swinging && mob.swingingArm == InteractionHand.MAIN_HAND
                || defense != null && now < defense.attackUntil;
        if (canBlock(mob) && !attacking && (visibleTarget || arrowDefense)) {
            Vec3 lookAt = visibleTarget ? target.getEyePosition() : defense.arrowOrigin;
            mob.lookAt(EntityAnchorArgument.Anchor.EYES, lookAt);
            mob.setYBodyRot(mob.getYRot());
            if (!mob.isUsingItem()) {
                mob.startUsingItem(InteractionHand.OFF_HAND);
            }
        } else {
            stopBlocking(mob);
        }
    }

    private static void stopBlocking(Mob mob) {
        if (mob.isUsingItem()
                && mob.getUsedItemHand() == InteractionHand.OFF_HAND
                && EquipmentUtil.isShield(mob.getUseItem())) {
            mob.stopUsingItem();
        }
    }

    private static boolean isShieldMob(Mob mob) {
        return !mob.level().isClientSide()
                && (mob instanceof Combatant<?> || mob.getPersistentData().contains("invmodWaveNumber"));
    }

    private static boolean canBlock(Mob mob) {
        return mob.isAlive() && !mob.isNoAi()
                && EquipmentUtil.isMeleeWeapon(mob.getMainHandItem())
                && EquipmentUtil.isShield(mob.getOffhandItem());
    }

    public static void onAttack(LivingEntity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && entity instanceof Mob mob
                && isShieldMob(mob) && canBlock(mob)) {
            DEFENSE.computeIfAbsent(mob, ignored -> new Defense()).attackUntil =
                    mob.level().getGameTime() + ATTACK_PAUSE_TICKS;
            stopBlocking(mob);
        }
    }

    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            if (event.getSource().getDirectEntity() instanceof AbstractArrow
                    && !(event.getSource().getDirectEntity() instanceof ThrownTrident)
                    && event.getEntity() instanceof Mob mob && isShieldMob(mob) && canBlock(mob)) {
                Defense defense = DEFENSE.computeIfAbsent(mob, ignored -> new Defense());
                defense.arrowUntil = mob.level().getGameTime() + ARROW_DEFENSE_TICKS;
                var attacker = event.getSource().getEntity();
                defense.arrowOrigin = attacker != null ? attacker.getEyePosition()
                        : event.getSource().getSourcePosition();
                if (defense.arrowOrigin == null) defense.arrowOrigin = mob.getEyePosition();
            }
        } else if (event.getSource().getDirectEntity() instanceof Mob attacker) {
            // Also cover custom melee attacks that do not swing an arm.
            onAttack(attacker, InteractionHand.MAIN_HAND);
        }
    }

    private static final class Defense {
        private long arrowUntil;
        private long attackUntil;
        private Vec3 arrowOrigin;
    }
}
