package com.invasion.mixin;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Phantom.class)
public interface PhantomAccessor {
    @Accessor(value = "f_33097_", remap = false)
    void invasion$setMoveTargetPoint(Vec3 target);
}
