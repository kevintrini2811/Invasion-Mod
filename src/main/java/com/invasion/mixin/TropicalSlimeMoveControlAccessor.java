package com.invasion.mixin;

import com.invasion.compat.NexusJumpControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Optional integration: retain the mod's own jump timing and movement. */
@Pseudo
@Mixin(targets = "baguchan.earthmobsmod.entity.TropicalSlime$SlimeMoveControl", remap = false)
public interface TropicalSlimeMoveControlAccessor extends NexusJumpControl {
    @Override
    @Invoker(value = "setDirection", remap = false)
    void invasion$setDirection(float direction, boolean aggressive);

    @Override
    @Invoker(value = "setWantedMovement", remap = false)
    void invasion$setWantedMovement(double speed);
}
