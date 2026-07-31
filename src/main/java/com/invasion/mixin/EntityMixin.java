package com.invasion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.invasion.nexus.Combatant;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void invasion_after_setRemoved(Entity.RemovalReason reason, CallbackInfo info) {
        if (this instanceof Combatant self && self.hasNexus()) {
            self.getNexus().notifyCombatantRemoved(self, reason);
        }
    }
}
