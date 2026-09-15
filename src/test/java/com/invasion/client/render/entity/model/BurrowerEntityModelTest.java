package com.invasion.client.render.entity.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.invasion.util.math.PosRotate3D;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BurrowerEntityModelTest {
    @Test
    void horizontalBodyRestsAboveFootHeight() {
        ModelPart part = BurrowerEntityModel.getTexturedModelData2().bakeRoot().getChild("odd_segment");
        BurrowerEntityModel.applyPose(part, new PosRotate3D(Vec3.ZERO, new Vector3f()));

        assertEquals(-BurrowerEntityModel.BODY_RADIUS, part.y, 1.0E-6);
    }

    @Test
    void pitchedTurnFollowsLocalSegmentHeading() {
        ModelPart part = BurrowerEntityModel.getTexturedModelData2().bakeRoot().getChild("odd_segment");
        float yaw = (float) (Math.PI / 4);
        float pitch = (float) (Math.PI / 3);
        BurrowerEntityModel.applyPose(part,
                new PosRotate3D(Vec3.ZERO, new Vector3f(0, -yaw, pitch)));

        Vector3f normal = new Vector3f(1, 0, 0).rotate(
                new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
        normal.mul(-1, -1, 1);
        double alignment = normal.x * Math.cos(yaw) * Math.cos(pitch)
                + normal.y * Math.sin(pitch)
                + normal.z * Math.sin(yaw) * Math.cos(pitch);
        assertEquals(1, Math.abs(alignment), 1.0E-6);
    }
}
