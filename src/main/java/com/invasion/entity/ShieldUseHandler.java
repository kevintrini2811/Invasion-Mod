package com.invasion.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Makes shield-equipped invasion mobs actively block during combat. */
public final class ShieldUseHandler {
    private static final double BLOCK_RANGE_SQUARED = 16.0D * 16.0D;

    private ShieldUseHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(ShieldUseHandler::tick);
    }

    private static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob)
                || mob.level().isClientSide()
                || !mob.getPersistentData().contains("invmodWaveNumber")) {
            return;
        }

        if (shouldBlock(mob)) {
            if (!mob.isUsingItem()) {
                mob.startUsingItem(InteractionHand.OFF_HAND);
            }
        } else if (mob.isUsingItem()
                && mob.getUsedItemHand() == InteractionHand.OFF_HAND
                && EquipmentUtil.isShield(mob.getUseItem())) {
            mob.stopUsingItem();
        }
    }

    static boolean shouldBlock(Mob mob) {
        LivingEntity target = mob.getTarget();
        return EquipmentUtil.isMeleeWeapon(mob.getMainHandItem())
                && EquipmentUtil.isShield(mob.getOffhandItem())
                && target != null
                && target.isAlive()
                && mob.distanceToSqr(target) <= BLOCK_RANGE_SQUARED
                && mob.hasLineOfSight(target);
    }
}
