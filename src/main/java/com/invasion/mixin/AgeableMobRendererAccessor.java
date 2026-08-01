package com.invasion.mixin;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AgeableMobRenderer.class)
public interface AgeableMobRendererAccessor {
    @Accessor("adultModel")
    @Mutable
    void invasion$setAdultModel(EntityModel<?> model);

    @Accessor("babyModel")
    @Mutable
    void invasion$setBabyModel(EntityModel<?> model);
}
