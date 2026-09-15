package com.invasion.mixin;

import com.invasion.client.render.ShieldPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererShieldPoseMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void invasion$applyShieldPose(LivingEntity entity, LivingEntityRenderState state,
            float partialTick, CallbackInfo ci) {
        if (state instanceof ArmedEntityRenderState armed) ShieldPose.apply(entity, armed);
    }
}
