package com.invasion.mixin;

import com.invasion.client.render.legacy.ShieldPose;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractZombieModel.class, ZombieVillagerModel.class, SkeletonModel.class})
public abstract class UndeadShieldPoseMixin {
    @Inject(method = "setupAnim", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            shift = At.Shift.AFTER), cancellable = true, require = 1)
    private void invasion$preserveShieldPose(@Coerce LivingEntity entity, float limbSwing,
            float limbSwingAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (ShieldPose.isBlocking(entity)) ci.cancel();
    }
}
