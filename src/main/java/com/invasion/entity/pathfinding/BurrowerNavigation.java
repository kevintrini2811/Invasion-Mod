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
    protected Node prevNode;

    private final int segmentCount;
    private final int segmentDelay;
    private final Deque<PosRotate3D> movementHistory = new ArrayDeque<>();
    protected float timePerTick = 0.05F;
    protected boolean nodeChanged;

    public BurrowerNavigation(BurrowerEntity entity, PathSource pathSource, int segments, int offset) {
        super(entity, pathSource);
        segmentCount = segments;
        segmentDelay = Math.max(1, Math.abs(offset));

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
        float progress = Mth.clamp(time * timePerTick, 0, 1);
        int activeIndex = path.getNextNodeIndex();
        Node followingNode = activeIndex + 2 < path.getNodeCount()
                ? path.getNode(activeIndex + 2)
                : nextNode;
        return interpolatePathEdge(progress, prevNode, activeNode, nextNode, followingNode);
    }

    @Override
    protected boolean isReadyForNextNode(int ticks) {
        return ticks * timePerTick >= 1;
    }

    @Override
    protected void pathFollow(int time) {
        if (isReadyForNextNode(time)) {
            int nextIndex = path.getNextNodeIndex() + 1;
            if (nextIndex < path.getNodeCount()) {
                timeParam = 0;
                path.setNextNodeIndex(nextIndex);
                prevNode = activeNode;
                activeNode = nextNode;
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
        theEntity.move(MoverType.SELF, movePos.position().subtract(theEntity.position()));
        ((BurrowerEntity) theEntity).setHeadRotation(movePos);

        if (nodeChanged) {
            ((BurrowerEntity) theEntity).setHeadRotation(movePos);
            nodeChanged = false;
        }

        if (theEntity.distanceToSqr(movePos.position()) < minMoveToleranceSq) {
            updateSegments(movePos);
            timeParam = time;
            ticksStuck--;
        } else {
            ticksStuck++;
        }
    }

    private void updateSegments(PosRotate3D headPosition) {
        movementHistory.addLast(headPosition);
        int historyLimit = segmentCount * segmentDelay + 1;
        while (movementHistory.size() > historyLimit) {
            movementHistory.removeFirst();
        }

        PosRotate3D[] history = movementHistory.toArray(PosRotate3D[]::new);
        for (int i = 0; i < segmentCount; i++) {
            int historyIndex = Math.max(0, history.length - 1 - (i + 1) * segmentDelay);
            ((BurrowerEntity) theEntity).setSegment(i, history[historyIndex]);
        }
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
        prevNode = activeNode;
        nextNode = path.getNode(1);
        timeParam = 0;
        nodeActionFinished = ActionablePathNode.getAction(activeNode) == PathAction.NONE;
        ticksStuck = 0;

        if (noSunPathfind) {
            removeSunnyPath();
        }

        return true;
    }

    private PosRotate3D interpolatePathEdge(
            float progress,
            Node previous,
            Node start,
            Node end,
            Node following
    ) {
        Vec3 p0 = Vec3.atCenterOf(start.asBlockPos());
        Vec3 p1 = Vec3.atCenterOf(end.asBlockPos());
        Vec3 tangent0 = Vec3.atCenterOf(end.asBlockPos())
                .subtract(Vec3.atCenterOf(previous.asBlockPos()))
                .scale(0.5D);
        Vec3 tangent1 = Vec3.atCenterOf(following.asBlockPos())
                .subtract(Vec3.atCenterOf(start.asBlockPos()))
                .scale(0.5D);

        double t = progress;
        double t2 = t * t;
        double t3 = t2 * t;
        Vec3 position = p0.scale(2 * t3 - 3 * t2 + 1)
                .add(tangent0.scale(t3 - 2 * t2 + t))
                .add(p1.scale(-2 * t3 + 3 * t2))
                .add(tangent1.scale(t3 - t2));

        Vec3 direction = p0.scale(6 * t2 - 6 * t)
                .add(tangent0.scale(3 * t2 - 4 * t + 1))
                .add(p1.scale(-6 * t2 + 6 * t))
                .add(tangent1.scale(3 * t2 - 2 * t));
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        Vector3f rotation = new Vector3f(
                0,
                (float) -Math.atan2(direction.z, direction.x),
                (float) Math.atan2(direction.y, horizontal)
        );
        return new PosRotate3D(position, rotation);
    }
}
