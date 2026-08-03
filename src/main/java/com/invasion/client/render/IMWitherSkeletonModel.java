package com.invasion.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.SkeletonModel;

public final class IMWitherSkeletonModel
        extends SkeletonModel<IMWitherSkeletonRenderState> {
    public IMWitherSkeletonModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(IMWitherSkeletonRenderState state) {
        super.setupAnim(state);
        if (!state.groupLeaderWaiting) {
            return;
        }
        rightArm.xRot = -(float) Math.PI;
        leftArm.xRot = -(float) Math.PI;
        rightArm.yRot = 0.0F;
        leftArm.yRot = 0.0F;
        rightArm.zRot = -0.12F;
        leftArm.zRot = 0.12F;
    }
}
