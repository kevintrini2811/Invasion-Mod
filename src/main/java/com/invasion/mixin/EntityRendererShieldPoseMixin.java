package com.invasion.mixin;

import com.invasion.client.render.ShieldPose;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererShieldPoseMixin {
    // Apply after the complete renderer chain has finished assigning arm poses.
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN"), require = 1)
    private void invasion$applyShieldPose(Entity entity, float partialTick,
            CallbackInfoReturnable<EntityRenderState> cir) {
        if (entity instanceof Mob mob && cir.getReturnValue() instanceof ArmedEntityRenderState armed) {
            ShieldPose.apply(mob, armed);
        }
    }
}
