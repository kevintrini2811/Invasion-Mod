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
    @Shadow(remap = false) @Final protected PathfinderMob f_25540_;

    @Inject(method = "m_25564_", at = @At("RETURN"), cancellable = true, remap = false)
    private void invasion$waitForShieldWindup(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !ShieldUseHandler.prepareAttack(f_25540_)) cir.setReturnValue(false);
    }
}
