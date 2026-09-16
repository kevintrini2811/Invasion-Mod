package com.invasion.mixin;

import com.invasion.entity.ShieldUseHandler;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ShieldCombatMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void invasion$lowerShield(InteractionHand hand, boolean sendToSelf, CallbackInfo ci) {
        if (!ShieldUseHandler.onAttack((LivingEntity)(Object)this, hand)) ci.cancel();
    }

    @Inject(method = "isDamageSourceBlocked", at = @At("HEAD"), cancellable = true)
    private void invasion$enforceShieldCooldown(DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        if (ShieldUseHandler.isBlockingSuppressed((LivingEntity)(Object)this)) cir.setReturnValue(false);
    }

    @Inject(method = "isDamageSourceBlocked", at = @At("RETURN"))
    private void invasion$countSuccessfulBlock(DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) ShieldUseHandler.onSuccessfulBlock((LivingEntity)(Object)this, 1.0F);
    }

    // Older hurt implementations still apply damage knockback after a full shield block.
    // Capture the result of this hit, including the block that lowers the shield for cooldown.
    @WrapWithCondition(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"), require = 1)
    private boolean invasion$skipBlockedDamageKnockback(LivingEntity defender,
            double strength, double x, double z, @Local(ordinal = 0) boolean fullyBlocked) {
        return !fullyBlocked || !ShieldUseHandler.isShieldMob(defender);
    }

    @WrapWithCondition(method = "blockedByShield", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"), require = 1)
    private boolean invasion$skipShieldRecoil(LivingEntity defender, double strength, double x, double z) {
        return !ShieldUseHandler.isShieldMob(defender);
    }
}
