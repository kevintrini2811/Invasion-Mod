package com.invasion.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.invasion.util.math.PosRotate3D;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BurrowerModelTest {
    @Test
    void verticalQuarterCirclesFollowTangentsInEveryHorizontalDirection() {
        ModelPart root = BurrowerModel.createBodyLayer().bakeRoot();
        BurrowerModel model = new BurrowerModel(root);
        for (int bearing = 0; bearing < 8; bearing++) {
            double yaw = bearing * Math.PI / 4;
            for (int sign : new int[] {-1, 1}) {
                for (boolean descending : new boolean[] {false, true}) {
                    BurrowerRenderState state = new BurrowerRenderState();
                    state.hasTrackedSegments = true;
                    for (int i = 0; i < state.segments.length; i++) {
                        int step = descending ? state.segments.length - 1 - i : i;
                        double pitch = sign * step * Math.PI / 32;
                        state.segments[i] = new PosRotate3D(
                                new Vec3(Math.cos(yaw) * Math.sin(pitch),
                                        64 - Math.cos(pitch), Math.sin(yaw) * Math.sin(pitch)),
                                new Vector3f(0, (float) -yaw, (float) pitch));
                    }
                    model.setupAnim(state);
                    for (int i = 0; i < state.segments.length; i++) {
                        ModelPart part = root.getChild("segment_" + i);
                        Vector3f normal = new Vector3f(1, 0, 0).rotate(
                                new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
                        normal.mul(-1, -1, 1);
                        double pitch = state.segments[i].rotation().z();
                        double alignment = normal.x * Math.cos(yaw) * Math.cos(pitch)
                                + normal.y * Math.sin(pitch)
                                + normal.z * Math.sin(yaw) * Math.cos(pitch);
                        assertEquals(1, Math.abs(alignment), 1.0E-6,
                                "Vertical fan must follow the tangent at bearing " + bearing);
                    }
                }
            }
        }
    }

    @Test
    void segmentPlateNormalsFollowTangentsAroundBothQuarterCircles() {
        ModelPart root = BurrowerModel.createBodyLayer().bakeRoot();
        BurrowerModel model = new BurrowerModel(root);
        for (int turnSign : new int[] {-1, 1}) {
            BurrowerRenderState state = new BurrowerRenderState();
            state.hasTrackedSegments = true;
            for (int i = 0; i < state.segments.length; i++) {
                double angle = turnSign * i * Math.PI / 32;
                state.segments[i] = new PosRotate3D(
                        new Vec3(Math.sin(angle), 64, -Math.cos(angle)),
                        new Vector3f(0, (float) -angle, 0));
            }
            model.setupAnim(state);

            for (int i = 0; i < state.segments.length; i++) {
                ModelPart part = root.getChild("segment_" + i);
                Vector3f normal = new Vector3f(1, 0, 0).rotate(
                        new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
                // Convert the plate normal back through the renderer's mirrored axes.
                normal.mul(-1, -1, 1);
                double angle = turnSign * i * Math.PI / 32;
                double alignment = normal.x * Math.cos(angle) + normal.z * Math.sin(angle);
                assertEquals(1, Math.abs(alignment), 1.0E-6,
                        "Each plate must face along its local tangent, not across the curve");
            }
        }
    }

    @Test
    void horizontalBodyRestsAboveFootHeight() {
        ModelPart root = BurrowerModel.createBodyLayer().bakeRoot();
        BurrowerModel model = new BurrowerModel(root);
        BurrowerRenderState state = new BurrowerRenderState();
        state.hasTrackedSegments = true;
        for (int i = 0; i < state.segments.length; i++) {
            state.segments[i] = new PosRotate3D(new Vec3(-i * 0.2, 64, 0), new Vector3f());
        }
        model.setupAnim(state);

        ModelPart widestSegment = root.getChild("segment_1");
        assertEquals(0, widestSegment.y + 3.5F, 1.0E-6,
                "The lower face must meet foot height after the model Y reflection");
        assertEquals(-3.5F, root.getChild("segment_0").y, 1.0E-6);
    }
}
