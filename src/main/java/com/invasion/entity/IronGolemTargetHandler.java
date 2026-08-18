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
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Targets Nexus-bound IM monsters for golems and player-supporting wolves. */
public final class IronGolemTargetHandler {
    private static final double TARGET_RANGE_SQR = 32.0D * 32.0D;
	private static final Map<ServerLevel, Set<Mob>> DEFENDERS = new WeakHashMap<>();

    private IronGolemTargetHandler() {}

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IronGolemTargetHandler::onJoin);
        NeoForge.EVENT_BUS.addListener(IronGolemTargetHandler::onLeave);
        NeoForge.EVENT_BUS.addListener(IronGolemTargetHandler::tick);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
		if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob
				&& isDefender(mob)) {
			DEFENDERS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(mob);
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
		Set<Mob> defenders = DEFENDERS.get(level);
		if (defenders != null) defenders.remove(mob);
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10L != 0L) return;
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
			if (IMMobFriendlyFireHandler.isHiddenInternalTarget(current)) {
				defender.setTarget(null);
				current = null;
			}
			if (current != null && current.isAlive() && !current.isRemoved()
					&& (current instanceof Combatant<?> ? loadedTargets.contains(current)
							: !BuiltInRegistries.ENTITY_TYPE.getKey(current.getType())
									.getNamespace().equals(InvasionMod.MOD_ID))) continue;
			if (current != null) defender.setTarget(null);
            LivingEntity nearest = null;
            double nearestDistance = TARGET_RANGE_SQR;
            for (Combatant<?> combatant : combatants) {
                LivingEntity candidate = combatant.asEntity();
                if (!candidate.isAlive() || candidate.isRemoved()
						|| candidate == defender || candidate instanceof IMWolfEntity
						|| IMMobFriendlyFireHandler.isHiddenInternalTarget(candidate)
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
				|| mob instanceof Wolf wolf && wolf.isTame()
				|| BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())
						.equals(net.minecraft.resources.ResourceLocation
								.fromNamespaceAndPath("guardvillagers", "guard"));
	}
}
