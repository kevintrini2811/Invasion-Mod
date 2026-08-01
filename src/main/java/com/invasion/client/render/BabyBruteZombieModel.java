package com.invasion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;

public final class BabyBruteZombieModel
        extends ZombieModel<InvasionZombieRenderState> {
    public BabyBruteZombieModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
                PartNames.HEAD,
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 9.0F, 2.0F));
        head.addOrReplaceChild(
                "head_cube",
                CubeListBuilder.create().texOffs(1, 14).addBox(
                        -3.9554F, -6.0F, -4.0427F,
                        6.0F, 6.0F, 6.0F,
                        CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0436F, 0.0F));
        head.addOrReplaceChild(
                PartNames.HAT, CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild(
                PartNames.BODY,
                CubeListBuilder.create().texOffs(0, 0).addBox(
                        -6.0F, 0.0F, -3.0F,
                        10.0F, 8.0F, 5.0F,
                        CubeDeformation.NONE),
                PartPose.offset(0.0F, 9.0F, 2.0F));
        root.addOrReplaceChild(
                PartNames.RIGHT_LEG,
                CubeListBuilder.create().texOffs(26, 13).addBox(
                        -2.0F, 0.0F, -2.5F,
                        4.0F, 7.0F, 5.0F,
                        CubeDeformation.NONE),
                PartPose.offset(-3.0F, 17.0F, 1.5F));
        root.addOrReplaceChild(
                PartNames.LEFT_LEG,
                CubeListBuilder.create().texOffs(26, 25).addBox(
                        -2.0F, 0.0F, -2.5F,
                        4.0F, 7.0F, 5.0F,
                        CubeDeformation.NONE),
                PartPose.offset(1.0F, 17.0F, 1.5F));
        root.addOrReplaceChild(
                PartNames.RIGHT_ARM,
                CubeListBuilder.create().texOffs(0, 27).addBox(
                        -3.0F, 0.0F, -2.5F,
                        3.0F, 9.0F, 5.0F,
                        CubeDeformation.NONE),
                PartPose.offset(-6.0F, 10.0F, 1.5F));
        root.addOrReplaceChild(
                PartNames.LEFT_ARM,
                CubeListBuilder.create().texOffs(16, 37).addBox(
                        0.0F, 0.0F, -2.5F,
                        3.0F, 9.0F, 5.0F,
                        CubeDeformation.NONE),
                PartPose.offset(4.0F, 10.0F, 1.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void translateToHand(
            HumanoidRenderState state,
            HumanoidArm arm,
            PoseStack poseStack) {
        super.translateToHand(state, arm, poseStack);
        poseStack.translate(
                arm == HumanoidArm.RIGHT ? -0.0625F : 0.0625F,
                0.125F,
                0.0F);
    }
}
