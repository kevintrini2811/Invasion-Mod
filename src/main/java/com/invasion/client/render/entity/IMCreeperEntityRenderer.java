package com.invasion.client.render.entity;

import com.invasion.entity.IMCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Copy of CreeperEntityRenderer modified to use different textures
 *
 * @see net.minecraft.client.renderer.entity.CreeperRenderer
 */
public class IMCreeperEntityRenderer extends LivingEntityRenderer<IMCreeperEntity, CreeperModel<IMCreeperEntity>> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png");

	public IMCreeperEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new ChargeFeature(this, context.getModelSet()));
    }

    @Override
    protected void scale(IMCreeperEntity creeperEntity, PoseStack matrices, float tickDelta) {
        float fuseTime = creeperEntity.getClientFuseTime(tickDelta);
        float magnitude = 1 + Mth.sin(fuseTime * 100) * fuseTime * 0.01F;
        fuseTime = (float)Math.pow(Mth.clamp(fuseTime, 0, 1), 3);
        float horScale = (1 + fuseTime * 0.4F) * magnitude;
        float verScale = (1 + fuseTime * 0.1F) / magnitude;
        matrices.scale(horScale, verScale, horScale);
    }

    @Override
    protected float getAnimationCounter(IMCreeperEntity creeperEntity, float tickDelta) {
        float fuseTime = creeperEntity.getClientFuseTime(tickDelta);
        return (int)(fuseTime * 10) % 2 == 0 ? 0 : Mth.clamp(fuseTime, 0.5F, 1);
    }

    @Override
    public Identifier getTexture(IMCreeperEntity creeperEntity) {
        return TEXTURE;
    }

    /**
     * Copy of {@link CreeperPowerLayer}
     *
     * @see net.minecraft.client.renderer.entity.layers.CreeperPowerLayer
     */
    private static final class ChargeFeature extends EnergySwirlLayer<IMCreeperEntity, CreeperModel<IMCreeperEntity>> {
        private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
        private final CreeperModel<IMCreeperEntity> model;

        public ChargeFeature(RenderLayerParent<IMCreeperEntity, CreeperModel<IMCreeperEntity>> context, EntityModelSet loader) {
            super(context);
            model = new CreeperModel<>(loader.bakeLayer(ModelLayers.CREEPER_ARMOR));
        }

        @Override
        protected float xOffset(float partialAge) {
            return partialAge * 0.01F;
        }

        @Override
        protected Identifier getTextureLocation() {
            return SKIN;
        }

        @Override
        protected EntityModel<IMCreeperEntity> model() {
            return model;
        }
    }
}