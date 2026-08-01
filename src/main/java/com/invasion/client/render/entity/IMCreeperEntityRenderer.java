package com.invasion.client.render.entity;

import com.invasion.entity.IMCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Copy of CreeperEntityRenderer modified to use different textures
 *
 * @see net.minecraft.client.renderer.entity.CreeperRenderer
 */
public class IMCreeperEntityRenderer extends LivingEntityRenderer<IMCreeperEntity, CreeperModel<IMCreeperEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png");

    public IMCreeperEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        addLayer(new MobHeadArmorLayer<>(this, context,
                creeper -> getModel().root().getChild("head"),
                1.3F, -1.0F, 0.0F));
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
    protected float getWhiteOverlayProgress(IMCreeperEntity creeperEntity, float tickDelta) {
        float fuseTime = creeperEntity.getClientFuseTime(tickDelta);
        return (int)(fuseTime * 10) % 2 == 0 ? 0 : Mth.clamp(fuseTime, 0.5F, 1);
    }

    @Override
    public ResourceLocation getTextureLocation(IMCreeperEntity creeperEntity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(IMCreeperEntity entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }

}
