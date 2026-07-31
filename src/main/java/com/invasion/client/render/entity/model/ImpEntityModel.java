package com.invasion.client.render.entity.model;

import com.invasion.entity.ImpEnitty;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class ImpEntityModel extends HierarchicalModel<ImpEnitty>
        implements net.minecraft.client.model.ArmedModel {
    private final ModelPart root;

    private final ModelPart head;

    private final ModelPart rightArm;
    private final ModelPart leftArm;

    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    private final ModelPart rightShin;
    private final ModelPart leftShin;

    private final ModelPart rightFoot;
    private final ModelPart leftFoot;

    public ImpEntityModel(ModelPart root) {
        this.root = root;
        head = root.getChild(PartNames.HEAD);
        rightArm = root.getChild(PartNames.RIGHT_ARM);
        leftArm = root.getChild(PartNames.LEFT_ARM);
        rightLeg = root.getChild(PartNames.RIGHT_LEG);
        leftLeg = root.getChild(PartNames.LEFT_LEG);
        rightShin = root.getChild("right_shin");
        leftShin = root.getChild("left_shin");
        rightFoot = root.getChild("right_foot");
        leftFoot = root.getChild("left_foot");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create().texOffs(44, 0).addBox(-2.733333F, -3, -2, 5, 3, 4), PartPose.offsetAndRotation(-0.4F, 9.8F, -3.3F, 0.15807F, 0, 0))
            .addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(0, 0).addBox(0.6F, -4.5F, -1.0F, 1, 2, 1), PartPose.ZERO)
            .addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(0, 2).addBox(-1.6F, -4.5F, -1.0F, 1, 2, 1), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create().texOffs(23, 1).addBox(-4, 0, -4, 7, 4, 3), PartPose.offsetAndRotation(0, 9.1F, -0.8666667F, 0.64346F, 0, 0));
        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create().texOffs(26, 9).addBox(-2, -0.7333333F, -1.133333F, 2, 7, 2), PartPose.offset(-4, 10.8F, -2.066667F));
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create().texOffs(18, 9).addBox(0, -0.8666667F, -1, 2, 7, 2), PartPose.offset(3, 10.8F, -2.1F));
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create().texOffs(0, 17).addBox(-1, 0, -2, 2, 4, 3), PartPose.offsetAndRotation(-2, 16.9F, -1, -0.15807F, 0, 0));
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create().texOffs(0, 24).addBox(-1, 0, -2, 2, 4, 3), PartPose.offsetAndRotation(1, 17, -1, -0.15919F, 0, 0));
        root.addOrReplaceChild("right_shin", CubeListBuilder.create().texOffs(10, 17).addBox(-2, 0.6F, -4.4F, 2, 3, 2), PartPose.offsetAndRotation(-1, 16.9F, -1, 0.82623F, 0, 0));
        root.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(18, 18).addBox(-2, 4.2F, -1, 2, 3, 2), PartPose.offsetAndRotation(-1, 16.9F, -1, -0.01403F, 0, 0));
        root.addOrReplaceChild("left_shin", CubeListBuilder.create().texOffs(10, 22).addBox(-1, 0.6F, -4.433333F, 2, 3, 2), PartPose.offsetAndRotation(1, 17, -1, 0.82461F, 0, 0));
        root.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(10, 27).addBox(-1, 4.2F, -1, 2, 3, 2), PartPose.offsetAndRotation(1, 17, -1, -0.01214F, 0, 0));
        root.addOrReplaceChild("stomach", CubeListBuilder.create().texOffs(1, 1).addBox(0, 0, 0, 7, 5, 3), PartPose.offsetAndRotation(-4, 12.46667F, -2.266667F, -0.15807F, 0, 0));
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(44, 7).addBox(0, 0, 0, 3, 2, 2), PartPose.offsetAndRotation(-2, 9.6F, -4.033333F, 0.27662F, 0, 0));
        root.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 9).addBox(0, -1, 0, 7, 6, 2), PartPose.offsetAndRotation(-4, 12.36667F, -3.8F, 0.31614F, 0, 0));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(18, 23).addBox(0, 0, 0, 1, 8, 1), PartPose.offsetAndRotation(-1, 15, -0.6666667F, 0.47304F, 0, 0));
        root.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(22, 23).addBox(0, 0, 0, 1, 4, 1), PartPose.offsetAndRotation(-1, 22.1F, 2.9F, 1.38309F, 0, 0));
        return LayerDefinition.create(data, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ImpEnitty entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        head.yRot = headYaw / 57.29578F;
        head.xRot = headPitch / 57.29578F;

        float armPitch = Mth.sin(animationProgress * 0.067F) * 0.05F;
        float armRoll = Mth.cos(animationProgress * 0.09F) * 0.05F + 0.05F;

        float cosA = Mth.cos(limbAngle * 0.6662F);
        float cosB = Mth.cos(limbAngle * 0.6662F + Mth.PI);

        rightArm.setRotation((cosB * limbDistance) + armPitch, 0, armRoll);
        leftArm.setRotation((cosA * limbDistance) - armPitch, 0, -armRoll);

        rightLeg.xRot = cosA * 1.4F * limbDistance - 0.158F;
        leftLeg.xRot = cosB * 1.4F * limbDistance - 0.15919F;

        rightShin.xRot = cosA * 1.4F * limbDistance + 0.82623F;
        leftShin.xRot = cosB * 1.4F * limbDistance + 0.82461F;
        rightFoot.xRot = cosA * 1.4F * limbDistance - 0.01403F;
        leftFoot.xRot = cosB * 1.4F * limbDistance - 0.01214F;
    }

    @Override
    public void translateToHand(net.minecraft.world.entity.HumanoidArm arm,
            com.mojang.blaze3d.vertex.PoseStack poseStack) {
        ModelPart armPart = arm == net.minecraft.world.entity.HumanoidArm.RIGHT
                ? rightArm : leftArm;
        armPart.translateAndRotate(poseStack);
        poseStack.translate(
                arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? -0.06F : 0.06F,
                0.08F, 0.0F);
    }
}
