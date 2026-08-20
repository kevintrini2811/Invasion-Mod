package com.invasion.compat;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;

import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

final class WildfireNexusHandler {
    private WildfireNexusHandler() {
    }

    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof SmallFireball fireball)
                || !(fireball.getOwner() instanceof Combatant<?> combatant)
                || !FriendsAndFoesCompatibility.isAnyWildfire(
                        fireball.getOwner().getType())
                || !(event.getRayTraceResult() instanceof BlockHitResult hit)) {
            return;
        }
        NexusAccess nexus = combatant.getNexus();
        if (nexus != null && hit.getBlockPos().equals(nexus.getOrigin())) {
            nexus.damage(fireball.damageSources().fireball(
                    fireball, fireball.getOwner()), 2);
            event.setCanceled(true);
            fireball.discard();
        }
    }
}
