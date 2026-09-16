package com.invasion.entity;

import com.invasion.nexus.Combatant;
import com.invasion.compat.ConfiguredModMobs;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
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
    private static final int ATTACK_WINDUP_TICKS = 20;
    private static final int BLOCK_COOLDOWN_TICKS = 4 * 20;
    private static final int BLOCK_LIMIT = 3;

    private ShieldUseHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(ShieldUseHandler::tick);
        NeoForge.EVENT_BUS.addListener(ShieldUseHandler::onIncomingDamage);
    }

    static void tick(EntityTickEvent.Post event) {
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
        if (defense != null && defense.attackReadyAt >= 0
                && now > defense.attackReadyAt + ATTACK_WINDUP_TICKS) {
            // Abandon an attack that the AI no longer attempts (for example, after fleeing).
            defense.attackReadyAt = -1;
        }
        if (visibleTarget && defense != null) {
            // A visible combat target replaces the reaction to the last arrow.
            defense.arrowUntil = 0;
        }
        boolean arrowDefense = defense != null && now < defense.arrowUntil;
        if (defense != null && now >= defense.attackUntil && !arrowDefense
                && defense.attackReadyAt < 0 && now >= defense.blockedUntil
                && defense.successfulBlocks == 0) {
            DEFENSE.remove(mob);
            defense = null;
        }
        boolean attacking = mob.swinging && mob.swingingArm == InteractionHand.MAIN_HAND
                || defense != null && (now < defense.attackUntil || defense.attackReadyAt >= 0
                        || now < defense.blockedUntil);
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
                && (mob instanceof Combatant<?> || mob.getPersistentData().contains("invmodWaveNumber")
                        || ConfiguredModMobs.isActive(mob.getType()));
    }

    private static boolean canBlock(Mob mob) {
        return mob.isAlive() && !mob.isNoAi()
                && EquipmentUtil.isMeleeWeapon(mob.getMainHandItem())
                && EquipmentUtil.isShield(mob.getOffhandItem());
    }

    public static boolean prepareAttack(Mob mob) {
        if (!isShieldMob(mob) || !canBlock(mob)) return true;
        Defense defense = DEFENSE.computeIfAbsent(mob, ignored -> new Defense());
        long now = mob.level().getGameTime();
        if (defense.lastAttackAt == now) return true;
        if (defense.attackReadyAt < 0) {
            defense.attackReadyAt = now + ATTACK_WINDUP_TICKS;
        }
        stopBlocking(mob);
        return now >= defense.attackReadyAt;
    }

    public static boolean onAttack(LivingEntity entity, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Mob mob)
                || !isShieldMob(mob) || !canBlock(mob)) return true;
        if (!prepareAttack(mob)) return false;
        Defense defense = DEFENSE.get(mob);
        long now = mob.level().getGameTime();
        defense.lastAttackAt = now;
        defense.attackReadyAt = -1;
        defense.attackUntil = now + ATTACK_PAUSE_TICKS;
        return true;
    }

    public static void onSuccessfulBlock(LivingEntity entity, float blockedDamage) {
        if (blockedDamage <= 0 || !(entity instanceof Mob mob) || !isShieldMob(mob)
                || !canBlock(mob)) return;
        Defense defense = DEFENSE.computeIfAbsent(mob, ignored -> new Defense());
        if (++defense.successfulBlocks == BLOCK_LIMIT) {
            defense.successfulBlocks = 0;
            defense.blockedUntil = mob.level().getGameTime() + BLOCK_COOLDOWN_TICKS;
            stopBlocking(mob);
        }
    }

    public static boolean isBlockingSuppressed(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !isShieldMob(mob)) return false;
        Defense defense = DEFENSE.get(mob);
        long now = mob.level().getGameTime();
        return defense != null && (now < defense.blockedUntil
                || defense.attackReadyAt >= 0 || now < defense.attackUntil);
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
            if (!onAttack(attacker, InteractionHand.MAIN_HAND)) event.setCanceled(true);
        }
    }

    private static final class Defense {
        private long arrowUntil;
        private long attackUntil;
        private long attackReadyAt = -1;
        private long lastAttackAt = -1;
        private long blockedUntil;
        private int successfulBlocks;
        private Vec3 arrowOrigin;
    }
}
