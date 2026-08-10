package com.invasion.client.render;

import com.invasion.entity.IMGhastEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.ghast.GhastModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

public final class IMGhastRenderer
        extends MobRenderer<IMGhastEntity, InvasionGhastRenderState, GhastModel> {
    // The vanilla Ghast body is a 16-unit cube enlarged by its 4.5 model
    // transformer. A scale of nine would cover it exactly; slightly reducing
    // that value keeps the helmet from overwhelming the Ghast's face.
    private static final float HELMET_SCALE = 8.1F;
    private static final float HELMET_Y_OFFSET = 9.36F;
    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/ghast/ghast.png");
    private static final Identifier SHOOTING_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/ghast/ghast_shooting.png");

    public IMGhastRenderer(EntityRendererProvider.Context context) {
        super(context, new GhastModel(context.bakeLayer(ModelLayers.GHAST)), 1.5F);
        addLayer(new HeadArmorLayer<>(
                this, context, state -> state.headEquipment,
                "body", HELMET_Y_OFFSET, 0.0F, HELMET_SCALE));
    }

    @Override
    public InvasionGhastRenderState createRenderState() {
        return new InvasionGhastRenderState();
    }

    @Override
    public void extractRenderState(
            IMGhastEntity entity, InvasionGhastRenderState state,
            float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isCharging = entity.isCharging();
        state.headEquipment = entity.getItemBySlot(EquipmentSlot.HEAD);
    }

    @Override
    public Identifier getTextureLocation(InvasionGhastRenderState state) {
        return state.isCharging ? SHOOTING_TEXTURE : TEXTURE;
    }
}
