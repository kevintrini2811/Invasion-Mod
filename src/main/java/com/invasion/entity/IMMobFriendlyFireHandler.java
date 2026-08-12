package com.invasion.entity;

import com.invasion.nexus.Combatant;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Prevents Nexus combatants from treating other Nexus combatants as enemies. */
public final class IMMobFriendlyFireHandler {
    private IMMobFriendlyFireHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::onTargetChanged);
        NeoForge.EVENT_BUS.addListener(IMMobFriendlyFireHandler::onIncomingDamage);
    }

    private static void onTargetChanged(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Combatant<?>
                && event.getNewAboutToBeSetTarget() instanceof Combatant<?>) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Combatant<?> && attacker instanceof Combatant<?>) {
            event.setCanceled(true);
        }
    }
}
