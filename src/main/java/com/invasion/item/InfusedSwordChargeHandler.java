package com.invasion.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

public final class InfusedSwordChargeHandler {
    private InfusedSwordChargeHandler() { }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(InfusedSwordChargeHandler::beforeDamage);
    }

    private static void beforeDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker) {
            InfusedSwordItem.addDamageCharge(attacker.getMainHandItem(), event.getAmount());
        }
    }
}
