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

public final class LargeZombieModel extends HumanoidModel<InvasionZombieRenderState> {
    private static final String LEGACY_HAT = "legacy_hat";
    private final ModelPart legacyHat;

    public LargeZombieModel(ModelPart root) {
        super(root);
        legacyHat = root.getChild(LEGACY_HAT);
    }

    @Override
    public void setupAnim(InvasionZombieRenderState state) {
        super.setupAnim(state);
        legacyHat.loadPose(head.storePose());
    }

    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(CubeDeformation.NONE);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation dilation) {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        PartDefinition head = root.addOrReplaceChild(PartNames.HEAD,
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F, -7, -3.5F, 7, 7, 7, dilation),
                PartPose.ZERO);
        head.addOrReplaceChild(PartNames.HAT,
                CubeListBuilder.create(),
                PartPose.ZERO);
        root.addOrReplaceChild(LEGACY_HAT,
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-3.5F, -7, -3.5F, 7, 7, 7, dilation.extend(0.5F)),
                PartPose.ZERO);
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create().texOffs(16, 15).addBox(-5, 0, -3, 10, 12, 5, dilation), PartPose.ZERO);
        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create().texOffs(46, 15).addBox(-3, -2, -2, 4, 12, 4, dilation), PartPose.offset(-6, 2, 0));
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create().texOffs(46, 15).mirror().addBox(-1, -2, -2, 4, 12, 4, dilation), PartPose.offset(6, 2, 0));
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4, dilation), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4, dilation), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(data, 64, 64);
    }
}
