package com.invasion.compat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class WildfireNexusHandler {
    private static final Map<ServerLevel, Set<SmallFireball>> FIREBALLS =
            new WeakHashMap<>();

    private WildfireNexusHandler() {
    }

    static void bootstrap() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof SmallFireball fireball) {
                FIREBALLS.computeIfAbsent(level, ignored ->
                        Collections.newSetFromMap(new IdentityHashMap<>()))
                        .add(fireball);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            Set<SmallFireball> fireballs = FIREBALLS.get(level);
            if (fireballs != null && entity instanceof SmallFireball fireball) {
                fireballs.remove(fireball);
            }
        });
        ServerTickEvents.START_LEVEL_TICK.register(
                WildfireNexusHandler::checkImpacts);
    }

    private static void checkImpacts(ServerLevel level) {
        Set<SmallFireball> fireballs = FIREBALLS.get(level);
        if (fireballs == null) return;
        for (SmallFireball fireball : Set.copyOf(fireballs)) {
            if (!(fireball.getOwner() instanceof Combatant<?> combatant)
                    || !FriendsAndFoesCompatibility.isAnyWildfire(
                            fireball.getOwner().getType())) continue;
            NexusAccess nexus = combatant.getNexus();
            if (nexus == null) continue;
            BlockHitResult hit = level.clip(new ClipContext(
                    fireball.position(),
                    fireball.position().add(fireball.getDeltaMovement()),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    fireball));
            if (hit.getType() == HitResult.Type.BLOCK
                    && hit.getBlockPos().equals(nexus.getOrigin())) {
                nexus.damage(fireball.damageSources().fireball(
                        fireball, fireball.getOwner()), 2);
                fireball.discard();
            }
        }
    }
}
