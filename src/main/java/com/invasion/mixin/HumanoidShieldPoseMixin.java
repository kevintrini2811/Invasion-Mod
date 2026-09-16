package com.invasion.mixin;

import com.invasion.client.render.legacy.ShieldPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HumanoidModel.class)
public abstract class HumanoidShieldPoseMixin {
    // Resolve poses per call without leaving BLOCK on a model shared by multiple mobs.
    @Redirect(method = "m_102878_", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/model/HumanoidModel;f_102815_:Lnet/minecraft/client/model/HumanoidModel$ArmPose;"),
            require = 1, remap = false)
    private HumanoidModel.ArmPose invasion$leftShieldPose(HumanoidModel<?> model, LivingEntity entity) {
        return ShieldPose.armPose(entity, HumanoidArm.LEFT, model.leftArmPose);
    }

    @Redirect(method = "m_102875_", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/model/HumanoidModel;f_102816_:Lnet/minecraft/client/model/HumanoidModel$ArmPose;"),
            require = 1, remap = false)
    private HumanoidModel.ArmPose invasion$rightShieldPose(HumanoidModel<?> model, LivingEntity entity) {
        return ShieldPose.armPose(entity, HumanoidArm.RIGHT, model.rightArmPose);
    }
}
