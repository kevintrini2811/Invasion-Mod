package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.SpiderEggEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class SpiderEggEntityRenderer
        extends EntityRenderer<SpiderEggEntity> {
    private static final ResourceLocation TEXTURE =
            InvasionMod.id("textures/entity/spider_egg.png");
    private final EggModel model = new EggModel(createLayer().bakeRoot());

    public SpiderEggEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.35F;
    }

    @Override
    public void render(SpiderEggEntity entity, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(
                pose, vertices, light, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiderEggEntity entity) {
        return TEXTURE;
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition egg = mesh.getRoot().addOrReplaceChild(
                "egg", CubeListBuilder.create(),
                PartPose.offset(-4.5F, 0.0F, -4.5F));
        egg.addOrReplaceChild("top", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(1, 0, 1, 7, 1, 7)
                .texOffs(0, 8).addBox(2, -11, 2, 5, 1, 5)
                .texOffs(28, 23).addBox(1, -10, 2, 1, 3, 6)
                .texOffs(0, 24).addBox(1, -10, 1, 6, 3, 1)
                .texOffs(28, 23).addBox(7, -10, 1, 1, 3, 6)
                .texOffs(0, 24).addBox(2, -10, 7, 6, 3, 1)
                .texOffs(10, 22).addBox(0, -7, 1, 1, 2, 8)
                .texOffs(0, 21).addBox(0, -7, 0, 8, 2, 1)
                .texOffs(10, 22).addBox(8, -7, 0, 1, 2, 8)
                .texOffs(0, 21).addBox(1, -7, 8, 8, 2, 1)
                .texOffs(20, 10).addBox(-1, -5, 0, 1, 4, 9)
                .texOffs(0, 16).addBox(0, -5, -1, 9, 4, 1)
                .texOffs(20, 10).addBox(9, -5, 0, 1, 4, 9)
                .texOffs(0, 16).addBox(0, -5, 9, 9, 4, 1)
                .texOffs(28, 0).addBox(0, -1, 1, 1, 1, 8)
                .texOffs(0, 14).addBox(0, -1, 0, 8, 1, 1)
                .texOffs(28, 0).addBox(8, -1, 0, 1, 1, 8), PartPose.ZERO);
        egg.addOrReplaceChild("bottom",
                CubeListBuilder.create().mirror().texOffs(0, 14)
                        .addBox(0, 0, 0, 8, 1, 1),
                PartPose.offset(1, -1, 8));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static final class EggModel
            extends HierarchicalModel<SpiderEggEntity> {
        private final ModelPart root;

        EggModel(ModelPart root) {
            this.root = root;
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(SpiderEggEntity entity, float limbSwing,
                float limbSwingAmount, float age, float headYaw,
                float headPitch) {
        }
    }
}
