package com.invasion.entity.pathfinding;

import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

import com.invasion.block.BlockMetadata;
import com.invasion.entity.BurrowerEntity;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.util.math.PosRotate3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

@Deprecated
public class BurrowerNavigation extends AbstractParametricNavigator {
    protected Node nextNode;
    private final int segmentCount;
    private final float[] stableSegmentYaw;
    private float stableBodyYaw;
    private final Deque<PosRotate3D> movementHistory = new ArrayDeque<>();
    private static final double SEGMENT_SPACING = 0.20D;
    private static final int MAX_HISTORY_SIZE = 512;
    static final float MAX_TURN_RADIANS = Mth.DEG_TO_RAD * 5.0F;
    private static final double FINAL_NODE_DISTANCE = 0.10D;
    private static final double STEP_SPEED = 0.015D;
    private static final double EDGE_CLEARANCE = 0.02D;
    private Vec3 movementDirection = Vec3.ZERO;
    private StepPhase stepPhase = StepPhase.APPROACH;
    protected float timePerTick = 0.05F;
    protected boolean nodeChanged;

    private enum StepPhase {
        APPROACH, CLIMB, CROSS
    }

    public BurrowerNavigation(BurrowerEntity entity, PathSource pathSource, int segments, int offset) {
        super(entity, pathSource);
        segmentCount = segments;
        stableSegmentYaw = new float[segments];

        actor.setCanDestroyBlocks(true);
        actor.setCanClimb(true);
    }

    @Override
    protected <T extends Entity> Actor<T> createActor(T entity) {
        return new Actor<>(entity) {
            @Override
            public void getSuccessors(BlockGetter worldMap, Node currentNode, PathBuilder pathBuilder) {
                super.getSuccessors(worldMap, currentNode, pathBuilder);

                BlockPos above = currentNode.asBlockPos().above();
                if (isClearClimbingSpace(worldMap, above) && hasClimbingSurface(worldMap, above)) {
                    pathBuilder.addNode(above, PathAction.NONE);
                }

                BlockPos below = currentNode.asBlockPos().below();
                if (isClearClimbingSpace(worldMap, below) && hasClimbingSurface(worldMap, below)) {
                    pathBuilder.addNode(below, PathAction.NONE);
                }

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos surface = currentNode.asBlockPos().relative(direction);
                    BlockPos ledge = surface.above();
                    if (worldMap.getBlockState(surface).blocksMotion()
                            && isClearClimbingSpace(worldMap, ledge)) {
                        pathBuilder.addNode(ledge, PathAction.NONE);
                    }
                }
            }

            private boolean isClearClimbingSpace(BlockGetter worldMap, BlockPos pos) {
                BlockState state = worldMap.getBlockState(pos);
                return state.isAir() || !state.blocksMotion();
            }

            private boolean hasClimbingSurface(BlockGetter worldMap, BlockPos pos) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockState state = worldMap.getBlockState(pos.relative(direction));
                    if (!state.isAir() && state.blocksMotion()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public float getPathNodePenalty(Node prevNode, Node node, BlockGetter worldMap) {
                BlockState block = worldMap.getBlockState(node.asBlockPos());

                float penalty = 0.0F;
                int enclosedLevelSide = 0;

                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                if (!entity.level().getBlockState(mutable.set(node.x, node.y, node.z).move(Direction.DOWN)).isPathfindable(PathComputationType.LAND)) {
                    penalty += 0.3F;
                }
                if (!entity.level().getBlockState(mutable.set(node.x, node.y, node.z).move(Direction.UP)).isPathfindable(PathComputationType.LAND)) {
                    penalty += 2;
                }

                for (Direction offset : Direction.Plane.HORIZONTAL) {
                    if (!entity.level().getBlockState(mutable.set(node.x, node.y, node.z).move(offset)).isPathfindable(PathComputationType.LAND)) {
                        enclosedLevelSide++;
                    }
                }

                if (enclosedLevelSide > 2) {
                    enclosedLevelSide = 2;
                }
                penalty += enclosedLevelSide * 0.5F;

                float factor = !block.isAir() && (!block.isPathfindable(PathComputationType.LAND) || BlockMetadata.getCost(block).isPresent()) ? 1.3F : 1;

                return prevNode.distanceTo(node) * factor * penalty;
            }
        };
    }

    @Override
    protected PosRotate3D entityPositionAtParam(int time) {
        boolean climbingStep = isClimbingStep();
        Vec3 steeringTarget = climbingStep ? stepTarget() : Vec3.atBottomCenterOf(nextNode.asBlockPos());
        Vec3 desiredDirection = steeringTarget.subtract(theEntity.position());
        double stepDistance = nextNode == activeNode
                ? 0
                : climbingStep ? Math.min(timePerTick, STEP_SPEED) : timePerTick;
        movementDirection = steerDirection(
                movementDirection,
                desiredDirection,
                MAX_TURN_RADIANS
        );
        // A fixed speed can orbit a nearby waypoint forever. Reduce the radius,
        // not the turn limit, when the target lies inside the normal turning circle.
        double targetDistance = desiredDirection.length();
        if (targetDistance > 1.0E-6D && targetDistance < timePerTick / MAX_TURN_RADIANS) {
            double alignment = Mth.clamp(movementDirection.dot(desiredDirection.scale(1 / targetDistance)), -1, 1);
            double sine = alignment < 0 ? 1 : Math.sqrt(Math.max(0, 1 - alignment * alignment));
            if (sine > 1.0E-6D) {
                stepDistance = Math.min(stepDistance, 0.8D * targetDistance * MAX_TURN_RADIANS / (2 * sine));
            }
        }
        // Never accumulate a virtual position through collisions or path replacements.
        // The only requested displacement is forward along the bounded heading.
        Vec3 movementPosition = theEntity.position().add(movementDirection.scale(stepDistance));
        double horizontal = Math.sqrt(
                movementDirection.x * movementDirection.x
                        + movementDirection.z * movementDirection.z
        );
        Vector3f rotation = new Vector3f(
                0,
                (float) -Math.atan2(movementDirection.z, movementDirection.x),
                (float) Math.atan2(movementDirection.y, horizontal)
        );
        return new PosRotate3D(movementPosition, rotation);
    }

    private Vec3 stepTarget() {
        Vec3 top = Vec3.atBottomCenterOf(nextNode.asBlockPos());
        Vec3 approach = new Vec3(nextNode.x - activeNode.x, 0, nextNode.z - activeNode.z).normalize();
        // Keep the head's collision box outside the riser, then follow its top.
        double offset = (0.5D + theEntity.getBbWidth() * 0.5D + EDGE_CLEARANCE)
                / Math.max(Math.abs(approach.x), Math.abs(approach.z));
        Vec3 face = top.subtract(approach.scale(offset));
        Vec3 target = stepPhase == StepPhase.APPROACH
                ? new Vec3(face.x, Math.max(activeNode.y, theEntity.getY()), face.z)
                : new Vec3(face.x, nextNode.y + EDGE_CLEARANCE, face.z);
        // Replanning halfway up a riser must not send the head back to its foot.
        if (stepPhase == StepPhase.APPROACH
                && (theEntity.position().distanceTo(target) <= FINAL_NODE_DISTANCE
                    || theEntity.getY() > activeNode.y + FINAL_NODE_DISTANCE)) {
            stepPhase = StepPhase.CLIMB;
            target = new Vec3(face.x, nextNode.y + EDGE_CLEARANCE, face.z);
        }
        if (theEntity.getY() >= nextNode.y) {
            stepPhase = StepPhase.CROSS;
        }
        return stepPhase == StepPhase.CROSS ? top : target;
    }

    private boolean isClimbingStep() {
        return nextNode.y > activeNode.y
                && (nextNode.x != activeNode.x || nextNode.z != activeNode.z);
    }

    @Override
    protected boolean isReadyForNextNode(int ticks) {
        double distance = theEntity.position().distanceTo(
                Vec3.atBottomCenterOf(nextNode.asBlockPos())
        );
        if (isClimbingStep()) {
            // Each riser and tread owns its complete transition, even at a turn.
            return theEntity.getY() >= nextNode.y && distance <= FINAL_NODE_DISTANCE;
        }
        if (path.getNextNodeIndex() + 1 >= path.getNodeCount() - 1) {
            return distance <= FINAL_NODE_DISTANCE;
        }
        return distance <= turnStartDistance();
    }

    @Override
    protected void pathFollow(int time) {
        if (isReadyForNextNode(time)) {
            int nextIndex = path.getNextNodeIndex() + 1;
            if (nextIndex < path.getNodeCount()) {
                timeParam = 0;
                path.setNextNodeIndex(nextIndex);
                activeNode = nextNode;
                stepPhase = StepPhase.APPROACH;
                nextNode = nextIndex + 1 < path.getNodeCount()
                        ? path.getNode(nextIndex + 1)
                        : activeNode;
                nodeChanged = true;
            }
        } else {
            timeParam = time;
        }
    }

    @Override
    protected void doMovementTo(int time) {
        PosRotate3D movePos = entityPositionAtParam(time);
        Vec3 previousPosition = theEntity.position();
        theEntity.move(MoverType.SELF, movePos.position().subtract(previousPosition));
        ((BurrowerEntity) theEntity).setHeadRotation(movePos);

        if (!waitingForNotify
                && path.getNextNodeIndex() >= path.getNodeCount() - 1
                && !path.canReach()) {
            BlockPos digTarget = getNextBlockTowardTarget(activeNode.asBlockPos(), path.getTarget());
            if (((BurrowerEntity) theEntity).tryClearPosition(digTarget, this)) {
                setDoingTaskAndHold();
                return;
            }
        }

        if (nodeChanged) {
            ((BurrowerEntity) theEntity).setHeadRotation(movePos);
            nodeChanged = false;
        }

        // Feed the existing follow logic the path actually travelled, including clipping.
        if (theEntity.position().distanceToSqr(previousPosition) > 1.0E-10D) {
            updateSegments(new PosRotate3D(theEntity.position(), movePos.rotation()));
            timeParam = time;
            ticksStuck--;
        } else {
            ticksStuck++;
        }
    }

    private BlockPos getNextBlockTowardTarget(BlockPos from, BlockPos target) {
        int deltaX = target.getX() - from.getX();
        int deltaY = target.getY() - from.getY();
        int deltaZ = target.getZ() - from.getZ();
        int absX = Math.abs(deltaX);
        int absY = Math.abs(deltaY);
        int absZ = Math.abs(deltaZ);

        if (absY > absX && absY > absZ) {
            return from.offset(0, Integer.signum(deltaY), 0);
        }
        if (absX >= absZ) {
            return from.offset(Integer.signum(deltaX), 0, 0);
        }
        return from.offset(0, 0, Integer.signum(deltaZ));
    }

    private void updateSegments(PosRotate3D headPosition) {
        movementHistory.addLast(headPosition);
        while (movementHistory.size() > MAX_HISTORY_SIZE) {
            movementHistory.removeFirst();
        }

        PosRotate3D[] history = movementHistory.toArray(PosRotate3D[]::new);
        PosRotate3D[] sampledSegments = new PosRotate3D[segmentCount + 1];
        for (int i = 0; i < sampledSegments.length; i++) {
            sampledSegments[i] =
                    sampleHistoryAtDistance(history, (i + 1) * SEGMENT_SPACING);
        }

        Vec3 headTangent = headPosition.position().subtract(sampledSegments[1].position());
        double headHorizontal =
                Math.sqrt(headTangent.x * headTangent.x + headTangent.z * headTangent.z);
        if (headHorizontal > Math.max(0.02D, Math.abs(headTangent.y) * 0.2D)) {
            stableBodyYaw = Mth.rotLerp(0.35F, stableBodyYaw,
                    (float) -Math.atan2(headTangent.z, headTangent.x));
        }

        for (int i = 0; i < segmentCount; i++) {
            Vec3 pointAhead = i == 0
                    ? headPosition.position()
                    : sampledSegments[i - 1].position();
            Vec3 pointBehind = sampledSegments[i + 1].position();
            Vector3f rotation = rotationAlong(pointAhead.subtract(pointBehind), i);
            ((BurrowerEntity) theEntity).setSegment(
                    i,
                    new PosRotate3D(sampledSegments[i].position(), rotation)
            );
        }
    }

    private Vector3f rotationAlong(Vec3 direction, int segmentIndex) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double vertical = Math.abs(direction.y);
        boolean isVertical = horizontal <= Math.max(0.02D, vertical * 0.2D);
        if (!isVertical) {
            float targetYaw = (float) -Math.atan2(direction.z, direction.x);
            stableSegmentYaw[segmentIndex] =
                    Mth.rotLerp(0.35F, stableSegmentYaw[segmentIndex], targetYaw);
            stableBodyYaw = Mth.rotLerp(0.35F, stableBodyYaw, targetYaw);
        }
        return new Vector3f(
                0,
                isVertical ? stableBodyYaw : stableSegmentYaw[segmentIndex],
                isVertical
                        ? Math.copySign(Mth.HALF_PI, (float) direction.y)
                        : (float) Math.atan2(direction.y, horizontal)
        );
    }

    private PosRotate3D sampleHistoryAtDistance(PosRotate3D[] history, double targetDistance) {
        double distance = 0;
        for (int newerIndex = history.length - 1; newerIndex > 0; newerIndex--) {
            PosRotate3D newer = history[newerIndex];
            PosRotate3D older = history[newerIndex - 1];
            double stepDistance = newer.position().distanceTo(older.position());
            if (stepDistance < 1.0E-6D) {
                continue;
            }
            if (distance + stepDistance >= targetDistance) {
                float progress = (float) ((targetDistance - distance) / stepDistance);
                return newer.lerp(progress, older);
            }
            distance += stepDistance;
        }
        return history[0];
    }

    @Override
    public boolean isIdle() {
        return path == null || path.getNextNodeIndex() >= path.getNodeCount() - 1;
    }

    @Override
    public boolean startMovingAlong(net.minecraft.world.level.pathfinder.Path newPath, double speed) {
        if (newPath == null || newPath.getNodeCount() < 2) {
            path = null;
            return false;
        }

        path = newPath;
        path.setNextNodeIndex(0);
        activeNode = path.getNode(0);
        nextNode = path.getNode(1);
        stepPhase = StepPhase.APPROACH;
        if (movementDirection.lengthSqr() < 1.0E-6D) {
            // Spawned entities already have a heading; a new path must not snap it.
            double yaw = theEntity.getYRot() * Mth.DEG_TO_RAD;
            movementDirection = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        timeParam = 0;
        nodeActionFinished = ActionablePathNode.getAction(activeNode) == PathAction.NONE;
        ticksStuck = 0;

        if (noSunPathfind) {
            removeSunnyPath();
        }

        return true;
    }

    private double turnStartDistance() {
        int followingIndex = path.getNextNodeIndex() + 2;
        // Finish gaining height before rounding a ledge. Cutting this corner early
        // pushes the head into the supporting wall while it is still below the top.
        if (nextNode.y > activeNode.y && path.getNode(followingIndex).y <= nextNode.y) {
            return FINAL_NODE_DISTANCE;
        }
        Vec3 incoming = Vec3.atCenterOf(nextNode.asBlockPos())
                .subtract(Vec3.atCenterOf(activeNode.asBlockPos()))
                .normalize();
        Vec3 outgoing = Vec3.atCenterOf(path.getNode(followingIndex).asBlockPos())
                .subtract(Vec3.atCenterOf(nextNode.asBlockPos()))
                .normalize();
        double angle = Math.acos(Mth.clamp(incoming.dot(outgoing), -1.0D, 1.0D));
        double turnRadius = timePerTick / MAX_TURN_RADIANS;
        // Short edges and reversals must not consume distant waypoints immediately.
        double edgeLength = activeNode.distanceTo(nextNode);
        return Math.max(FINAL_NODE_DISTANCE,
                Math.min(edgeLength * 0.5D, turnRadius * Math.tan(angle * 0.5D)));
    }

    static Vec3 steerDirection(Vec3 current, Vec3 desired, float maxTurnRadians) {
        Vec3 target = desired.normalize();
        if (target.lengthSqr() < 1.0E-6D) {
            return current.normalize();
        }
        Vec3 heading = current.normalize();
        if (heading.lengthSqr() < 1.0E-6D) {
            return target;
        }

        double dot = Mth.clamp(heading.dot(target), -1.0D, 1.0D);
        double angle = Math.acos(dot);
        if (angle <= maxTurnRadians) {
            return target;
        }

        Vec3 axis = heading.cross(target);
        if (axis.lengthSqr() < 1.0E-6D) {
            axis = heading.cross(Math.abs(heading.y) < 0.9D
                    ? new Vec3(0, 1, 0)
                    : new Vec3(1, 0, 0));
        }
        axis = axis.normalize();
        double cosine = Math.cos(maxTurnRadians);
        double sine = Math.sin(maxTurnRadians);
        return heading.scale(cosine)
                .add(axis.cross(heading).scale(sine))
                .add(axis.scale(axis.dot(heading) * (1.0D - cosine)))
                .normalize();
    }
}
