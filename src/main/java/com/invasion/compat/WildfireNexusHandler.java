package com.invasion.compat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;
import com.faboslav.friendsandfoes.common.entity.WildfireShieldDebrisEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class WildfireNexusHandler {
    private static final Map<ServerLevel, Set<WildfireShieldDebrisEntity>> DEBRIS =
            new WeakHashMap<>();

    private WildfireNexusHandler() {
    }

    static void bootstrap() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof WildfireShieldDebrisEntity debris) {
                DEBRIS.computeIfAbsent(level, ignored ->
                        Collections.newSetFromMap(new IdentityHashMap<>()))
                        .add(debris);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            Set<WildfireShieldDebrisEntity> debrisSet = DEBRIS.get(level);
            if (debrisSet != null
                    && entity instanceof WildfireShieldDebrisEntity debris) {
                debrisSet.remove(debris);
            }
        });
        ServerTickEvents.START_LEVEL_TICK.register(
                WildfireNexusHandler::checkImpacts);
    }

    private static void checkImpacts(ServerLevel level) {
        Set<WildfireShieldDebrisEntity> debrisSet = DEBRIS.get(level);
        if (debrisSet == null) return;
        for (WildfireShieldDebrisEntity debris : Set.copyOf(debrisSet)) {
            if (!(debris.getOwner() instanceof Combatant<?> combatant)
                    || !FriendsAndFoesCompatibility.isAnyWildfire(
                            debris.getOwner().getType())) continue;
            NexusAccess nexus = combatant.getNexus();
            if (nexus == null) continue;
            BlockHitResult hit = level.clip(new ClipContext(
                    debris.position(),
                    debris.position().add(debris.getDeltaMovement()),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    debris));
            if (hit.getType() == HitResult.Type.BLOCK
                    && hit.getBlockPos().equals(nexus.getOrigin())) {
                nexus.damage(debris.damageSources().fireball(
                        debris, debris.getOwner()), 5);
                debris.discard();
            }
        }
    }
}
