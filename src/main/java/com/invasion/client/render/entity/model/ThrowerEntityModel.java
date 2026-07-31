package com.invasion.client.render.entity.model;

import com.invasion.entity.ThrowerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class ThrowerEntityModel extends HumanoidModel<ThrowerEntity> {

    public ThrowerEntityModel(ModelPart root) {
        super(root);
    }

    public static MeshDefinition createMesh(CubeDeformation dilation, float pivotOffsetY) {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create().texOffs(16, 14).addBox(-2, -2, -2, 4, 2, 4, dilation), PartPose.offset(0, 16 + pivotOffsetY, 4));
        root.addOrReplaceChild(PartNames.HAT, CubeListBuilder.create().texOffs(16, 14).addBox(-2, -2, -2, 4, 2, 4, dilation.extend(0.5F)), PartPose.offset(0, 16 + pivotOffsetY, 4));
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create()
                .texOffs(1, 1).addBox(-6, 2, -2, 12, 4, 9, dilation)
                .texOffs(0, 23).addBox(-6, 0, 0, 12, 2, 7, dilation), PartPose.offset(-0.4F, 16 + pivotOffsetY, 3));
        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create()
                .texOffs(39, 22).addBox(-3, 0, -1.466667F, 3, 7, 3, dilation), PartPose.offset(-6.5F, 16 + pivotOffsetY, 5));
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(0, 0, 0, 2, 4, 2, dilation), PartPose.offset(5, 16 + pivotOffsetY, 5));
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create()
                .texOffs(0, 14).addBox(-2, 0, -2, 4, 2, 4, dilation), PartPose.offset(-4.066667F, 22 + pivotOffsetY, 4));
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create()
                .texOffs(0, 14).mirror().addBox(-2, 0, -2, 4, 2, 4, dilation), PartPose.offset(3, 22 + pivotOffsetY, 4));
        return data;
    }

    public static LayerDefinition getTexturedModelData() {
        return LayerDefinition.create(createMesh(CubeDeformation.NONE, 0), 64, 32);
    }

    @Override
    public void setupAnim(ThrowerEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.hat.visible = false;
        super.setupAnim(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        head.setPos(0, 16, 1);
        hat.setPos(0, 16, 1);
        body.setPos(0, 16, -3);
        rightLeg.setPos(-3, 22, 0);
        leftLeg.setPos(3, 22, 0);
        leftArm.setPos(6, 16, -1);
        rightArm.setPos(-6, 16, 0);
    }
}