package com.invasion.client.render;

import com.invasion.entity.BurrowerEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public final class BurrowerModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart[] segments = new ModelPart[BurrowerEntity.NUMBER_OF_SEGMENTS];

    public BurrowerModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        for (int i = 0; i < segments.length; i++) {
            segments[i] = root.getChild("segment_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -3.0F, -2.0F, 6.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 20.0F, -2.0F));
        for (int i = 0; i < BurrowerEntity.NUMBER_OF_SEGMENTS; i++) {
            float radius = i % 2 == 0 ? 3.5F : 2.5F;
            root.addOrReplaceChild("segment_" + i,
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-radius, -radius, -1.0F,
                                    radius * 2.0F, radius * 2.0F, 2.0F),
                    PartPose.offset(0.0F, 20.0F, i * 1.45F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float movement = Math.min(1.0F, state.walkAnimationSpeed * 2.0F);
        head.yRot = Mth.sin(state.ageInTicks * 0.22F) * 0.08F * movement;
        for (int i = 0; i < segments.length; i++) {
            ModelPart segment = segments[i];
            float phase = state.ageInTicks * 0.22F - i * 0.42F;
            segment.x = Mth.sin(phase) * 1.7F * movement;
            segment.y = 20.0F + Mth.cos(phase * 0.8F) * 0.45F * movement;
            segment.yRot = Mth.sin(phase) * 0.22F * movement;
        }
    }
}
