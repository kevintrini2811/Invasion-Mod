package com.invasion.mixin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.UndeadRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnimationUtils.class)
public abstract class ZombieShieldPoseMixin {
    @Inject(method = "animateZombieArms", at = @At("HEAD"), cancellable = true)
    private static void invasion$preserveShieldPose(ModelPart leftArm, ModelPart rightArm,
            boolean aggressive, UndeadRenderState state, CallbackInfo ci) {
        if (state.leftArmPose == HumanoidModel.ArmPose.BLOCK
                || state.rightArmPose == HumanoidModel.ArmPose.BLOCK) {
            ci.cancel();
        }
    }
}
