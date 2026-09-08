package com.invasion.entity;

import com.invasion.InvasionMod;
import com.invasion.compat.ConfiguredModMobs;
import com.invasion.nexus.Combatant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Prevents Nexus combatants from treating other Nexus combatants as enemies. */
public final class IMMobFriendlyFireHandler {
    private static final Object TARGET_LOCK = new Object();
    private static final Map<ServerLevel, Set<Mob>> IM_TARGETING_MOBS =
            new WeakHashMap<>();

    private IMMobFriendlyFireHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::onTargetChanged);
        NeoForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::onLeave);
        NeoForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::tickTargets);
    }

    private static void onTargetChanged(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        Mob mob = event.getEntity() instanceof Mob source ? source : null;
        if (isHiddenInternalTarget(target)
                || mob != null && isInvmodTarget(target)
                        && !mob.hasLineOfSight(target)) {
            event.setNewAboutToBeSetTarget(null);
            untrack(mob);
            return;
        }
        if (isInvasionAlly(event.getEntity()) && isInvasionAlly(target)) {
            event.setNewAboutToBeSetTarget(null);
            untrack(mob);
            return;
        }
        track(mob, target);
    }

    private static void track(Mob mob, LivingEntity target) {
        if (mob != null && (isInvmodTarget(target) || isInvasionAlly(target))
                && mob.level() instanceof ServerLevel level) {
            synchronized (TARGET_LOCK) {
                IM_TARGETING_MOBS.computeIfAbsent(level, ignored ->
                        Collections.newSetFromMap(new IdentityHashMap<>()))
                        .add(mob);
            }
        } else {
            untrack(mob);
        }
    }

    private static void tickTargets(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % 10L != 0L) return;
        List<Mob> snapshot;
        synchronized (TARGET_LOCK) {
            Set<Mob> tracked = IM_TARGETING_MOBS.get(level);
            if (tracked == null) return;
            snapshot = List.copyOf(tracked);
        }
        for (Mob mob : snapshot) {
            LivingEntity target = mob.getTarget();
            if (!mob.isAlive() || mob.isRemoved()
                    || !(isInvmodTarget(target) || isInvasionAlly(target))) {
                untrack(mob);
            } else if (isInvasionAlly(mob) && isInvasionAlly(target)
                    || isHiddenInternalTarget(target)
                    || isInvmodTarget(target) && !mob.hasLineOfSight(target)) {
                mob.setTarget(null);
                untrack(mob);
            }
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) untrack(mob);
    }

    private static void untrack(Mob mob) {
        if (mob == null || !(mob.level() instanceof ServerLevel level)) return;
        synchronized (TARGET_LOCK) {
            Set<Mob> tracked = IM_TARGETING_MOBS.get(level);
            if (tracked != null) {
                tracked.remove(mob);
                if (tracked.isEmpty()) IM_TARGETING_MOBS.remove(level);
            }
        }
    }

    public static boolean isHiddenInternalTarget(LivingEntity target) {
        return isInvmodTarget(target)
                && (target instanceof SpawnProxyEntity
                        || target.isInvisible()
                        || !target.isAttackable());
    }

    public static boolean isInvmodTarget(LivingEntity target) {
        return target != null
                && BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())
                        .getNamespace().equals(InvasionMod.MOD_ID);
    }

    private static boolean isInvasionAlly(Entity entity) {
        return entity instanceof Combatant<?>
                || entity instanceof Mob mob && ConfiguredModMobs.isInvasionAlly(mob);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (isInvasionAlly(event.getEntity()) && isInvasionAlly(attacker)) {
            event.setCanceled(true);
        }
    }
}
