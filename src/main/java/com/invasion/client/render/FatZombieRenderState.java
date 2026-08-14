package com.invasion.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class FatZombieRenderState extends LivingEntityRenderState {
    public float growthScale = 1.0F;
    public float eatAnimation;
    public boolean eating;
}
