package com.invasion.client.render.entity.model;

import com.invasion.entity.VultureEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class ModelB extends HierarchicalModel<VultureEntity> {
    private final ModelPart root;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart outerRightWing;
    private final ModelPart outerLeftWing;

    public ModelB(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.outerRightWing = rightWing.getChild("outer");
        this.outerLeftWing = leftWing.getChild("outer");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();

        PartDefinition batHead = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.ZERO);
        batHead.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-4, -6, -2, 3, 4, 1), PartPose.ZERO);
        batHead.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).addBox(1, -6, -2, 3, 4, 1).mirror(), PartPose.ZERO);
        PartDefinition batBody = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3, 4, -3, 6, 12, 6)
                .texOffs(0, 34).addBox(-5, 16, 0, 10, 6, 1), PartPose.ZERO);
        batBody
            .addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(42, 0).addBox(-12, 1, 1.5F, 10, 16, 1), PartPose.ZERO)
            .addOrReplaceChild("outer", CubeListBuilder.create().texOffs(24, 16).addBox(-8, 1, 0, 8, 12, 1), PartPose.offset(-12, 1, 1.5F));
        batBody
            .addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(42, 0).addBox(2, 1, 1.5F, 10, 16, 1).mirror(), PartPose.ZERO)
            .addOrReplaceChild("outer", CubeListBuilder.create().texOffs(24, 16).addBox(0, 1, 0, 8, 12, 1).mirror(), PartPose.offset(12, 1, 1.5F));

        return LayerDefinition.create(data, 64, 64);
    }

    public int getBatSize() {
        return 36;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setAngles(VultureEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        head.setRotation((headPitch / 57.295776F), (headYaw / 57.295776F), 0);
        body.xRot = (0.7853982F + Mth.cos(animationProgress * 0.1F) * 0.15F);
        rightWing.yRot = (Mth.cos(animationProgress * 1.3F) * Mth.PI * 0.25F);
        leftWing.yRot = -rightWing.yRot;
        outerRightWing.yRot = rightWing.yRot * 0.5F;
        outerLeftWing.yRot = -rightWing.yRot * 0.5F;
    }
}