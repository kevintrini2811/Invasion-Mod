package com.invasion.entity;

import java.util.Comparator;

import com.invasion.nexus.Combatant;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Gives every hostile IM mob the shared civilian target set. */
public final class IMCivilianTargetHandler {
    private static final double TARGET_RANGE = 32.0D;

    private IMCivilianTargetHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IMCivilianTargetHandler::tick);
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % 10L != 0L) {
            return;
        }
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)
                    || !(entity instanceof Combatant<?>)
                    || entity instanceof IMWolfEntity
                    || !mob.isAlive()
                    || mob.getTarget() != null && mob.getTarget().isAlive()) {
                continue;
            }
            level.getEntitiesOfClass(LivingEntity.class,
                            mob.getBoundingBox().inflate(TARGET_RANGE),
                            candidate -> isCivilian(candidate)
                                    && candidate.isAlive()
                                    && mob.canAttack(candidate))
                    .stream()
                    .min(Comparator.comparingDouble(mob::distanceToSqr))
                    .ifPresent(mob::setTarget);
        }
    }

    private static boolean isCivilian(LivingEntity entity) {
        return entity instanceof AbstractVillager
                || entity instanceof AbstractPiglin
                || entity instanceof Pig;
    }
}
