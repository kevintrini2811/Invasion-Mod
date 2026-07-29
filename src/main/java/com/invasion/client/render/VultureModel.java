package com.invasion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class VultureModel extends EntityModel<VultureRenderState> {
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart upperBeak;
    private final ModelPart lowerBeak;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftThigh;
    private final ModelPart rightThigh;

    public VultureModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        ModelPart neck2 = neck.getChild("neck");
        ModelPart neck3 = neck2.getChild("neck");
        head = neck3.getChild("head");
        upperBeak = head.getChild("upper_beak");
        lowerBeak = head.getChild("lower_beak");
        leftWing = body.getChild("left_wing");
        rightWing = body.getChild("right_wing");
        leftThigh = body.getChild("left_thigh");
        rightThigh = body.getChild("right_thigh");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-10, -10, -10, 20, 30, 20),
                PartPose.offsetAndRotation(0, -19, 0, 0.7F, 0, 0));

        addLeg(body, "right_thigh", -5, false);
        addLeg(body, "left_thigh", 5, true);

        PartDefinition neck3 = body.addOrReplaceChild("neck",
                        CubeListBuilder.create().texOffs(43, 95)
                                .addBox(-7, -7, -6.5F, 14, 10, 13),
                        PartPose.offsetAndRotation(0, -10, 1, -0.18F, 0, 0))
                .addOrReplaceChild("neck",
                        CubeListBuilder.create().texOffs(50, 73)
                                .addBox(-5, -4, -5, 10, 8, 10),
                        PartPose.offsetAndRotation(0, -8, 0, 0.52F, 0, 0))
                .addOrReplaceChild("neck",
                        CubeListBuilder.create().texOffs(80, 65)
                                .addBox(-4, -5.5F, -5, 8, 5, 10),
                        PartPose.offsetAndRotation(0, -2, 0, 0.26F, 0, 0));

        PartDefinition head = neck3.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(14, 108)
                        .addBox(-4.5F, -5, -9.5F, 9, 8, 11),
                PartPose.offsetAndRotation(0, -4, 0, -0.97F, 0, 0));
        head.addOrReplaceChild("upper_beak",
                CubeListBuilder.create().texOffs(54, 118)
                        .addBox(-2.5F, -1, -5, 5, 2, 8),
                PartPose.offset(0, -0.8F, -10));
        head.addOrReplaceChild("lower_beak",
                CubeListBuilder.create().texOffs(80, 118)
                        .addBox(-2.5F, -1, -5, 5, 2, 8),
                PartPose.offset(0, 1.5F, -10));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(80, 23)
                        .addBox(-8.5F, -5, -1, 17, 40, 2),
                PartPose.offsetAndRotation(0, 19, 8, 0.3F, 0, 0));
        addWing(body, "right_wing", false);
        addWing(body, "left_wing", true);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addWing(PartDefinition body, String name, boolean left) {
        float direction = left ? 1 : -1;
        PartDefinition wing = body.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 50)
                        .mirror(left)
                        .addBox(left ? -0.5F : -24.5F, -4.5F, -1.5F, 25, 29, 3),
                PartPose.offset(7 * direction, -8, 6));
        PartDefinition elbow = wing.addOrReplaceChild("elbow",
                CubeListBuilder.create().texOffs(0, 82)
                        .mirror(left)
                        .addBox(left ? -2.5F : -20.5F, -5, -1, 23, 24, 2),
                PartPose.offset(23 * direction, 1, 0));
        elbow.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(80, 0)
                        .mirror(left)
                        .addBox(left ? -2.5F : -20.5F, -5, -0.5F, 23, 22, 1),
                PartPose.offset(21 * direction, 0.2F, 0.3F));
    }

    private static void addLeg(PartDefinition body, String name, float x, boolean left) {
        PartDefinition leg = body.addOrReplaceChild(name,
                        CubeListBuilder.create().texOffs(84, 82).mirror(left)
                                .addBox(-4.5F, -3.5F, -4.5F, 9, 15, 9),
                        PartPose.offsetAndRotation(x, 20, -2, -0.39F, 0, left ? -0.09F : 0.09F))
                .addOrReplaceChild("leg",
                        CubeListBuilder.create().texOffs(56, 50).mirror(left)
                                .addBox(-2, -3, -2, 4, 16, 4),
                        PartPose.offsetAndRotation(0, 11, 0, -0.72F, 0, 0));
        leg.addOrReplaceChild("foot",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3, 0, -6, 6, 2, 8),
                PartPose.offsetAndRotation(0, 12, 0, -0.4F, 0, 0));
    }

    @Override
    public void setupAnim(VultureRenderState state) {
        super.setupAnim(state);
        float flap = Mth.cos(state.ageInTicks * 0.55F);
        leftWing.zRot = -0.25F - flap * 0.65F;
        rightWing.zRot = 0.25F + flap * 0.65F;
        body.zRot = -state.roll * Mth.DEG_TO_RAD;
        body.xRot += -state.xRot * Mth.DEG_TO_RAD * 0.45F;
        neck.yRot = state.yRot * Mth.DEG_TO_RAD * 0.25F;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.35F;
        upperBeak.xRot = state.beakOpen ? -0.15F : 0;
        lowerBeak.xRot = state.beakOpen ? 0.25F : 0;
        float legPitch = state.clawsForward ? -1.1F : 0;
        leftThigh.xRot += legPitch;
        rightThigh.xRot += legPitch;
    }
}
