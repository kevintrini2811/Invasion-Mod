package com.invasion.client.render.legacy;

import com.invasion.entity.EquipmentUtil;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Preserves the native humanoid blocking pose while a mob uses its offhand shield. */
public final class ShieldPose {
    private ShieldPose() {
    }

    public static boolean isBlocking(LivingEntity entity) {
        return entity instanceof Mob && entity.isUsingItem()
                && entity.getUsedItemHand() == InteractionHand.OFF_HAND
                && EquipmentUtil.isShield(entity.getUseItem());
    }

    public static HumanoidModel.ArmPose armPose(LivingEntity entity, HumanoidArm arm,
            HumanoidModel.ArmPose original) {
        return isBlocking(entity) && arm != entity.getMainArm()
                ? HumanoidModel.ArmPose.BLOCK : original;
    }
}
