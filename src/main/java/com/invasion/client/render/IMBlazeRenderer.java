package com.invasion.client.render;

import com.invasion.entity.IMBlazeEntity;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;

public final class IMBlazeRenderer extends BlazeRenderer {
    private static final float HELMET_Y_OFFSET = 2.8F;
    private static final float HELMET_SCALE = 1.05F;

    public IMBlazeRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new HeadArmorLayer<LivingEntityRenderState, BlazeModel>(
                this, context,
                state -> state instanceof InvasionBlazeRenderState blazeState
                        ? blazeState.headEquipment
                        : ItemStack.EMPTY,
                HELMET_Y_OFFSET, 0.0F, HELMET_SCALE));
    }

    @Override
    public InvasionBlazeRenderState createRenderState() {
        return new InvasionBlazeRenderState();
    }

    @Override
    public void extractRenderState(
            Blaze entity, LivingEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity instanceof IMBlazeEntity
                && state instanceof InvasionBlazeRenderState blazeState) {
            blazeState.headEquipment = entity.getItemBySlot(EquipmentSlot.HEAD);
        }
    }
}
