package com.invasion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * A state-neutral humanoid helmet mesh that can follow any mob model's head.
 */
public final class HeadArmorModel<S extends LivingEntityRenderState>
        extends EntityModel<S> {
    public final ModelPart head;

    public HeadArmorModel(ModelPart root) {
        super(root);
        head = root.getChild(PartNames.HEAD);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                PartNames.HEAD,
                CubeListBuilder.create().texOffs(0, 0).addBox(
                        -4.0F, -8.0F, -4.0F,
                        8.0F, 8.0F, 8.0F,
                        new CubeDeformation(1.0F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
