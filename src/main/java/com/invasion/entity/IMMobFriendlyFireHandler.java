package com.invasion.entity;

import com.invasion.nexus.Combatant;
import com.invasion.InvasionMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (isHiddenInternalTarget(target)
                || event.getEntity() instanceof net.minecraft.world.entity.Mob mob
                        && isInvmodTarget(target)
                        && !mob.hasLineOfSight(target)) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (event.getEntity() instanceof Combatant<?>
                && event.getNewAboutToBeSetTarget() instanceof Combatant<?>) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    public static boolean isHiddenInternalTarget(LivingEntity target) {
        return isInvmodTarget(target)
                && (target instanceof SpawnProxyEntity
                        || target.isInvisible()
                        || !target.isAttackable());
    }

    public static boolean isInvmodTarget(LivingEntity target) {
        return target != null
                && BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())
                        .getNamespace().equals(InvasionMod.MOD_ID);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Combatant<?> && attacker instanceof Combatant<?>) {
            event.setCanceled(true);
        }
    }
}
