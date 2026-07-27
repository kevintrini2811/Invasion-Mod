package com.invasion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class EggModel extends EntityModel<EntityRenderState> {
    public EggModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        PartDefinition egg = root.addOrReplaceChild("egg", CubeListBuilder.create(),
                PartPose.offset(-4.5F, 0, -4.5F));
        egg.addOrReplaceChild("top", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(1F, 0F, 1F, 7, 1, 7)
                .texOffs(0, 8).addBox(2F, -11F, 2F, 5, 1, 5)
                .texOffs(28, 23).addBox(1F, -10F, 2F, 1, 3, 6)
                .texOffs(0, 24).addBox(1F, -10F, 1F, 6, 3, 1)
                .texOffs(28, 23).addBox(7F, -10F, 1F, 1, 3, 6)
                .texOffs(0, 24).addBox(2F, -10F, 7F, 6, 3, 1)
                .texOffs(10, 22).addBox(0F, -7F, 1F, 1, 2, 8)
                .texOffs(0, 21).addBox(0F, -7F, 0F, 8, 2, 1)
                .texOffs(10, 22).addBox(8F, -7F, 0F, 1, 2, 8)
                .texOffs(0, 21).addBox(1F, -7F, 8F, 8, 2, 1)
                .texOffs(20, 10).addBox(-1F, -5F, 0F, 1, 4, 9)
                .texOffs(0, 16).addBox(0F, -5F, -1F, 9, 4, 1)
                .texOffs(20, 10).addBox(9F, -5F, 0F, 1, 4, 9)
                .texOffs(0, 16).addBox(0F, -5F, 9F, 9, 4, 1)
                .texOffs(28, 0).addBox(0F, -1F, 1F, 1, 1, 8)
                .texOffs(0, 14).addBox(0F, -1F, 0F, 8, 1, 1)
                .texOffs(28, 0).addBox(8F, -1F, 0F, 1, 1, 8), PartPose.ZERO);
        egg.addOrReplaceChild("bottom", CubeListBuilder.create().mirror().texOffs(0, 14)
                .addBox(0F, 0F, 0F, 8, 1, 1), PartPose.offset(1F, -1F, 8F));
        return LayerDefinition.create(data, 64, 32);
    }
}
