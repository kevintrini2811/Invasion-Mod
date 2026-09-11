package com.invasion.entity.pathfinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invasion.entity.BurrowerEntity;
import com.invasion.util.math.PosRotate3D;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class BurrowerNavigationTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void activeNavigationDoesNotReceiveASecondVanillaTravelStep() {
        BurrowerEntity entity = mock(BurrowerEntity.class);
        when(entity.level()).thenReturn(mock(ServerLevel.class));
        when(entity.getNavigation()).thenReturn(mock(PathNavigation.class));
        doCallRealMethod().when(entity).travel(any(Vec3.class));

        entity.travel(new Vec3(1, 0, 1));

        verify(entity).setDeltaMovement(Vec3.ZERO);
    }

    @Test
    void verticalTargetRetainsBoundedThreeDimensionalSteering() {
        MovementFixture fixture = new MovementFixture(new Vec3(0.5, 64, 0.5));
        fixture.navigation.startMovingAlong(new Path(List.of(
                new Node(0, 64, 0), new Node(0, 68, 0)), new BlockPos(0, 68, 0), true), 1);
        Vec3 heading = new Vec3(1, 0, 0);
        for (int tick = 0; tick < 200 && !fixture.navigation.isIdle(); tick++) {
            fixture.step();
            if (fixture.requestedMovement.lengthSqr() < EPSILON) {
                continue;
            }
            Vec3 nextHeading = fixture.requestedMovement.normalize();
            assertTrue(Math.acos(Math.clamp(heading.dot(nextHeading), -1, 1))
                    <= BurrowerNavigation.MAX_TURN_RADIANS + EPSILON);
            heading = nextHeading;
        }
        assertTrue(fixture.navigation.isIdle());
        assertTrue(fixture.position.distanceTo(new Vec3(0.5, 68, 0.5)) <= 0.1);
    }

    @Test
    void navigationTurnsOnGroundAndReachesEndWithoutCuttingDiagonally() {
        MovementFixture fixture = new MovementFixture(new Vec3(0.5, 64, 0.5));
        fixture.navigation.startMovingAlong(new Path(List.of(
                new Node(0, 64, 0), new Node(1, 64, 0),
                new Node(2, 64, 0), new Node(2, 64, 1),
                new Node(2, 64, 2), new Node(2, 64, 3)),
                new BlockPos(2, 64, 3), true), 1);
        Vec3 previousDirection = new Vec3(1, 0, 0);
        int turningTicks = 0;
        for (int tick = 0; tick < 200 && !fixture.navigation.isIdle(); tick++) {
            Vec3 before = fixture.position;
            fixture.step();
            Vec3 displacement = fixture.position.subtract(before);
            if (displacement.lengthSqr() < EPSILON) {
                continue;
            }
            assertEquals(0.05, displacement.length(), EPSILON);
            assertEquals(64, fixture.position.y, EPSILON);
            Vec3 heading = displacement.normalize();
            double angle = Math.acos(Math.clamp(previousDirection.dot(heading), -1, 1));
            assertTrue(angle <= BurrowerNavigation.MAX_TURN_RADIANS + EPSILON);
            if (heading.z > 0.01 && turningTicks++ == 0) {
                assertTrue(heading.x > 0.99, "The first turn step must still point mostly forward");
            }
            previousDirection = heading;
        }
        assertTrue(turningTicks >= 18, "The corner must span multiple forward steps");
        assertTrue(fixture.navigation.isIdle(), "Ground-level navigation must finish");
        assertTrue(fixture.position.distanceTo(new Vec3(2.5, 64, 3.5)) <= 0.1);
    }

    @Test
    void blockedMovementDoesNotAccumulateCatchUpDisplacement() {
        MovementFixture fixture = new MovementFixture(new Vec3(0.2, 64, 0.7));
        fixture.navigation.startMovingAlong(straightPath(), 1);
        fixture.allowedMovement = 0;
        for (int tick = 0; tick < 30; tick++) {
            fixture.step();
            assertEquals(0.05, fixture.requestedMovement.length(), EPSILON);
        }
        fixture.allowedMovement = 1;
        Vec3 before = fixture.position;
        fixture.step();
        assertEquals(0.05, fixture.position.distanceTo(before), EPSILON);
    }

    @Test
    void segmentsSampleClippedMovementInsteadOfUnreachedPosition() {
        MovementFixture fixture = new MovementFixture(new Vec3(0.5, 64, 0.5));
        fixture.navigation.startMovingAlong(straightPath(), 1);
        fixture.allowedMovement = 0.5;
        fixture.step();
        assertEquals(fixture.position, fixture.firstSegment.position());
    }

    @Test
    void replacingPathPreservesHeadingAndDoesNotSnapToBlockCenter() {
        MovementFixture fixture = new MovementFixture(new Vec3(0.2, 64, 0.7));
        fixture.navigation.startMovingAlong(straightPath(), 1);
        fixture.step();
        Vec3 heading = fixture.requestedMovement.normalize();
        fixture.navigation.startMovingAlong(new Path(List.of(
                new Node(0, 64, 0), new Node(0, 64, 1)), new BlockPos(0, 64, 1), true), 1);
        Vec3 before = fixture.position;
        fixture.step();
        assertEquals(0.05, fixture.position.distanceTo(before), EPSILON);
        assertTrue(Math.acos(heading.dot(fixture.requestedMovement.normalize()))
                <= BurrowerNavigation.MAX_TURN_RADIANS + EPSILON);
    }

    private static Path straightPath() {
        return new Path(List.of(new Node(0, 64, 0), new Node(1, 64, 0)),
                new BlockPos(1, 64, 0), true);
    }

    private static final class MovementFixture {
        private Vec3 position;
        private Vec3 requestedMovement;
        private PosRotate3D firstSegment;
        private double allowedMovement = 1;
        private final BurrowerNavigation navigation;

        private MovementFixture(Vec3 initialPosition) {
            position = initialPosition;
            BurrowerEntity entity = mock(BurrowerEntity.class);
            when(entity.position()).thenAnswer(invocation -> position);
            when(entity.getYRot()).thenReturn(-90F);
            doAnswer(invocation -> {
                requestedMovement = invocation.getArgument(1);
                position = position.add(requestedMovement.scale(allowedMovement));
                return null;
            }).when(entity).move(eq(MoverType.SELF), any(Vec3.class));
            doAnswer(invocation -> {
                firstSegment = invocation.getArgument(1);
                return null;
            }).when(entity).setSegment(eq(0), any(PosRotate3D.class));
            navigation = new BurrowerNavigation(entity, mock(PathSource.class), 16, -4);
        }

        private void step() {
            navigation.pathFollow(navigation.timeParam + 1);
            navigation.doMovementTo(navigation.timeParam);
        }
    }

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
