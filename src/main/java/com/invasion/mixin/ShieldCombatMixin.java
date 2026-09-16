package com.invasion.mixin;

import com.invasion.entity.ShieldUseHandler;
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
    @Inject(method = "m_8119_", at = @At("TAIL"), remap = false)
    private void invasion$tickShieldDefense(CallbackInfo ci) {
        if ((Object)this instanceof net.minecraft.world.entity.Mob mob) ShieldUseHandler.update(mob);
    }

    @Inject(method = "m_6469_", at = @At("HEAD"), cancellable = true, remap = false)
    private void invasion$prepareShieldDefense(DamageSource source, float damage,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ShieldUseHandler.onIncomingDamage((LivingEntity)(Object)this, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "m_21011_(Lnet/minecraft/world/InteractionHand;Z)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void invasion$lowerShield(InteractionHand hand, boolean sendToSelf, CallbackInfo ci) {
        if (!ShieldUseHandler.onAttack((LivingEntity)(Object)this, hand)) ci.cancel();
    }

    @Inject(method = "m_21275_", at = @At("HEAD"), cancellable = true, remap = false)
    private void invasion$enforceShieldCooldown(DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        if (ShieldUseHandler.isBlockingSuppressed((LivingEntity)(Object)this)) cir.setReturnValue(false);
    }

    @Inject(method = "m_21275_", at = @At("RETURN"), remap = false)
    private void invasion$countSuccessfulBlock(DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) ShieldUseHandler.onSuccessfulBlock((LivingEntity)(Object)this, 1.0F);
    }
}
