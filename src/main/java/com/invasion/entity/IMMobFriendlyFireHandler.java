package com.invasion.entity;

import com.invasion.nexus.Combatant;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;

/** Prevents Nexus combatants from treating other Nexus combatants as enemies. */
public final class IMMobFriendlyFireHandler {
    private IMMobFriendlyFireHandler() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::onTargetChanged);
        MinecraftForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::onLivingAttack);
    }

    private static void onTargetChanged(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof SpawnProxyEntity) {
            event.setNewTarget(null);
            return;
        }
        if (event.getEntity() instanceof Combatant<?>
                && event.getNewTarget() instanceof Combatant<?>) {
            event.setNewTarget(null);
        }
    }

    private static void onLivingAttack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Combatant<?> && attacker instanceof Combatant<?>) {
            event.setCanceled(true);
        }
    }
}
