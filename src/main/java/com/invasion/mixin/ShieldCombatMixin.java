package com.invasion.mixin;

import com.invasion.entity.ShieldUseHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ShieldCombatMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"), cancellable = true)
    private void invasion$lowerShield(InteractionHand hand, SwingAnimation animation,
            boolean sendToSelf, CallbackInfoReturnable<Boolean> cir) {
        if (!ShieldUseHandler.onAttack((LivingEntity)(Object)this, hand)) cir.setReturnValue(false);
    }

    @Inject(method = "applyItemBlocking", at = @At("HEAD"), cancellable = true)
    private void invasion$enforceShieldCooldown(ServerLevel level, DamageSource source, float damage,
            CallbackInfoReturnable<Float> cir) {
        if (ShieldUseHandler.isBlockingSuppressed((LivingEntity)(Object)this)) cir.setReturnValue(0.0F);
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void invasion$countSuccessfulBlock(ServerLevel level, DamageSource source, float damage,
            CallbackInfoReturnable<Float> cir) {
        ShieldUseHandler.onSuccessfulBlock((LivingEntity)(Object)this, cir.getReturnValue());
    }
}
