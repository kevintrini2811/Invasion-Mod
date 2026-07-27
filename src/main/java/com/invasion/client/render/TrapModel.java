package com.invasion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import com.invasion.entity.TrapEntity;

public final class TrapModel extends EntityModel<TrapRenderState> {
    private final ModelPart core;
    private final ModelPart flames;

    public TrapModel(ModelPart root) {
        super(root);
        core = root.getChild("core");
        flames = root.getChild("flames");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 23).mirror()
                .addBox(0, 0, 0, 4, 1, 2), PartPose.offset(-2, -1, -1));
        root.addOrReplaceChild("base_s1", CubeListBuilder.create().texOffs(0, 27).mirror()
                .addBox(0, 0, 0, 2, 1, 1), PartPose.offset(-1, -1, 1));
        root.addOrReplaceChild("base_s2", CubeListBuilder.create().texOffs(0, 27).mirror()
                .addBox(0, 0, 0, 2, 1, 1), PartPose.offset(-1, -1, -2));
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 13).mirror()
                .addBox(0, 0, 0, 4, 2, 4), PartPose.offset(-2, -2, -2));
        root.addOrReplaceChild("flames", CubeListBuilder.create().texOffs(5, 7).mirror()
                .addBox(0, 0, 0, 4, 2, 4), PartPose.offset(-2, -2, -2));
        root.addOrReplaceChild("clasp_1a", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(0, 0, 0, 2, 2, 1), PartPose.offset(-1, -2, 2));
        root.addOrReplaceChild("clasp_2a", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(0, 0, 0, 2, 2, 1), PartPose.offset(-1, -2, -3));
        root.addOrReplaceChild("clasp_2b", CubeListBuilder.create().texOffs(0, 7).mirror()
                .addBox(0, 0, 0, 2, 1, 2), PartPose.offset(-1, -1, -5));
        root.addOrReplaceChild("clasp_1b", CubeListBuilder.create().texOffs(0, 7).mirror()
                .addBox(0, 0, 0, 2, 1, 2), PartPose.offset(-1, -1, 3));
        root.addOrReplaceChild("clasp_3a", CubeListBuilder.create().texOffs(0, 3).mirror()
                .addBox(0, 0, 0, 1, 2, 2), PartPose.offset(2, -2, -1));
        root.addOrReplaceChild("clasp_4a", CubeListBuilder.create().texOffs(0, 3).mirror()
                .addBox(0, 0, 0, 1, 2, 2), PartPose.offset(-3, -2, -1));
        root.addOrReplaceChild("clasp_3b", CubeListBuilder.create().texOffs(0, 19).mirror()
                .addBox(0, 0, 0, 2, 1, 2), PartPose.offset(3, -1, -1));
        root.addOrReplaceChild("clasp_4b", CubeListBuilder.create().texOffs(0, 19).mirror()
                .addBox(0, 0, 0, 2, 1, 2), PartPose.offset(-5, -1, -1));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(TrapRenderState state) {
        super.setupAnim(state);
        core.visible = state.trapType == TrapEntity.Type.RIFT;
        flames.visible = state.trapType == TrapEntity.Type.FIRE;
    }
}
