package com.invasion.client.render;

import com.invasion.entity.IMEndermanEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.ResourceLocation;

public final class IMEndermanRenderer
        extends HumanoidMobRenderer<IMEndermanEntity, EndermanRenderState, EndermanModel<EndermanRenderState>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/enderman/enderman.png");
    private final BlockModelResolver blockModelResolver;

    public IMEndermanRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
        blockModelResolver = context.getBlockModelResolver();
        addLayer(new EnderEyesLayer(this));
        addLayer(new CarriedBlockLayer(this));
        addLayer(new HeadArmorLayer<>(
                this, context, state -> state.headEquipment));
    }

    @Override
    public EndermanRenderState createRenderState() {
        return new EndermanRenderState();
    }

    @Override
    public void extractRenderState(IMEndermanEntity entity, EndermanRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isCreepy = entity.isAggressive();
        entity.getCarriedBlock().ifPresentOrElse(
                block -> blockModelResolver.update(state.carriedBlock, block, EndermanRenderer.BLOCK_DISPLAY_CONTEXT),
                state.carriedBlock::clear);
    }

    @Override
    public ResourceLocation getTextureLocation(EndermanRenderState state) {
        return TEXTURE;
    }
}
