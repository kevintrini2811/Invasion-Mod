package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Renders all six player-skin overlay regions with slime-like translucency. */
final class MysteryZombieOuterLayer extends RenderLayer<
        InvasionZombieRenderState,
        HumanoidModel<InvasionZombieRenderState>> {
    private static final Identifier TEXTURE = InvasionMod.id(
            "textures/entity/zombie/mystery_zombie.png");
    private final ZombieModel<InvasionZombieRenderState> adultModel;
    private final ZombieModel<InvasionZombieRenderState> babyModel;

    MysteryZombieOuterLayer(RenderLayerParent<
            InvasionZombieRenderState,
            HumanoidModel<InvasionZombieRenderState>> renderer) {
        super(renderer);
        MeshDefinition mesh = createOuterMesh();
        adultModel = new ZombieModel<>(
                LayerDefinition.create(mesh, 64, 64).bakeRoot());
        babyModel = new ZombieModel<>(LayerDefinition.create(
                HumanoidModel.BABY_TRANSFORMER.apply(mesh),
                64, 64).bakeRoot());
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            InvasionZombieRenderState state,
            float yRot,
            float xRot) {
        if (state.isInvisible) {
            return;
        }
        ZombieModel<InvasionZombieRenderState> model =
                state.isBaby ? babyModel : adultModel;
        collector.order(1).submitModel(
                model,
                state,
                poseStack,
                RenderTypes.entityTranslucent(TEXTURE),
                lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                -1,
                null,
                state.outlineColor,
                null);
    }

    private static MeshDefinition createOuterMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation headInflation = new CubeDeformation(0.5F);
        CubeDeformation bodyInflation = new CubeDeformation(0.25F);
        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(32, 0).addBox(
                        -4.0F, -8.0F, -4.0F,
                        8.0F, 8.0F, 8.0F, headInflation),
                PartPose.ZERO).addOrReplaceChild(
                        "hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(16, 32).addBox(
                        -4.0F, 0.0F, -2.0F,
                        8.0F, 12.0F, 4.0F, bodyInflation),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 32).addBox(
                        -3.0F, -2.0F, -2.0F,
                        4.0F, 12.0F, 4.0F, bodyInflation),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(48, 48).addBox(
                        -1.0F, -2.0F, -2.0F,
                        4.0F, 12.0F, 4.0F, bodyInflation),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(0, 32).addBox(
                        -2.0F, 0.0F, -2.0F,
                        4.0F, 12.0F, 4.0F, bodyInflation),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(0, 48).addBox(
                        -2.0F, 0.0F, -2.0F,
                        4.0F, 12.0F, 4.0F, bodyInflation),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return mesh;
    }
}
