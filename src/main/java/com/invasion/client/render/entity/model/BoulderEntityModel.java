package com.invasion.client.render.entity.model;

import com.invasion.entity.BoulderEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BoulderEntityModel extends HierarchicalModel<BoulderEntity> {
    private final ModelPart root;

    public BoulderEntityModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        root.addOrReplaceChild("boulder", CubeListBuilder.create().addBox(-4, -4, -4, 8, 8, 8), PartPose.ZERO);
        return LayerDefinition.create(data, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(BoulderEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        root.zRot = animationProgress;
        root.xRot = headPitch;
        root.yRot = headYaw;
    }
}