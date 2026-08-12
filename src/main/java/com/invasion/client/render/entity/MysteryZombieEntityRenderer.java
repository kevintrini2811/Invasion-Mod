package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.AbstractIMZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class MysteryZombieEntityRenderer
        extends AbstractIMZombieEntityRenderer {
    private static final ResourceLocation TEXTURE = InvasionMod.id(
            "textures/entity/zombie/mystery_zombie.png");

    public MysteryZombieEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        normalModel.hat.visible = false;
        addLayer(new OuterLayer(this));
    }

    @Override
    protected boolean isBrute(AbstractIMZombieEntity entity) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractIMZombieEntity entity) {
        return TEXTURE;
    }

    private static final class OuterLayer extends RenderLayer<
            AbstractIMZombieEntity,
            HumanoidModel<AbstractIMZombieEntity>> {
        private final HumanoidModel<AbstractIMZombieEntity> model =
                new OuterModel(LayerDefinition.create(
                        createOuterMesh(), 64, 64).bakeRoot());

        OuterLayer(MysteryZombieEntityRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers,
                int light, AbstractIMZombieEntity entity,
                float limbSwing, float limbSwingAmount, float partialTick,
                float age, float headYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            getParentModel().copyPropertiesTo(model);
            model.prepareMobModel(
                    entity, limbSwing, limbSwingAmount, partialTick);
            model.setupAnim(entity, limbSwing, limbSwingAmount,
                    age, headYaw, headPitch);
            model.renderToBuffer(
                    pose,
                    buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                    light,
                    OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static final class OuterModel
            extends HumanoidModel<AbstractIMZombieEntity> {
        OuterModel(net.minecraft.client.model.geom.ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(AbstractIMZombieEntity entity, float limbSwing,
                float limbSwingAmount, float age, float headYaw,
                float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount,
                    age, headYaw, headPitch);
            AnimationUtils.animateZombieArms(
                    leftArm, rightArm, entity.isAggressive(),
                    attackTime, age);
        }
    }

    private static MeshDefinition createOuterMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation head = new CubeDeformation(0.5F);
        CubeDeformation body = new CubeDeformation(0.25F);
        root.addOrReplaceChild(
                "head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4, -8, -4, 8, 8, 8, head), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 32)
                .addBox(-4, 0, -2, 8, 12, 4, body), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 32)
                .addBox(-3, -2, -2, 4, 12, 4, body), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 48)
                .addBox(-1, -2, -2, 4, 12, 4, body), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-2, 0, -2, 4, 12, 4, body), PartPose.offset(-1.9F, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 48)
                .addBox(-2, 0, -2, 4, 12, 4, body), PartPose.offset(1.9F, 12, 0));
        return mesh;
    }
}
