package com.invasion.entity;

import java.util.Comparator;

import com.invasion.nexus.Combatant;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Gives every hostile IM mob the shared civilian target set. */
public final class IMCivilianTargetHandler {
    private static final double TARGET_RANGE = 32.0D;

    private IMCivilianTargetHandler() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(IMCivilianTargetHandler::tick);
    }

    private static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 10L != 0L) {
            return;
        }
        for (Combatant<?> combatant : BoundIMMobRegistry.activeBound(level)) {
            LivingEntity entity = combatant.asEntity();
            if (!(entity instanceof Mob mob)
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
                || entity instanceof Pig
                || entity instanceof Hoglin;
    }
}
