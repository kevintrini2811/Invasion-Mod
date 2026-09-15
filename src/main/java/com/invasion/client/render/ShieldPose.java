package com.invasion.client.render;

import com.invasion.entity.EquipmentUtil;
import com.invasion.entity.ImpEnitty;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

public final class ShieldPose {
    private ShieldPose() {
    }

    public static void apply(LivingEntity entity, ArmedEntityRenderState state) {
        if (entity instanceof ImpEnitty) {
            state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        }
        if (!entity.isUsingItem() || entity.getUsedItemHand() != InteractionHand.OFF_HAND
                || !EquipmentUtil.isShield(entity.getUseItem())) {
            return;
        }
        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            state.leftArmPose = HumanoidModel.ArmPose.BLOCK;
        } else {
            state.rightArmPose = HumanoidModel.ArmPose.BLOCK;
        }
        if (state instanceof SkeletonRenderState skeleton) {
            // Skeleton attack animation otherwise overwrites both arm poses.
            skeleton.isAggressive = false;
        }
    }
}
