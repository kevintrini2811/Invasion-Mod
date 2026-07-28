package com.invasion.client.render;

import com.invasion.entity.BurrowerEntity;
import com.invasion.util.math.PosRotate3D;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BurrowerModel extends EntityModel<BurrowerRenderState> {
    private static final double POSITION_SCALE = 16.0D / 2.2D;
    private static final Vec3 POSITION_TRANSFORM =
            new Vec3(-POSITION_SCALE, -POSITION_SCALE, POSITION_SCALE);

    private final ModelPart[] parts =
            new ModelPart[BurrowerEntity.NUMBER_OF_SEGMENTS + 1];

    public BurrowerModel(ModelPart root) {
        super(root);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = root.getChild("segment_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("segment_0",
                CubeListBuilder.create().addBox(-1.0F, -3.0F, -3.0F, 2.0F, 6.0F, 6.0F).mirror(),
                PartPose.ZERO);
        for (int i = 1; i <= BurrowerEntity.NUMBER_OF_SEGMENTS; i++) {
            float radius = i % 2 == 1 ? 3.5F : 2.5F;
            root.addOrReplaceChild("segment_" + i,
                    CubeListBuilder.create()
                            .addBox(-0.5F, -radius, -radius, 2.0F, radius * 2.0F, radius * 2.0F)
                            .mirror(),
                    PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(BurrowerRenderState state) {
        super.setupAnim(state);
        if (!state.hasTrackedSegments) {
            setupFallbackPose(state);
            return;
        }

        Vec3 origin = state.segments[0].position();
        for (int i = 0; i < parts.length; i++) {
            PosRotate3D segment = state.segments[i];
            Vec3 position = segment.position().subtract(origin).multiply(POSITION_TRANSFORM);
            parts[i].setPos((float) position.x, (float) position.y, (float) position.z);
            parts[i].setRotation(segment.rotation().x(),
                    segment.rotation().y(),
                    segment.rotation().z());
        }
    }

    private void setupFallbackPose(BurrowerRenderState state) {
        parts[0].setPos(0.0F, 0.0F, 0.0F);
        parts[0].setRotation(0.0F, 0.0F, 0.0F);
        for (int i = 1; i < parts.length; i++) {
            float phase = state.ageInTicks * 0.12F - i * 0.35F;
            parts[i].setPos(i * 3.0F,
                    Mth.sin(phase) * 0.35F,
                    Mth.sin(phase) * 0.8F);
            parts[i].setRotation(0.0F,
                    Mth.cos(phase) * 0.08F,
                    Mth.cos(phase) * 0.04F);
        }
    }
}
