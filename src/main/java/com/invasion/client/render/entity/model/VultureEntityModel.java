package com.invasion.client.render.entity.model;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import com.invasion.client.render.animation.AnimationRegistry;
import com.invasion.client.render.animation.MouthBones;
import com.invasion.client.render.animation.Animator;
import com.invasion.entity.VultureEntity;

public class VultureEntityModel extends ModelGiantBird<VultureEntity> {
    private final Animator<?> animationBeak;

    public VultureEntityModel(ModelPart root) {
        super(root);
        animationBeak = AnimationRegistry.instance().<MouthBones>get("bird_beak").createAnimator(Map.of(
                MouthBones.UPPER_MOUTH, head.getChild("upper_beak"),
                MouthBones.LOWER_MOUTH, head.getChild("lower_beak")
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
        createFoot(body.addOrReplaceChild("left_thigh", CubeListBuilder.create().texOffs(84, 82).mirror().addBox(-4.5F, -3.5F, -4.5F, 9, 15, 9), PartPose.offsetAndRotation(5, 20, -2, -0.39F, 0, -0.09F))
                .addOrReplaceChild("leg", CubeListBuilder.create().texOffs(56, 50).mirror().addBox(-2, -3, -2, 4, 16, 4), PartPose.offsetAndRotation(0, 11, 0, -0.72F, 0, 0))
                .addOrReplaceChild("ankle", CubeListBuilder.create().texOffs(16, 16).addBox(0, 0, 0, 0, 0, 0), PartPose.offsetAndRotation(0, 12, 0, 0.1F, -0.2F, 0)),
                0, -1
        );

        PartDefinition neck3 = body
            .addOrReplaceChild("neck", CubeListBuilder.create().texOffs(43, 95).addBox(-7, -7, -6.5F, 14, 10, 13), PartPose.offsetAndRotation(0, -10, 1, -0.18F, 0, 0))
            .addOrReplaceChild("neck", CubeListBuilder.create().texOffs(50, 73).addBox(-5, -4, -5, 10, 8, 10), PartPose.offsetAndRotation(0, -8, 0, 0.52F, 0, 0))
            .addOrReplaceChild("neck", CubeListBuilder.create().texOffs(80, 65).addBox(-4, -5.5F, -5, 8, 5, 10), PartPose.offsetAndRotation(0, -2, 0, 0.26F, 0, 0));

        PartDefinition head = neck3.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 108).addBox(-4.5F, -5, -9.5F, 9, 8, 11), PartPose.offsetAndRotation(0, -4, 0, -0.97F, 0, 0));
        head.addOrReplaceChild("upper_beak", CubeListBuilder.create().texOffs(54, 118).addBox(-2.5F, -1, -5, 5, 2, 8), PartPose.offset(0, -0.8F, -10))
                .addOrReplaceChild("tip", CubeListBuilder.create().texOffs(72, 118).addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, 0, -6));
        head.addOrReplaceChild("lower_beak", CubeListBuilder.create().texOffs(80, 118).addBox(-2.5F, -1, -5, 5, 2, 8), PartPose.offset(0, 1.5F, -10))
                .addOrReplaceChild("tip", CubeListBuilder.create().texOffs(78, 121).addBox(-1, -0.5F, -1, 2, 1, 2), PartPose.offset(0, -0.5F, -6));

        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(80, 23).addBox(-8.5F, -5, -1, 17, 40, 2), PartPose.offsetAndRotation(0, 19, 8, 0.3F, 0, 0));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 50).addBox(-24.5F, -4.5F, -1.5F, 25, 29, 3), PartPose.offset(-7, -8, 6))
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
            .addOrReplaceChild("left_toe", CubeListBuilder.create().texOffs(0, 0).addBox(-1, 0.5F, -1, 2, 9, 2), PartPose.offsetAndRotation(0.5F * mirror, 0, 1, -0.8F, -0.28F, -0.28F))
            .addOrReplaceChild("claw", CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0, -1, 1, 4, 2), PartPose.offset(0, 9, 0));
        ankle
            .addOrReplaceChild("middle_toe", CubeListBuilder.create().texOffs(8, 0).addBox(-1, 0, -1, 2, 10, 2), PartPose.rotation(-0.8F, 0, 0))
            .addOrReplaceChild("claw", CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0, -1, 1, 4, 2), PartPose.offset(0, 9, 0));
        ankle
            .addOrReplaceChild("right_toe", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -0.5F, -1, 2, 9, 2), PartPose.offsetAndRotation(-1 * mirror, 0, 1, -0.8F, 0.28F, 0.28F))
            .addOrReplaceChild("claw", CubeListBuilder.create().texOffs(0, 11).addBox(-0.5F, 0, -1, 1, 4, 2), PartPose.offset(0, 8, 0));
    }

    @Override
    protected void animateRunning(VultureEntity entity, float legProgress) {
        if (legProgress >= 0.109195F && legProgress < 0.5373563F) {
            legProgress += 0.03735632F;
            if (legProgress >= 0.5373563F) {
                legProgress -= 0.4281609F;
            }
            float t = Mth.cos(25.132742F * legProgress / 0.8908046F);
            body.xRot += -t * 0.04F;
            neck1.xRot += t * 0.08F;
            body.y += -t * 1.9F;
        }
    }

    @Override
    protected void updateAnimations(VultureEntity entity, float tickDelta) {
        super.updateAnimations(entity, tickDelta);
        animationBeak.updateAnimation(entity.getBeakAnimationState().getCurrentAnimationTimeInterp(tickDelta));
    }
}