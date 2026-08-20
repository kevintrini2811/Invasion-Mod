package com.invasion.compat;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;
import com.faboslav.friendsandfoes.common.entity.WildfireShieldDebrisEntity;

import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

final class WildfireNexusHandler {
    private WildfireNexusHandler() {
    }

    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof WildfireShieldDebrisEntity debris)
                || !(debris.getOwner() instanceof Combatant<?> combatant)
                || !FriendsAndFoesCompatibility.isAnyWildfire(
                        debris.getOwner().getType())
                || !(event.getRayTraceResult() instanceof BlockHitResult hit)) {
            return;
        }
        NexusAccess nexus = combatant.getNexus();
        if (nexus != null && hit.getBlockPos().equals(nexus.getOrigin())) {
            nexus.damage(debris.damageSources().fireball(
                    debris, debris.getOwner()), 5);
            event.setCanceled(true);
            debris.discard();
        }
    }
}
