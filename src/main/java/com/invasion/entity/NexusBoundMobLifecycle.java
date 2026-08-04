package com.invasion.entity;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Enforces the lifetime shared by every hostile Nexus-bound IM mob. */
public final class NexusBoundMobLifecycle {
    private NexusBoundMobLifecycle() {
    }

    public static void bootstrap() {
        ServerTickEvents.END_LEVEL_TICK.register(NexusBoundMobLifecycle::tick);
    }

    private static void tick(ServerLevel level) {
        for (Combatant<?> combatant : BoundIMMobRegistry.bound(level)) {
            LivingEntity living = combatant.asEntity();
            if (living instanceof IMWolfEntity
                    || !combatant.getNexusHandle().hasBinding()) {
                continue;
            }
            NexusAccess nexus = combatant.getNexus();
            if (nexus == null || nexus.isDiscarded() || !nexus.isActive()) {
                if (living instanceof IMSlimeEntity slime) {
                    slime.suppressSplitOnNexusDeath();
                } else if (living instanceof IMMagmaCubeEntity magmaCube) {
                    magmaCube.suppressSplitOnNexusDeath();
                }
                combatant.setNexus(null);
                living.kill(level);
            }
        }
    }
}
