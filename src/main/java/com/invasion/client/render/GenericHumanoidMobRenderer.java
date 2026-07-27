package com.invasion.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Temporary 26.2-compatible renderer used until the original bespoke entity
 * models have been migrated to the render-state API.
 */
public final class GenericHumanoidMobRenderer<T extends Mob>
        extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private final Identifier texture;

    public GenericHumanoidMobRenderer(EntityRendererProvider.Context context, Identifier texture, float shadowRadius) {
        super(
                context,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
                shadowRadius);
        this.texture = texture;
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return texture;
    }
}
