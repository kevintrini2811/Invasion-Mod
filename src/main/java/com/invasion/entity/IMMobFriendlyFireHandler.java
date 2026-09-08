package com.invasion.entity;

import com.invasion.nexus.Combatant;
import com.invasion.compat.ConfiguredModMobs;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Prevents Nexus combatants from treating other Nexus combatants as enemies. */
public final class IMMobFriendlyFireHandler {
    private static final Object TARGET_LOCK = new Object();
    private static final Map<ServerLevel, Set<Mob>> IM_TARGETING_MOBS =
            new WeakHashMap<>();

    private IMMobFriendlyFireHandler() {
    }

    public static void bootstrap() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(IMMobFriendlyFireHandler::allowDamage);
        ServerTickEvents.END_LEVEL_TICK.register(IMMobFriendlyFireHandler::tickTargets);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Mob mob) untrack(mob);
        });
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            for (Combatant<?> combatant : BoundIMMobRegistry.loaded(level)) {
                LivingEntity entity = combatant.asEntity();
                if (entity instanceof Mob mob && isInvasionAlly(mob.getTarget())) {
                    mob.setTarget(null);
                }
            }
        });
    }

    public static boolean allowTarget(Mob mob, LivingEntity target) {
        if (isHiddenInternalTarget(target)
                || isInvmodTarget(target) && !mob.hasLineOfSight(target)
                || isInvasionAlly(mob) && isInvasionAlly(target)) {
            untrack(mob);
            return false;
        }
        if ((isInvmodTarget(target) || isInvasionAlly(target)) && mob.level() instanceof ServerLevel level) {
            synchronized (TARGET_LOCK) {
                IM_TARGETING_MOBS.computeIfAbsent(level, ignored ->
                        Collections.newSetFromMap(new IdentityHashMap<>()))
                        .add(mob);
            }
        } else {
            untrack(mob);
        }
        return true;
    }

    private static void tickTargets(ServerLevel level) {
        if (level.getGameTime() % 10L != 0L) return;
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

    private static boolean isHiddenInternalTarget(LivingEntity target) {
        return isInvmodTarget(target)
                && (target instanceof SpawnProxyEntity
                        || target.isInvisible()
                        || !target.isAttackable());
    }

    private static boolean isInvmodTarget(LivingEntity target) {
        return target != null
                && BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())
                        .getNamespace().equals("invmod");
    }

    private static boolean isInvasionAlly(Entity entity) {
        return entity instanceof Combatant<?>
                || entity instanceof Mob mob && ConfiguredModMobs.isInvasionAlly(mob);
    }

    private static boolean allowDamage(
            LivingEntity victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        return !(isInvasionAlly(victim) && isInvasionAlly(attacker));
    }
}
