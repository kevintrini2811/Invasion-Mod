package com.invasion.mixin;

import com.invasion.entity.VanillaMobSpawnReplacement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
abstract class NaturalMobSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void invmod$blockNaturalSpawn(
            ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData spawnData,
            CallbackInfoReturnable<SpawnGroupData> callback) {
        Mob mob = (Mob) (Object) this;
        if (reason == EntitySpawnReason.NATURAL
                && level.getLevel() instanceof ServerLevel serverLevel
                && VanillaMobSpawnReplacement.shouldBlockNaturalSpawn(mob, serverLevel)) {
            mob.discard();
        }
    }
}
