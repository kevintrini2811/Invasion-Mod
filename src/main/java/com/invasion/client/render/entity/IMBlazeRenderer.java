package com.invasion.client.render.entity;

import com.invasion.entity.IMBlazeEntity;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class IMBlazeRenderer extends BlazeRenderer {
    private static final float HELMET_Y_OFFSET = -0.5F;
    private static final float HELMET_SCALE = 1.157625F;

    public IMBlazeRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new MobHeadArmorLayer<>(this, context,
                blaze -> getModel().root().getChild("head"),
                HELMET_SCALE, HELMET_Y_OFFSET, 0.0F));
    }
}
