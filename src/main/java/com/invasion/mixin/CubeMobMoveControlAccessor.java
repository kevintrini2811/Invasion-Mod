package com.invasion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Access to the native jump steering shared by slime and magma cube variants. */
@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobMoveControl")
public interface CubeMobMoveControlAccessor {
    @Invoker("setDirection")
    void invasion$setDirection(float direction, boolean aggressive);

    @Invoker("setWantedMovement")
    void invasion$setWantedMovement(double speed);
}
