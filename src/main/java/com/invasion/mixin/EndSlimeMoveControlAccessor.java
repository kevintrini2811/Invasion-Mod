package com.invasion.mixin;

import com.invasion.compat.NexusJumpControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Optional integration: retain the mod's own jump timing and movement. */
@Pseudo
@Mixin(targets = "org.betterx.betterend.entity.EndSlimeEntity$EndSlimeMoveControl", remap = false)
public interface EndSlimeMoveControlAccessor extends NexusJumpControl {
    @Override
    @Invoker(value = "look", remap = false)
    void invasion$setDirection(float direction, boolean aggressive);

    @Override
    @Invoker(value = "move", remap = false)
    void invasion$setWantedMovement(double speed);
}
