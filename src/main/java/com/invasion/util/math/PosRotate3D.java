package com.invasion.util.math;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record PosRotate3D (Vec3 position, Vector3fc rotation) {
    public static final PosRotate3D ZERO = new PosRotate3D(new Vec3(0, 0, 0), new Vector3f());

    public PosRotate3D lerp(float delta, PosRotate3D b) {
        return new PosRotate3D(lerp(delta, position, b.position()), lerp(delta, rotation, b.rotation(), new Vector3f()));
    }

    public PosRotate3D multiplyPosition(Vec3 positionMul) {
        return new PosRotate3D(position.multiply(positionMul), rotation);
    }

    public static Vec3 lerp(float delta, Vec3 a, Vec3 b) {
        return new Vec3(
                Mth.lerp(delta, a.x, b.x),
                Mth.lerp(delta, a.y, b.y),
                Mth.lerp(delta, a.z, b.z)
        );
    }

    public static Vector3fc lerp(float delta, Vector3fc a, Vector3fc b, Vector3f into) {
        return into.set(
                Mth.rotLerp(delta, a.x(), b.x()),
                Mth.rotLerp(delta, a.y(), b.y()),
                Mth.rotLerp(delta, a.z(), b.z())
        );
    }
}
