package com.invasion.entity;

import com.invasion.nexus.Combatant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Restores vanilla-style Iron Golem hostility towards Nexus-bound IM monsters. */
public final class IronGolemTargetHandler {
    private static final double TARGET_RANGE_SQR = 32.0D * 32.0D;
    private static final Map<ServerLevel, Set<IronGolem>> GOLEMS = new WeakHashMap<>();

    private IronGolemTargetHandler() {}

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IronGolemTargetHandler::onJoin);
        NeoForge.EVENT_BUS.addListener(IronGolemTargetHandler::onLeave);
        NeoForge.EVENT_BUS.addListener(IronGolemTargetHandler::tick);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof IronGolem golem) {
            GOLEMS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(golem);
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof IronGolem golem)) return;
        Set<IronGolem> golems = GOLEMS.get(level);
        if (golems != null) golems.remove(golem);
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10L != 0L) return;
        Set<IronGolem> golems = GOLEMS.get(level);
        if (golems == null) return;
        golems.removeIf(golem -> !golem.isAlive() || golem.isRemoved());
        if (golems.isEmpty()) { GOLEMS.remove(level); return; }

        var combatants = BoundIMMobRegistry.activeBound(level);
        for (IronGolem golem : golems) {
            if (golem.getTarget() != null && golem.getTarget().isAlive()) continue;
            LivingEntity nearest = null;
            double nearestDistance = TARGET_RANGE_SQR;
            for (Combatant<?> combatant : combatants) {
                LivingEntity candidate = combatant.asEntity();
                if (!candidate.isAlive() || candidate.isRemoved()
                        || candidate instanceof IMCreeperEntity || !golem.canAttack(candidate)) continue;
                double distance = golem.distanceToSqr(candidate);
                if (distance < nearestDistance) { nearest = candidate; nearestDistance = distance; }
            }
            if (nearest != null) golem.setTarget(nearest);
        }
    }
}
