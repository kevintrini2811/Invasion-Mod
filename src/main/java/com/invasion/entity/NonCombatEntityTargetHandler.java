package com.invasion.entity;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/** Keeps internal helper entities out of vanilla and modded combat AI. */
public final class NonCombatEntityTargetHandler {
    private NonCombatEntityTargetHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(
                NonCombatEntityTargetHandler::onTargetChange);
    }

    private static void onTargetChange(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target instanceof SpawnProxyEntity) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
