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
        if (state.dancing) {
            float time = state.ageInTicks / 60.0F;
            head.x = (float) Math.sin(time * 10.0F);
            head.y = (float) Math.sin(time * 40.0F) + 0.4F;
            rightArm.zRot = (float) Math.toRadians(
                    70.0F + Math.cos(time * 40.0F) * 10.0F);
            leftArm.zRot = -rightArm.zRot;
            rightArm.y = (float) Math.sin(time * 40.0F) * 0.5F + 1.5F;
            leftArm.y = rightArm.y;
            body.y = (float) Math.sin(time * 40.0F) * 0.35F;
        }
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
