package com.invasion.client.render.entity.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

@Deprecated(since = "unused")
public class QuetzalcoatlusEntityModel<T extends Entity> extends HierarchicalModel<T> {

    private final ModelPart root;

    public QuetzalcoatlusEntityModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        // TODO: Usage of texture mirroring is nonsensical. Left limbs should mirror, right and everything else should not. WHY IS EVERYTHING MIRRORED???
        root.addOrReplaceChild("head", CubeListBuilder.create().mirror().addBox(-2.5F, -3.5F, -11, 5, 7, 11).addBox(0, -9, -13, 0, 7, 16).addBox(-2, -2.5F, -21F, 4, 6, 10).addBox(-1.5F, -1.5F, -30, 3, 5, 9), PartPose.offset(0, 19, -40));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(3, 93).mirror().addBox(0, -2, 0, 3, 2, 5), PartPose.offset(-1.5F, 18.5F, 11));
        root.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(108, 104).mirror().addBox(-4.5F, -2, 11, 9, 7, 7), PartPose.offset(0, 18, -7));
        root.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(95, 81).mirror().addBox(-5, -2, 0, 10, 8, 11), PartPose.offset(0, 18, -7));
        root.addOrReplaceChild("neck_1", CubeListBuilder.create().texOffs(1, 73).mirror().addBox(-2.5F, -5, -11, 5, 7, 10), PartPose.offset(0, 21, -6));
        root.addOrReplaceChild("neck_2", CubeListBuilder.create().texOffs(28, 73).mirror().addBox(-1, -4, -33, 4, 6, 23), PartPose.offset(-1, 20, -7));
        root.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(3, 104).mirror().addBox(0, -3, 4F, 2, 2, 3), PartPose.offset(-1, 19.5F, 12));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .mirror().addBox(0, -2, 8, 2, 2, 14)
                .mirror(false).addBox(0, -2, -2, 2, 4, 10).addBox(-0.5F, -2, 22, 3, 1, 4), PartPose.offset(3, 19, 10));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().mirror().addBox(-2, -2, -2, 2, 4, 10).addBox(-2, -2, 8, 2, 2, 14).addBox(-2.5F, -2, 22, 3, 1, 4), PartPose.offset(-3, 19, 10));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().mirror().addBox(-8, 0, 3, 9, 1, 29).addBox(-8, -1.5F, -2, 8, 4, 5), PartPose.offset(-5, 18, -5F))
            .addOrReplaceChild("joint", CubeListBuilder.create().mirror().addBox(-15, -1, -2, 15, 3, 4).addBox(-15, 0, 2, 15, 1, 30), PartPose.offset(-8F, 0, 0))
            .addOrReplaceChild("joint", CubeListBuilder.create().mirror().addBox(-21, -1, -1, 18, 3, 3).addBox(-21, 0, 2, 18, 1, 31).addBox(-57, -0.5F, -1, 36, 2, 2).addBox(-57F, 0, 1, 36, 1, 32).addBox(-21, 0, -4, 6, 1, 3), PartPose.offset(-12, 0, -1));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().mirror().addBox(0, -1.5F, -2, 8, 4, 5).addBox(-1, 0, 3, 9, 1, 29), PartPose.offset(5F, 18F, -5F))
            .addOrReplaceChild("joint", CubeListBuilder.create().mirror().addBox(0, 0, 2, 15, 1, 30).addBox(0, -1, -2, 15, 3, 4), PartPose.ZERO)
            .addOrReplaceChild("joint", CubeListBuilder.create().mirror().addBox(0, -1, -1, 18, 3, 3).addBox(0, 0, 2, 18, 1, 31).addBox(18, 0, 1, 36, 1, 32).addBox(18, -0.5F, -0.5F, 36, 2, 2).addBox(12, 0, -4, 6, 1, 3), PartPose.offset(15F, 0, -1));
        return LayerDefinition.create(data, 500, 150);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }
}
