package com.invasion.mixin;

import com.invasion.InvasionMod;
import com.invasion.entity.IMSilverfishEntity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentEntitySectionManager.class)
abstract class SilverfishSpawnMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void invmod$blockSilverfishSpawn(EntityAccess entity, boolean loadedFromDisk,
            CallbackInfoReturnable<Boolean> callback) {
        if (!loadedFromDisk && entity instanceof IMSilverfishEntity
                && !InvasionMod.getConfig().enableSilverfish) {
            callback.setReturnValue(false);
        }
    }
}
