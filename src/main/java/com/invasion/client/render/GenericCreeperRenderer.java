package com.invasion.client.render;

import com.invasion.entity.IMCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

public final class GenericCreeperRenderer
        extends MobRenderer<IMCreeperEntity, InvasionCreeperRenderState, CreeperModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/creeper/creeper.png");

    @SuppressWarnings({"rawtypes", "unchecked"})
    public GenericCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        addLayer((net.minecraft.client.renderer.entity.layers.RenderLayer)
                new CreeperPowerLayer(
                        (net.minecraft.client.renderer.entity.RenderLayerParent) this,
                        context.getModelSet()));
        addLayer(new HeadArmorLayer<>(
                this, context, state -> state.headEquipment));
    }

    @Override
    public InvasionCreeperRenderState createRenderState() {
        return new InvasionCreeperRenderState();
    }

    @Override
    public void extractRenderState(IMCreeperEntity entity, InvasionCreeperRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.swelling = entity.getClientFuseTime(tickDelta);
        state.isPowered = entity.isPowered();
        state.headEquipment = entity.getItemBySlot(EquipmentSlot.HEAD);
    }

    @Override
    protected void scale(InvasionCreeperRenderState state, PoseStack poseStack) {
        float swelling = state.swelling;
        float pulse = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
        swelling = Mth.clamp(swelling, 0.0F, 1.0F);
        swelling *= swelling;
        swelling *= swelling;
        float horizontal = (1.0F + swelling * 0.4F) * pulse;
        float vertical = (1.0F + swelling * 0.1F) / pulse;
        poseStack.scale(horizontal, vertical, horizontal);
    }

    @Override
    public ResourceLocation getTextureLocation(InvasionCreeperRenderState state) {
        return TEXTURE;
    }
}
