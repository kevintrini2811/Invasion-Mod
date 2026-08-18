package com.invasion.mixin;

import com.invasion.entity.SpawnProxyEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class SpawnProxyEntityMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void invasion$replaceSpawnProxy(
            Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!(entity instanceof SpawnProxyEntity proxy)) {
            return;
        }
        ServerLevel world = (ServerLevel) (Object) this;
        SpawnProxyEntity.generateMobGroup(world, replacement -> {
            replacement.absSnapTo(proxy.getX(), proxy.getY(), proxy.getZ(),
                    proxy.getYRot(), proxy.getXRot());
            world.addFreshEntity(replacement);
        });
        callback.setReturnValue(false);
    }
}
