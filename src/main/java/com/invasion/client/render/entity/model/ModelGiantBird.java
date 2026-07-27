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
import com.invasion.client.render.animation.AnimationAction;
import com.invasion.client.render.animation.AnimationRegistry;
import com.invasion.client.render.animation.BirdLegBone;
import com.invasion.client.render.animation.WingBone;
import com.invasion.client.render.animation.Animator;
import com.invasion.entity.VultureEntity;

public class ModelGiantBird<T extends VultureEntity> extends HierarchicalModel<T> {
    protected final Animator<WingBone> animationFlap;
    protected final Animator<BirdLegBone> animationRun;

    protected final ModelPart body;
    protected final ModelPart tail;

    protected final ModelPart rightThigh;
    protected final ModelPart leftThigh;

    protected final ModelPart neck1;
    protected final ModelPart neck2;
    protected final ModelPart neck3;
    protected final ModelPart head;

    public ModelGiantBird(ModelPart root) {
        body = root.getChild("body");
        tail = body.getChild("tail");
        rightThigh = body.getChild("right_thigh");
        leftThigh = body.getChild("left_thigh");
        neck1 = body.getChild("neck");
        neck2 = neck1.getChild("neck");
        neck3 = neck2.getChild("neck");
        head = neck3.getChild("head");


        animationRun = AnimationRegistry.instance().<BirdLegBone>get("bird_run").createAnimator(Map.of(
            BirdLegBone.LEFT_KNEE, leftThigh,
            BirdLegBone.RIGHT_KNEE, rightThigh,
            BirdLegBone.LEFT_ANKLE, leftThigh.getChild("leg"),
            BirdLegBone.RIGHT_ANKLE, rightThigh.getChild("leg"),
            BirdLegBone.LEFT_METATARSOPHALANGEAL_ARTICULATIONS, leftThigh.getChild("leg").getChild("ankle"),
            BirdLegBone.RIGHT_METATARSOPHALANGEAL_ARTICULATIONS, rightThigh.getChild("leg").getChild("ankle"),
            BirdLegBone.LEFT_BACK_CLAW, leftThigh.getChild("leg").getChild("ankle").getChild("back_toe"),
            BirdLegBone.RIGHT_BACK_CLAW, rightThigh.getChild("leg").getChild("ankle").getChild("back_toe")
        ));
        animationFlap = AnimationRegistry.instance().<WingBone>get("wing_flap_2_piece").createAnimator(Map.of(
                WingBone.LEFT_SHOULDER, body.getChild("left_wing"),
                WingBone.RIGHT_SHOULDER, body.getChild("right_wing"),
                WingBone.LEFT_ELBOW, body.getChild("left_wing").getChild("elbow"),
                WingBone.RIGHT_ELBOW, body.getChild("right_wing").getChild("elbow")
        ));
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-10, -10, -10, 20, 30, 20), PartPose.offsetAndRotation(0, -19, 0, 0.7F, 0, 0));
        createFoot(body.addOrReplaceChild("right_thigh", CubeListBuilder.create().texOffs(84, 82).addBox(-4.5F, -3.5F, -4.5F, 9, 15, 9), PartPose.offsetAndRotation(-5, 20, -2, -0.39F, 0, 0.09F))
                .addOrReplaceChild("leg", CubeListBuilder.create().texOffs(56, 50).addBox(-2, -3, -2, 4, 16, 4), PartPose.offsetAndRotation(0, 11, 0, -0.72F, 0, 0))
                .addOrReplaceChild("ankle", CubeListBuilder.create().texOffs(16, 16).addBox(0, 0, 0, 0, 0, 0), PartPose.offsetAndRotation(0, 12, 0, 0.1F, 0.2F, 0)),
                -36, 1
        );
        createFoot(body.addOrReplaceChild("left_thigh", CubeListBuilder.create().texOffs(84, 82).mirror().addBox(-4.5F, -3.5F, -4.5F, 9, 15, 9), PartPose.offsetAndRotation(-5, 20, -2, -0.39F, 0, -0.09F))
                .addOrReplaceChild("leg", CubeListBuilder.create().texOffs(56, 50).mirror().addBox(-2, -3, -2, 4, 16, 4), PartPose.offsetAndRotation(0, 11, 0, -0.72F, 0, 0))
                .addOrReplaceChild("ankle", CubeListBuilder.create().texOffs(16, 16).addBox(0, 0, 0, 0, 0, 0), PartPose.offsetAndRotation(0, 12, 0, 0.1F, -0.2F, 0)),
                0, -1
        );

        PartDefinition neck3 = body
                .addOrReplaceChild("neck", CubeListBuilder.create().texOffs(43, 95).addBox(-7, -7, -6.5F, 14, 10, 13), PartPose.offsetAndRotation(0, -10, 1, -0.18F, 0, 0))
                .addOrReplaceChild("neck", CubeListBuilder.create().texOffs(50, 73).addBox(-5, -4, -5, 10, 8, 10), PartPose.offsetAndRotation(0, -8, 0, 0.52F, 0, 0))
                .addOrReplaceChild("neck", CubeListBuilder.create().texOffs(80, 65).addBox(-4, -5.5F, -5, 8, 5, 10), PartPose.offsetAndRotation(0, -2, 0, 0.26F, 0, 0));
        neck3.addOrReplaceChild("back_feathers", CubeListBuilder.create().texOffs(-7, 108).addBox(-4, 0, -1.5F, 8, 0, 7), PartPose.offsetAndRotation(0, -3, 5, -1.11F, 0, 0));
        neck3.addOrReplaceChild("left_feathers", CubeListBuilder.create().texOffs(-6, 115).addBox(-3, 0, -1, 6, 0, 6), PartPose.offsetAndRotation(4, -3, 2, -0.85F, -1.87F, 0.39F));
        neck3.addOrReplaceChild("right_feathers", CubeListBuilder.create().texOffs(-6, 115).addBox(-3, 0, -1, 6, 0, 6), PartPose.offsetAndRotation(-4, -3, 2, -0.85F, 1.87F, -0.39F));

        PartDefinition head = neck3.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 108).addBox(-4.5F, -5, -9.5F, 9, 8, 11), PartPose.offsetAndRotation(0, -4, 0, -0.97F, 0, 0));
        head.addOrReplaceChild("upper_beak", CubeListBuilder.create().texOffs(54, 118).addBox(-2.5F, -1, -3, 5, 2, 6), PartPose.offset(0, -0.8F, -10))
                .addOrReplaceChild("tip", CubeListBuilder.create().texOffs(70, 118).addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, 0, -4));
        head.addOrReplaceChild("lower_beak", CubeListBuilder.create().texOffs(78, 118).addBox(-2.5F, -1, -3, 5, 2, 6), PartPose.offset(0, 1.5F, -10))
                .addOrReplaceChild("tip", CubeListBuilder.create().texOffs(76, 121).addBox(-1, -0.5F, -1, 2, 1, 2), PartPose.offset(0, -0.5F, -4));
        head.addOrReplaceChild("feathers", CubeListBuilder.create().texOffs(-5, 121).addBox(-3.5F, 0, -0.5F, 7, 0, 5), PartPose.offsetAndRotation(0, -5, 0, 0.38F, 0, 0));

        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(80, 23).addBox(-8.5F, -5, -1, 17, 40, 2), PartPose.offsetAndRotation(0, 19, 8, 0.3F, 0, 0));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 50).addBox(-24.5F, -4.5F, -1.5F, 25, 29, 3), PartPose.offset(7, -8, 6))
            .addOrReplaceChild("elbow", CubeListBuilder.create().texOffs(0, 82).addBox(-20.5F, -5, -1, 23, 24, 2), PartPose.offset(-23, 1, 0))
            .addOrReplaceChild("tip", CubeListBuilder.create().texOffs(80, 0).addBox(-20.5F, -5, -0.5F, 23, 22, 1), PartPose.offset(-21, 0.2F, 0.3F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 50).mirror().addBox(-0.5F, -4.5F, -1.5F, 25, 29, 3), PartPose.offset(7, -8, 6))
            .addOrReplaceChild("elbow", CubeListBuilder.create().texOffs(0, 82).mirror().addBox(-2.5F, -5, -1, 23, 24, 2), PartPose.offset(23, 1, 0))
            .addOrReplaceChild("tip", CubeListBuilder.create().texOffs(80, 0).mirror().addBox(-2.5F, -5, -0.5F, 23, 22, 1), PartPose.offset(21, 0.2F, 0.3F));

        return LayerDefinition.create(data, 128, 128);
    }

    private static void createFoot(PartDefinition ankle, float clawAngle, int mirror) {
        ankle
            .addOrReplaceChild("back_toe", CubeListBuilder.create().texOffs(60, 0).addBox(-1, -1, -1, 2, 8, 2), PartPose.offsetAndRotation(0, 0, 2, 1.34F, 0, 0))
            .addOrReplaceChild("claw", CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0, -1, 1, 4, 2), PartPose.offsetAndRotation(0, 6, 0, clawAngle, 0, 0));
        ankle
            .addOrReplaceChild("left_toe", CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0.5F, -1, 2, 9, 2), PartPose.offsetAndRotation(-0.5F, 0, 1, -0.8F, mirror * 0.28F, mirror * 0.28F))
            .addOrReplaceChild("claw", CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0, -1, 1, 4, 2), PartPose.offset(0, 9, 0));
        ankle
            .addOrReplaceChild("middle_toe", CubeListBuilder.create().texOffs(8, 0).addBox(-1, 0, -1, 2, 10, 2), PartPose.rotation(-0.8F, 0, 0))
            .addOrReplaceChild("claw", CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0, -1, 1, 4, 2), PartPose.offset(0, 9, 0));
        ankle
            .addOrReplaceChild("right_toe", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -0.5F, -1, 2, 9, 2), PartPose.offsetAndRotation(1, 0, 1, -0.8F, mirror * -0.28F, mirror * -0.28F))
            .addOrReplaceChild("claw", CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0, -1, 1, 4, 2), PartPose.offset(0, 8, 0));
    }

    @Override
    public ModelPart root() {
        return body;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        body.xRot += 0.8707963705062867F - headPitch * Mth.DEG_TO_RAD;
    }

    @Override
    public void animateModel(T entity, float limbAngle, float limbDistance, float tickDelta) {
        float roll = entity.getRoll(tickDelta);
        float headYaw = Mth.rotLerp(tickDelta, entity.yHeadRotO, entity.getYHeadRot());
        float headPitch = entity.getViewXRot(tickDelta);
        resetSkeleton();

        updateAnimations(entity, tickDelta);

        if (entity.getWingAnimationState().getCurrentAction() == AnimationAction.WINGFLAP) {
            float flapCycle = entity.getWingAnimationState().getCurrentAnimationTimeInterp(tickDelta) / 0.2714932F;

            body.y += Mth.cos(flapCycle * Mth.TWO_PI) * 1.4F;
            rightThigh.xRot += Mth.cos(flapCycle * Mth.TWO_PI) * 0.08726646324990228F;
            leftThigh.xRot += Mth.cos(flapCycle * Mth.TWO_PI) * 0.08726646324990228F;
            tail.xRot += Mth.cos(flapCycle * Mth.TWO_PI) * 0.03490658588512815F;
        }

        body.zRot = (-roll / 180 * 3.141593F);

        headPitch = Mth.clamp(Mth.wrapDegrees(headPitch), -56.650002F, 37.16F);
        float pitchFactor = (headPitch + 56.650002F) / 93.800003F;
        head.xRot += -0.96F + pitchFactor * -0.1400001F;
        neck3.xRot += 0.378F + pitchFactor * -0.528F;
        neck2.xRot += 0.4F + pitchFactor * -0.4F;
        neck1.xRot += 0.513F + pitchFactor * -0.613F;

        headYaw = Mth.clamp(Mth.wrapDegrees(headYaw), -30.5F, 30.5F);
        float yawFactor = (headYaw + 30.5F) / 61;
        head.zRot += 0.8F + yawFactor * 2 * -0.8F;
        neck3.zRot += 0.38F + yawFactor * 2 * -0.38F;
        neck2.zRot += 0.14F + yawFactor * 2 * -0.14F;
        head.yRot += -0.7F + yawFactor * 2 * 0.7F;
        neck3.yRot += -0.12F + yawFactor * 2 * 0.12F;
    }

    public void resetSkeleton() {
        root().getAllParts().forEach(ModelPart::resetPose);
    }

    protected void animateRunning(T entity, float legProgress) {
        if (legProgress >= 0.109195F && legProgress < 0.5373563F) {
            float t = Mth.cos(25.132742F * legProgress / 0.7967914F);
            body.xRot += -t * 0.1F;
            neck1.yRot += t * 0.08F;
            body.zRot += -t;
        }
    }

    protected void updateAnimations(T entity, float tickDelta) {
        float flapProgress = entity.getWingAnimationState().getCurrentAnimationTimeInterp(tickDelta);
        float legProgress = entity.getLegAnimationState().getCurrentAnimationTimeInterp(tickDelta);
        animationFlap.updateAnimation(flapProgress);
        animationRun.updateAnimation(legProgress);
        if (entity.getLegAnimationState().getCurrentAction() == AnimationAction.RUN) {
            animateRunning(entity, legProgress);
        }
    }
}