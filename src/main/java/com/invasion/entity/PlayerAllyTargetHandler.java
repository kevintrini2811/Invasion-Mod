package com.invasion.entity;

import com.invasion.nexus.Combatant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Gives visible player allies priority over pursuing or attacking a Nexus. */
public final class PlayerAllyTargetHandler {
    private static final double TARGET_RANGE = 32.0D;

    private PlayerAllyTargetHandler() {}

    public static void bootstrap() {
        ServerTickEvents.END_LEVEL_TICK.register(PlayerAllyTargetHandler::tick);
    }

    private static void tick(ServerLevel level) {
        if (level.getGameTime() % 5L != 0L) return;

        for (Combatant<?> combatant : BoundIMMobRegistry.loaded(level)) {
            LivingEntity entity = combatant.asEntity();
            if (!(entity instanceof Mob mob) || isExcluded(mob)) continue;

            LivingEntity current = mob.getTarget();
            if (isVisiblePlayerAlly(mob, current, level)) {
                prioritizeTarget(mob, current);
                continue;
            }

            if (current != null && IMWitchEntity.isPlayerAlly(current, level)) {
                mob.setTarget(null);
                if (mob instanceof NexusEntity nexusEntity
                        && nexusEntity.hasNexus()) {
                    nexusEntity.transitionAIGoal(HasAiGoals.Goal.BREAK_NEXUS);
                }
            }

            LivingEntity nearest = null;
            double nearestDistance = TARGET_RANGE * TARGET_RANGE;
            for (LivingEntity candidate : level.getEntitiesOfClass(
                    LivingEntity.class,
                    mob.getBoundingBox().inflate(TARGET_RANGE),
                    candidate -> isVisiblePlayerAlly(mob, candidate, level))) {
                double distance = mob.distanceToSqr(candidate);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
            if (nearest != null) prioritizeTarget(mob, nearest);
        }
    }

    private static boolean isVisiblePlayerAlly(
            Mob mob, LivingEntity candidate, ServerLevel level) {
        return candidate != null
                && IMWitchEntity.isPlayerAlly(candidate, level)
                && candidate.isAlive()
                && !candidate.isRemoved()
                && mob.canAttack(candidate)
                && mob.hasLineOfSight(candidate);
    }

    private static void prioritizeTarget(Mob mob, LivingEntity target) {
        mob.setTarget(target);
        if (mob instanceof NexusEntity nexusEntity) {
            nexusEntity.transitionAIGoal(HasAiGoals.Goal.TARGET_ENTITY);
        }
    }

    private static boolean isExcluded(Mob mob) {
        return mob instanceof IMWitherEntity
                || mob instanceof PigmanEngineerEntity
                || mob instanceof ZombieBuilderEntity
                || mob instanceof IMWolfEntity;
    }
}
