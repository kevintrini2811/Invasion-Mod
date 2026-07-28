package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.ImpEnitty;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;

public final class ImpRenderer
        extends MobRenderer<ImpEnitty, ArmedEntityRenderState, ImpModel> {
    private static final Identifier TEXTURE = InvasionMod.id("textures/entity/imp.png");
    private final ItemModelResolver itemModelResolver;

    public ImpRenderer(EntityRendererProvider.Context context) {
        super(context, new ImpModel(ImpModel.createBodyLayer().bakeRoot()), 0.3F);
        itemModelResolver = context.getItemModelResolver();
        addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public ArmedEntityRenderState createRenderState() {
        return new ArmedEntityRenderState();
    }

    @Override
    public void extractRenderState(
            ImpEnitty entity, ArmedEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        ArmedEntityRenderState.extractArmedEntityRenderState(
                entity, state, itemModelResolver, tickDelta);
    }

    @Override
    public Identifier getTextureLocation(ArmedEntityRenderState state) {
        return TEXTURE;
    }
}
