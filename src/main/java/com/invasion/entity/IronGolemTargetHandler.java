package com.invasion.entity;

import com.invasion.InvasionMod;
import com.invasion.nexus.Combatant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Entity;

/** Targets Nexus-bound IM monsters for golems and player-supporting wolves. */
public final class IronGolemTargetHandler {
    private static final double TARGET_RANGE_SQR = 32.0D * 32.0D;
	private static final Map<ServerLevel, Set<Mob>> DEFENDERS = new WeakHashMap<>();

    private IronGolemTargetHandler() {}

    public static void bootstrap() {
        ServerEntityEvents.ENTITY_LOAD.register(IronGolemTargetHandler::onJoin);
        ServerEntityEvents.ENTITY_UNLOAD.register(IronGolemTargetHandler::onLeave);
        ServerTickEvents.END_LEVEL_TICK.register(IronGolemTargetHandler::tick);
    }

    private static void onJoin(Entity entity, ServerLevel level) {
		if (entity instanceof Mob mob
				&& (mob instanceof IronGolem || mob instanceof Wolf)) {
			DEFENDERS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(mob);
        }
    }

    private static void onLeave(Entity entity, ServerLevel level) {
		if (!(entity instanceof Mob mob)) return;
		Set<Mob> defenders = DEFENDERS.get(level);
		if (defenders != null) defenders.remove(mob);
    }

    private static void tick(ServerLevel level) {
        if (level.getGameTime() % 10L != 0L) return;
		Set<Mob> defenders = DEFENDERS.get(level);
		if (defenders == null) return;
		defenders.removeIf(defender -> !defender.isAlive() || defender.isRemoved());
		if (defenders.isEmpty()) { DEFENDERS.remove(level); return; }

		var combatants = BoundIMMobRegistry.loaded(level);
		Set<LivingEntity> loadedTargets = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Combatant<?> combatant : combatants) {
			LivingEntity entity = combatant.asEntity();
			if (entity.isAlive() && !entity.isRemoved()) loadedTargets.add(entity);
		}
		for (Mob defender : defenders) {
			if (!isDefender(defender)) continue;
			LivingEntity current = defender.getTarget();
			if (current != null && current.isAlive() && !current.isRemoved()
					&& (current instanceof Combatant<?> ? loadedTargets.contains(current)
							: !BuiltInRegistries.ENTITY_TYPE.getKey(current.getType())
									.getNamespace().equals("invmod"))) continue;
			if (current != null) defender.setTarget(null);
            LivingEntity nearest = null;
            double nearestDistance = TARGET_RANGE_SQR;
            for (Combatant<?> combatant : combatants) {
                LivingEntity candidate = combatant.asEntity();
                if (!candidate.isAlive() || candidate.isRemoved()
						|| candidate == defender || candidate instanceof IMWolfEntity
						|| !defender.canAttack(candidate)) continue;
				double distance = defender.distanceToSqr(candidate);
                if (distance < nearestDistance) { nearest = candidate; nearestDistance = distance; }
            }
			if (nearest != null) defender.setTarget(nearest);
        }
    }

	private static boolean isDefender(Mob mob) {
		return mob instanceof IronGolem
				|| mob instanceof IMWolfEntity
				|| mob instanceof Wolf wolf && wolf.isTame();
	}
}
