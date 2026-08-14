package com.invasion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class FatZombieModel extends EntityModel<FatZombieRenderState> {
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public FatZombieModel(ModelPart root) {
        super(root);
        head = root.getChild("Head");
        rightLeg = root.getChild("rightLeg");
        leftLeg = root.getChild("leftLeg");
        rightArm = root.getChild("rightArm");
        leftArm = root.getChild("leftArm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;
        root.addOrReplaceChild("Body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10, -9, -5, 20, 20, 10, none)
                .texOffs(0, 28).addBox(-10, -2, -11, 20, 13, 6, none),
                PartPose.offset(0, 7, 0));
        root.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 47)
                .addBox(-6, -6, -4, 12, 6, 8, none), PartPose.offset(0, -2, 0));
        root.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(60, 0)
                .addBox(-3, -1, -3, 5, 8, 5, none), PartPose.offset(-5, 17, 0));
        root.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(60, 13)
                .addBox(-2, -1, -3, 5, 8, 5, none), PartPose.offset(5, 17, 0));
        root.addOrReplaceChild("rightArm", CubeListBuilder.create().texOffs(40, 47)
                .addBox(-5, -1, -3, 7, 13, 6, none), PartPose.offset(-11, 2, 0));
        root.addOrReplaceChild("leftArm", CubeListBuilder.create().texOffs(52, 28)
                .addBox(-1, -1, -3, 7, 13, 6, none), PartPose.offset(11, 2, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(FatZombieRenderState state) {
        super.setupAnim(state);
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F)
                * 1.4F * state.walkAnimationSpeed;
        leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI)
                * 1.4F * state.walkAnimationSpeed;
        rightArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + Mth.PI)
                * state.walkAnimationSpeed;
        leftArm.xRot = Mth.cos(state.walkAnimationPos * 0.6662F)
                * state.walkAnimationSpeed;
        if (state.eating) {
            float bite = Mth.sin(state.eatAnimation * Mth.PI * 8.0F) * 0.12F;
            rightArm.xRot = -2.05F + bite;
            leftArm.xRot = -2.05F + bite;
            rightArm.yRot = -0.35F;
            leftArm.yRot = 0.35F;
            head.xRot = 0.35F + bite;
        }
    }
}
