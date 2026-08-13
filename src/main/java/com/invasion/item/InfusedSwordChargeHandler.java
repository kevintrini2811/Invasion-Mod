package com.invasion.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class InfusedSwordChargeHandler {
    private InfusedSwordChargeHandler() { }

    public static void bootstrap() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(InfusedSwordChargeHandler::beforeDamage);
    }

    private static boolean beforeDamage(LivingEntity victim, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker
                && source.getDirectEntity() == attacker) {
            InfusedSwordItem.addDamageCharge(attacker.getMainHandItem(), amount);
        }
        return true;
    }
}
