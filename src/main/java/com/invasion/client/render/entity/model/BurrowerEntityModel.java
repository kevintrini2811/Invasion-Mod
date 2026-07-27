package com.invasion.client.render.entity.model;

import org.joml.Vector3f;

import com.invasion.entity.BurrowerEntity;
import com.invasion.util.math.PosRotate3D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.phys.Vec3;

public class BurrowerEntityModel extends EntityModel<BurrowerEntity> {
    private static final double POSITION_SCALE = 7.269999980926514D;
    private static final Vec3 POSITION_TRANSFORM = new Vec3(-POSITION_SCALE, -POSITION_SCALE, POSITION_SCALE);

    private final ModelPart head;
    private final ModelPart evenSegment;
    private final ModelPart oddSegment;

    private PosRotate3D[] segments = {};

    public BurrowerEntityModel(ModelPart root) {
        head = root.getChild(root.hasChild("head") ? "head" : "segment");
        evenSegment = root.hasChild("even_segment") ? root.getChild("even_segment") : head;
        oddSegment = root.hasChild("odd_segment") ? root.getChild("odd_segment") : head;
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        root.addOrReplaceChild("segment", CubeListBuilder.create().addBox(-2, -2.5F, -2.5F, 4, 5, 5).mirror(), PartPose.ZERO);
        return LayerDefinition.create(data, 64, 32);
    }

    public static LayerDefinition getTexturedModelData2() {
        MeshDefinition data = new MeshDefinition();
        PartDefinition root = data.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().addBox(-1, -3, -3, 2, 6, 6).mirror(), PartPose.ZERO);
        root.addOrReplaceChild("even_segment", CubeListBuilder.create().addBox(-0.5F, -3.5F, -3.5F, 2, 7, 7).mirror(), PartPose.ZERO);
        root.addOrReplaceChild("odd_segment", CubeListBuilder.create().addBox(-0.5F, -2.5F, -2.5F, 2, 5, 5).mirror(), PartPose.ZERO);
        return LayerDefinition.create(data, 64, 32);
    }

    @Override
    public void animateModel(BurrowerEntity entity, float limbAngle, float limbDistance, float tickDelta) {
        segments = new PosRotate3D[17];

        segments[0] = new PosRotate3D(
            entity.position().multiply(POSITION_TRANSFORM),
            PosRotate3D.lerp(tickDelta, entity.getPrevRotation(), entity.getRotation(), new Vector3f())
        );

        for (int i = 0; i < 16; i++) {
            segments[(i + 1)] = entity.getSegments3DLastTick()[i].lerp(tickDelta, entity.getSegments3D()[i]).multiplyPosition(POSITION_TRANSFORM);
        }
    }

    protected ModelPart getPart(int i) {
        if (i == 0) {
            return head;
        }
        return i % 2 == 0 ? evenSegment : oddSegment;
    }

    @Override
    public void setAngles(BurrowerEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        for (int i = 0; i < segments.length; i++) {
            ModelPart segment = getPart(i);
            segment.setPos((float) segments[i].position().x, (float) segments[i].position().y, (float) segments[i].position().z);
            segment.setRotation(segments[i].rotation().x(), segments[i].rotation().y(), segments[i].rotation().z());
            segment.render(matrices, vertices, light, overlay, color);
        }
    }
}