package com.invasion.client.render.entity.model;

import com.invasion.entity.IMSkeletonEntity;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Extension of SkeletonEntityModel that does not play animations... or some reason
 */
@Deprecated
public class ModelIMSkeleton extends SkeletonModel<IMSkeletonEntity> {
    public ModelIMSkeleton(ModelPart modelPart) {
        super(modelPart);
    }

    @Override
    public void prepareMobModel(IMSkeletonEntity mobEntity, float f, float g, float h) {
    }
}