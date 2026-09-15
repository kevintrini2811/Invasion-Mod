package com.invasion.mixin;

import com.invasion.entity.ShieldUseHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ShieldSwingMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void invasion$lowerShield(InteractionHand hand, boolean sendToSelf, CallbackInfo ci) {
        ShieldUseHandler.onAttack((LivingEntity)(Object)this, hand);
    }
}
