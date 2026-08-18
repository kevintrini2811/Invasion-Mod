package com.invasion.mixin;

import com.invasion.entity.SpawnProxyEntity;
import com.invasion.entity.IMMobFriendlyFireHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps internal helper entities out of goal- and brain-based combat AI. */
@Mixin({Mob.class, Brain.class})
abstract class NonCombatTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true,
            require = 0)
    private void invasion_preventHelperMobTarget(
            LivingEntity target, CallbackInfo info) {
        if (!IMMobFriendlyFireHandler.allowTarget((Mob) (Object) this, target)) {
            info.cancel();
        }
    }

    @Inject(method = "setMemoryInternal(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;J)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private <U> void invasion_preventHelperBrainTarget(
            MemoryModuleType<U> type, U value, long expiry,
            CallbackInfo info) {
        if (type == MemoryModuleType.ATTACK_TARGET
                && value instanceof SpawnProxyEntity) {
            info.cancel();
        }
    }
}
