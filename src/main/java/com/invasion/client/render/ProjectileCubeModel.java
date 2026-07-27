package com.invasion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

final class ProjectileCubeModel extends EntityModel<EntityRenderState> {
    ProjectileCubeModel(ModelPart root) {
        super(root);
    }

    static LayerDefinition createLayer(int textureWidth, int textureHeight) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, textureWidth, textureHeight);
    }
}
