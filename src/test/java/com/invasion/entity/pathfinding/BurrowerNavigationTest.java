package com.invasion.entity.pathfinding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class BurrowerNavigationTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void ninetyDegreeTurnAdvancesAlongCurrentHeadingWhileYawChangesGradually() {
        Vec3 direction = new Vec3(1, 0, 0);
        Vec3 position = Vec3.ZERO;

        direction = BurrowerNavigation.steerDirection(
                direction,
                new Vec3(0, 0, 1),
                BurrowerNavigation.MAX_TURN_RADIANS
        );
        position = position.add(direction.scale(0.05D));

        assertEquals(Math.cos(BurrowerNavigation.MAX_TURN_RADIANS), direction.x, EPSILON);
        assertEquals(Math.sin(BurrowerNavigation.MAX_TURN_RADIANS), direction.z, EPSILON);
        assertEquals(0.05D, position.length(), EPSILON);
        assertEquals(BurrowerNavigation.MAX_TURN_RADIANS,
                Math.atan2(direction.z, direction.x), EPSILON);
    }

    @Test
    void repeatedSteeringProducesQuarterCircleInsteadOfDiagonalShortcut() {
        Vec3 direction = new Vec3(1, 0, 0);
        Vec3 position = Vec3.ZERO;

        for (int tick = 0; tick < 18; tick++) {
            direction = BurrowerNavigation.steerDirection(
                    direction,
                    new Vec3(0, 0, 1),
                    BurrowerNavigation.MAX_TURN_RADIANS
            );
            position = position.add(direction.scale(0.05D));
        }

        assertEquals(0, direction.x, EPSILON);
        assertEquals(1, direction.z, EPSILON);
        double chord = 0.05D
                * Math.sin(9 * BurrowerNavigation.MAX_TURN_RADIANS)
                / Math.sin(BurrowerNavigation.MAX_TURN_RADIANS * 0.5D);
        assertEquals(chord * Math.cos(9.5D * BurrowerNavigation.MAX_TURN_RADIANS),
                position.x, EPSILON);
        assertEquals(chord * Math.sin(9.5D * BurrowerNavigation.MAX_TURN_RADIANS),
                position.z, EPSILON);
    }
}
