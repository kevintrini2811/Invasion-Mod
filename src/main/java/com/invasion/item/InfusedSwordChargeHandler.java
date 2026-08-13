package com.invasion.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class InfusedSwordChargeHandler {
    private InfusedSwordChargeHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(InfusedSwordChargeHandler::afterDamage);
    }

    private static void afterDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        InfusedSwordItem.addDamageCharge(weapon, event.getInflictedDamage());
    }
}
