package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.BurrowerEntity;
import com.invasion.util.math.PosRotate3D;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class BurrowerRenderer
        extends MobRenderer<BurrowerEntity, BurrowerRenderState, BurrowerModel> {
    private static final ResourceLocation TEXTURE =
            InvasionMod.id("textures/entity/burrower.png");

    public BurrowerRenderer(EntityRendererProvider.Context context) {
        super(context, new BurrowerModel(BurrowerModel.createBodyLayer().bakeRoot()), 0.45F);
    }

    @Override
    public BurrowerRenderState createRenderState() {
        return new BurrowerRenderState();
    }

    @Override
    public void extractRenderState(BurrowerEntity entity, BurrowerRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.hasTrackedSegments = false;
        state.segments[0] = new PosRotate3D(
                entity.position(),
                PosRotate3D.lerp(tickDelta, entity.getPrevRotation(), entity.getRotation(), new Vector3f()));
        for (int i = 0; i < BurrowerEntity.NUMBER_OF_SEGMENTS; i++) {
            state.segments[i + 1] = entity.getSegments3DLastTick()[i]
                    .lerp(tickDelta, entity.getSegments3D()[i]);
            state.hasTrackedSegments |= state.segments[i + 1].position().lengthSqr() > 1.0E-6D;
        }
        if (state.hasTrackedSegments) {
            state.segments[0] = new PosRotate3D(
                    entity.position(),
                    state.segments[1].rotation());
        }
    }

    @Override
    protected void scale(BurrowerRenderState state, PoseStack poseStack) {
        poseStack.scale(2.2F, 2.2F, 2.2F);
        // LivingEntityRenderer applies this offset after scale(). Vanilla
        // models are authored around y=24, while the legacy Burrower uses
        // independently positioned world-space segments around the origin.
        poseStack.translate(0.0F, 1.501F, 0.0F);
    }

    @Override
    protected void setupRotations(BurrowerRenderState state, PoseStack poseStack,
            float bodyRotation, float scale) {
        // Every segment already contains its complete world-space rotation.
        // Applying the normal mob body yaw would rotate the whole chain again.
    }

    @Override
    public ResourceLocation getTextureLocation(BurrowerRenderState state) {
        return TEXTURE;
    }
}
