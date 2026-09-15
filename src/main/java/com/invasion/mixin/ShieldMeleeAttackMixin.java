package com.invasion.mixin;

import com.invasion.entity.ShieldUseHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeleeAttackGoal.class)
public abstract class ShieldMeleeAttackMixin {
    @Shadow @Final protected PathfinderMob mob;

    @Inject(method = "canPerformAttack", at = @At("RETURN"), cancellable = true)
    private void invasion$waitForShieldWindup(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !ShieldUseHandler.prepareAttack(mob)) cir.setReturnValue(false);
    }
}
