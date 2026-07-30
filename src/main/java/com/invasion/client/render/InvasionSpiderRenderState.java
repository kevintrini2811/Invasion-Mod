package com.invasion.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;

public final class InvasionSpiderRenderState extends LivingEntityRenderState {
    public float invasionScale = 1.0F;
    public ItemStack headEquipment = ItemStack.EMPTY;
}
