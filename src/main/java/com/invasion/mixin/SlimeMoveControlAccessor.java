package com.invasion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeMoveControl")
public interface SlimeMoveControlAccessor {
    @Invoker("setDirection")
    void invasion$setDirection(float direction, boolean aggressive);

    @Invoker("setWantedMovement")
    void invasion$setWantedMovement(double speed);
}
