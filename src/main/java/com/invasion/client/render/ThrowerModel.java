package com.invasion.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class ThrowerModel extends HumanoidModel<ThrowerRenderState> {
    public ThrowerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        CubeDeformation dilation = CubeDeformation.NONE;
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        PartDefinition head = root.addOrReplaceChild(PartNames.HEAD,
                CubeListBuilder.create().texOffs(16, 14).addBox(-2, -2, -2, 4, 2, 4, dilation),
                PartPose.offset(0, 16, 4));
        // Since 26.2 HumanoidModel expects "hat" to be a child of "head".
        head.addOrReplaceChild(PartNames.HAT, CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create()
                .texOffs(1, 1).addBox(-6, 2, -2, 12, 4, 9, dilation)
                .texOffs(0, 23).addBox(-6, 0, 0, 12, 2, 7, dilation), PartPose.offset(-0.4F, 16, 3));
        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create()
                .texOffs(39, 22).addBox(-3, 0, -1.466667F, 3, 7, 3, dilation), PartPose.offset(-6.5F, 16, 5));
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(0, 0, 0, 2, 4, 2, dilation), PartPose.offset(5, 16, 5));
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create()
                .texOffs(0, 14).addBox(-2, 0, -2, 4, 2, 4, dilation), PartPose.offset(-4.066667F, 22, 4));
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create()
                .texOffs(0, 14).mirror().addBox(-2, 0, -2, 4, 2, 4, dilation), PartPose.offset(3, 22, 4));
        return LayerDefinition.create(data, 64, 32);
    }

    @Override
    public void setupAnim(ThrowerRenderState state) {
        super.setupAnim(state);
        hat.visible = false;
        head.setPos(0, 16, 1);
        hat.setPos(0, 16, 1);
        body.setPos(0, 16, -3);
        rightLeg.setPos(-3, 22, 0);
        leftLeg.setPos(3, 22, 0);
        leftArm.setPos(6, 16, -1);
        rightArm.setPos(-6, 16, 0);
    }
}
