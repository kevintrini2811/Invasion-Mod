package com.invasion.entity;

import com.invasion.nexus.Combatant;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Prevents Nexus combatants from treating other Nexus combatants as enemies. */
public final class IMMobFriendlyFireHandler {
    private IMMobFriendlyFireHandler() {
    }

    public static void bootstrap() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(IMMobFriendlyFireHandler::allowDamage);
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            for (Combatant<?> combatant : BoundIMMobRegistry.loaded(level)) {
                LivingEntity entity = combatant.asEntity();
                if (entity instanceof Mob mob && mob.getTarget() instanceof Combatant<?>) {
                    mob.setTarget(null);
                }
            }
        });
    }

    private static boolean allowDamage(
            LivingEntity victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        return !(victim instanceof Combatant<?> && attacker instanceof Combatant<?>);
    }
}
