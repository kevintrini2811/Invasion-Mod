package com.invasion.client.render.entity.model;

import java.util.Map;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import com.invasion.client.render.animation.AnimationRegistry;
import com.invasion.client.render.animation.WingBone;
import com.invasion.client.render.animation.Animator;
import com.invasion.entity.VultureEntity;

public class ModelBird extends HierarchicalModel<VultureEntity> {
    private Animator<WingBone> animationWingFlap;
    private final ModelPart root;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;

    private final ModelPart leftThigh;
    private final ModelPart rightThigh;

    private final ModelPart[] legParts;

    public ModelBird(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("tail");
        leftThigh = body.getChild("left_thigh");
        rightThigh = body.getChild("right_thigh");
        ModelPart leftLeg = leftThigh.getChild("leg");
        ModelPart rightLeg = rightThigh.getChild("leg");
        legParts = new ModelPart[] {
                leftThigh, rightThigh,
                leftLeg, rightLeg,
                leftLeg.getChild("left_toe"), leftLeg.getChild("right_toe"), leftLeg.getChild("back_toe"),
                rightLeg.getChild("left_toe"), rightLeg.getChild("right_toe"), rightLeg.getChild("back_toe")
        };
        animationWingFlap = AnimationRegistry.instance().<WingBone>get("bird_wing_flap").createAnimator(Map.of(
                WingBone.RIGHT_SHOULDER, body.getChild("right_wing_1"),
                WingBone.LEFT_SHOULDER, body.getChild("left_wing_1"),
                WingBone.RIGHT_ELBOW, body.getChild("right_wing_1").getChild("right_wing_2"),
                WingBone.LEFT_ELBOW, body.getChild("left_wing_1").getChild("left_wing_2")
        ));
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-3.5F, 0, -3.5F, 7, 12, 7), PartPose.offset(3.5F, 7, 3.5F));
        body.addOrReplaceChild("right_wing_1", CubeListBuilder.create().texOffs(0, 22).addBox(-7, -1, -1, 7, 9, 1), PartPose.offset(-3.5F, 2, 3.5F))
            .addOrReplaceChild("right_wing_2", CubeListBuilder.create().texOffs(16, 24).addBox(-14, -1, -0.5F, 14, 7, 1), PartPose.offset(-7, 0, -0.5F));
        body.addOrReplaceChild("left_wing_1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(0, -1, -1, 7, 9, 1), PartPose.offset(3.5F, 2, 3.5F))
            .addOrReplaceChild("left_wing_2", CubeListBuilder.create().texOffs(16, 24).mirror().addBox(0, -1, -0.5F, 14, 7, 1), PartPose.offset(7, 0, -0.5F));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 0).mirror().addBox(-2.5F, -5, -4, 5, 6, 6), PartPose.offset(0, 0.5F, 1.5F))
            .addOrReplaceChild("beak", CubeListBuilder.create().texOffs(19, 0).mirror().addBox(-0.5F, 0, -2, 1, 2, 2), PartPose.offset(0, -3, -4));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 12).addBox(-3, 0, 0, 5, 9, 1), PartPose.offsetAndRotation(0.5F, 12, 2.5F, 0.446143F, 0, 0));

        PartDefinition rightLeg = body
                .addOrReplaceChild("right_thigh", CubeListBuilder.create().texOffs(13, 18).addBox(-1, 0, -1, 2, 2, 2), PartPose.offset(-1.5F, 12, -1))
                .addOrReplaceChild("leg", CubeListBuilder.create().texOffs(13, 12).addBox(-0.5F, 0, -0.5F, 1, 5, 1), PartPose.ZERO);
        rightLeg.addOrReplaceChild("left_toe", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, -2, 1, 1, 2), PartPose.offsetAndRotation(0.2F, 4, 0, 0, -0.1396263F, 0));
        rightLeg.addOrReplaceChild("back_toe", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 0, 0, 1, 1, 2), PartPose.offsetAndRotation(0, 4, 0, -0.349066F, 0, 0));
        rightLeg.addOrReplaceChild("right_toe", CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0, -2, 1, 1, 2), PartPose.offsetAndRotation(-0.2F, 4, 0, 0, 0.1396263F, 0));

        PartDefinition leftLeg = body
                .addOrReplaceChild("left_thigh", CubeListBuilder.create().texOffs(13, 18).mirror().addBox(-1, 0, -1, 2, 2, 2), PartPose.offset(1.5F, 12, -1))
                .addOrReplaceChild("leg", CubeListBuilder.create().texOffs(13, 12).mirror().addBox(-0.5F, 0, -0.5F, 1, 5, 1), PartPose.ZERO);
        leftLeg.addOrReplaceChild("left_toe", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0, 0, -2, 1, 1, 2), PartPose.offsetAndRotation(0.2F, 4, 0, 0, -0.1396263F, 0));
        leftLeg.addOrReplaceChild("back_toe", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, 0, 0, 1, 1, 2), PartPose.offsetAndRotation(0, 4, 0, -0.349066F, 0, 0));
        leftLeg.addOrReplaceChild("right_toe", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1, 0, -2, 1, 1, 2), PartPose.offsetAndRotation(-0.2F, 4, 0, 0, 0.1396263F, 0));
        return LayerDefinition.create(data, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setAngles(VultureEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        body.xRot = 1.570796F - headPitch * Mth.DEG_TO_RAD;
        body.yRot = 0;
    }

    @Override
    public void animateModel(VultureEntity entity, float limbAngle, float limbDistance, float tickDelta) {
        float legSweepProgress = entity.getLegSweepProgress();
        float flapProgress = entity.getWingAnimationState().getCurrentAnimationTimeInterp(tickDelta);
        animationWingFlap.updateAnimation(flapProgress);

        for (ModelPart i : legParts) {
            i.xRot = 0.08726647F * legSweepProgress;
        }

        body.zRot = -entity.getRoll(tickDelta) * Mth.DEG_TO_RAD;
        body.y = (7 + Mth.cos(flapProgress * Mth.TWO_PI) * 1.4F);
        rightThigh.xRot += Mth.cos(flapProgress * Mth.TWO_PI) * 0.08726646324990228D;
        leftThigh.xRot += Mth.cos(flapProgress * Mth.TWO_PI) * 0.08726646324990228D;
        tail.xRot = ((float)(0.2617993956013792D + Mth.cos(flapProgress * Mth.TWO_PI) * 0.03490658588512815D));
        head.xRot = ((float)(-0.3141592700403172D - Mth.cos(flapProgress * Mth.TWO_PI) * 0.03490658588512815D));
    }
}