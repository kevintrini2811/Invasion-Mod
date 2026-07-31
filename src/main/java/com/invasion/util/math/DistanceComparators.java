package com.invasion.util.math;

import java.util.Comparator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface DistanceComparators {
    static Comparator<Entity> ofComparisonEntities(double x, double y, double z) {
        return ofComparisonEntities(new Vec3(x, y, z));
    }

    static Comparator<Entity> ofComparisonEntities(Vec3 origin) {
        return Comparator.comparingDouble(e -> e.distanceToSqr(origin));
    }
}