package com.invasion.mixin;

import com.invasion.nexus.Combatant;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "atomicstryker.infernalmobs.common.InfernalMobsCore", remap = false)
public abstract class InfernalMobsHealthMixin {
    @Inject(method = "getMobClassMaxHealth", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void invmod$useIndividualHealth(
            LivingEntity entity, CallbackInfoReturnable<Double> callback) {
        if (entity instanceof Combatant<?>) {
            // Wave tiers and reduced-health vanilla replacements share classes.
            // Infernal Mobs must not cache one instance's health for all of them.
            // Its modifier count and health factor still apply to this base.
            callback.setReturnValue((double) entity.getMaxHealth());
        }
    }
}
