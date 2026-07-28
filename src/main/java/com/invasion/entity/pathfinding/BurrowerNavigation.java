package com.invasion.entity.pathfinding;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import com.invasion.block.BlockMetadata;
import com.invasion.entity.BurrowerEntity;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.util.math.PosRotate3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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

    protected Node[] prevSegmentNodes;
    protected Node[] activeSegmentNodes;
    protected Node[] nextSegmentNodes;

    protected int[] segmentPathIndices;
    protected int[] segmentTime;
    protected int[] segmentOffsets;
    protected float timePerTick = 0.05F;
    protected Path lastPath;
    protected boolean nodeChanged;

    public BurrowerNavigation(BurrowerEntity entity, PathSource pathSource, int segments, int offset) {
        super(entity, pathSource);
        prevSegmentNodes = new Node[segments];
        activeSegmentNodes = new Node[segments];
        nextSegmentNodes = new Node[segments];
        segmentPathIndices = new int[segments];
        segmentTime = new int[segments];
        segmentOffsets = new int[segments];

        for (int i = 0; i < segmentOffsets.length; i++) {
            segmentOffsets[i] = ((i + 1) * offset);
        }

        actor.setCanDestroyBlocks(true);
        actor.setCanClimb(true);
    }

    @Override
    protected <T extends Entity> Actor<T> createActor(T entity) {
        return new Actor<>(entity) {
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
        return calcAbsolutePositionAndRotation(time * timePerTick, prevNode, activeNode, nextNode);
    }

    @Override
    protected boolean isReadyForNextNode(int ticks) {
        return ticks * timePerTick >= 1;
    }

    @Override
    protected void pathFollow(int time) {
        int nextFrontIndex = path.getNextNodeIndex() + 2;
        if (isReadyForNextNode(time)) {
            if (nextFrontIndex < path.getNodeCount()) {
                timeParam = 0;
                path.setNextNodeIndex(nextFrontIndex - 1);
                prevNode = activeNode;
                activeNode = nextNode;
                nextNode = path.getNode(nextFrontIndex);
                nodeChanged = true;
            }
        } else {
            timeParam = time;
        }
    }

    protected void doSegmentFollowTo(int ticks, int segmentIndex) {
        ticks += segmentOffsets[segmentIndex];
        // TODO: Tick rate can be changed
        while (ticks <= 0) {
            ticks += 20;
        }

        int nextFrontIndex = segmentPathIndices[segmentIndex] + 2;
        if (isReadyForNextNode(ticks)) {
            if (nextFrontIndex < path.getNodeCount()) {
                segmentPathIndices[segmentIndex] = (nextFrontIndex - 1);
                prevSegmentNodes[segmentIndex] = activeSegmentNodes[segmentIndex];
                activeSegmentNodes[segmentIndex] = nextSegmentNodes[segmentIndex];
                nextSegmentNodes[segmentIndex] = path.getNode(segmentPathIndices[segmentIndex] >= 0 ? nextFrontIndex : 0);
                segmentTime[segmentIndex] = 0;
            }
        } else {
            segmentTime[segmentIndex] = ticks;
        }

        if (segmentPathIndices[segmentIndex] >= 0) {
            PosRotate3D pos = positionAtTime(segmentTime[segmentIndex], prevSegmentNodes[segmentIndex], activeSegmentNodes[segmentIndex], nextSegmentNodes[segmentIndex]);
            ((BurrowerEntity) theEntity).setSegment(segmentIndex, pos);
            if (segmentTime[segmentIndex] == 0) {
                ((BurrowerEntity) theEntity).setSegment(segmentIndex, pos);
            }
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
            for (int segmentIndex = 0; segmentIndex < segmentPathIndices.length; segmentIndex++) {
                doSegmentFollowTo(time, segmentIndex);
            }
            timeParam = time;
            ticksStuck--;
        } else {
            ticksStuck++;
        }
    }

    @Override
    public boolean isIdle() {
        return path == null || path.getNextNodeIndex() >= path.getNodeCount() - 2;
    }

    @Override
    public boolean startMovingAlong(net.minecraft.world.level.pathfinder.Path newPath, double speed) {
        if (newPath == null || newPath.getNodeCount() < 2) {
            path = null;
            return false;
        }

        if (path == null) {
            path = newPath;
            activeNode = path.getNode(0);
            prevNode = activeNode;
            nextNode = path.getNode(1);
            if (ActionablePathNode.getAction(activeNode) != PathAction.NONE) {
                nodeActionFinished = false;
            }
            for (int i = 0; i < segmentPathIndices.length; i++) {
                if (activeSegmentNodes[i] == null) {
                    activeSegmentNodes[i] = activeNode;
                    nextSegmentNodes[i] = activeNode;
                    segmentPathIndices[i] = 0;
                    segmentTime[i] = segmentOffsets[i];
                    while (segmentTime[i] < 0) {
                        // TODO: tick rate can change now
                        segmentTime[i] += 20;
                        segmentPathIndices[i]--;
                    }
                }
            }
        }

        int mainIndex = path.getNextNodeIndex();
        if (newPath.getNode(0).equals(activeNode)) {
            if (segmentPathIndices.length > 0) {
                int lowestIndex = Math.max(0, segmentPathIndices[segmentPathIndices.length - 1]);
                path = extendPath(path, newPath, lowestIndex, mainIndex);
                mainIndex -= lowestIndex;
                path.setNextNodeIndex(mainIndex);
                nextNode = path.getNode(mainIndex + 1);
                for (int i = 0; i < segmentPathIndices.length; i++) {
                    segmentPathIndices[i] -= lowestIndex;
                    if (segmentPathIndices[i] == mainIndex) {
                        nextSegmentNodes[i] = nextNode;
                    }
                }
            } else {
                path = newPath;
                path.setNextNodeIndex(0);
                nextNode = path.getNode(1);
            }
        } else {
            path = newPath;
            activeNode = path.getNode(0);
            prevNode = activeNode;
            nextNode = path.getNode(1);
            if (ActionablePathNode.getAction(activeNode) != PathAction.NONE) {
                nodeActionFinished = false;
            }
            for (int i = 0; i < segmentPathIndices.length; i++) {
                if (activeSegmentNodes[i] == null) {
                    activeSegmentNodes[i] = activeNode;
                    nextSegmentNodes[i] = activeNode;
                    segmentPathIndices[i] = 0;
                    segmentTime[i] = segmentOffsets[i];
                    while (segmentTime[i] < 0) {
                        // TODO: tick rate can change now
                        segmentTime[i] += 20;
                        segmentPathIndices[i]--;
                    }
                }
            }
        }

        ticksStuck = 0;

        if (noSunPathfind) {
            removeSunnyPath();
        }

        return true;
    }

    protected PosRotate3D positionAtTime(int tick, Node start, Node middle, Node end) {
        return calcAbsolutePositionAndRotation(tick * timePerTick, start, middle, end);
    }

    private PosRotate3D calcAbsolutePositionAndRotation(float time, Node start, Node middle, Node end) {
        PosRotate3D pos = calcPositionAndRotation(time, start, middle, end);
        return new PosRotate3D(pos.position().add(Vec3.atLowerCornerOf(middle.asBlockPos())), pos.rotation());
    }

    private PosRotate3D calcPositionAndRotation(float time, Node start, Node middle, Node end) {
        Vec3i v = end.asBlockPos().subtract(start.asBlockPos());
        Vector3dc vd = new Vector3d(v.getX(), v.getY(), v.getZ());
        Vector3dc h = new Vector3d(
                middle.x != start.x ? 1 : -1,
                middle.y != start.y ? 1 : -1,
                middle.z != start.z ? 1 : -1
        );
        Vector3dc g = new Vector3d(
                middle.x != end.x ? 1 : -1,
                middle.y != end.y ? 1 : -1,
                middle.z != end.z ? 1 : -1
        );
        Vector3dc offset = vd.mul(-0.5D, new Vector3d()).mul(h);

        if (h.x() == 1 && g.x() == 1) {
            return new PosRotate3D(new Vec3(time * v.getX() * 0.5D + (v.getX() > 0 ? 0 : 1), 0.5D, 0.5D), new Vector3f(0, v.getX() >= 1 ? 0 : Mth.PI, 0));
        }
        if (h.y() == 1 && g.y() == 1) {
            return new PosRotate3D(new Vec3(0.5D, time * v.getY() * 0.5D + (v.getY() > 0 ? 0 : 1), 0.5D), new Vector3f());
        }
        if (h.z() == 1 && g.z() == 1) {
            return new PosRotate3D(
                    new Vec3(0.5D, 0.5D,
                            time * v.getZ() * 0.5D + (v.getZ() > 0 ? 0 : 1)),
                    new Vector3f(0, v.getZ() * Mth.PI / 4F, 0));
        }

        double sin = Math.sin(time * 0.5D * Math.PI) * 0.5D;
        double cos = Math.cos(time * 0.5D * Math.PI) * 0.5D;

        Vector3d pos = vd.mul(h, new Vector3d()).mul(
                h.x() == 1 ? sin : cos,
                h.y() == 1 ? sin : cos,
                h.z() == 1 ? sin : cos
        ).add(offset);

        Vector3f rot = new Vector3f();

        if (h.x() == 1) {
            rot.set(0, vd.x() == 1 ? 0 : 180, 0);
            if (g.z() == 1)
                rot.add(0F, (float)(time * vd.z() * vd.x() * 90F), 0F);
            else if (g.y() == 1)
                rot.add(0F, 0F, (float)(time * vd.y() * 90F));
        } else if (h.y() == 1) {
            if (g.x() == 1) {
                rot.set(vd.x() == 1 ? 0 : 180, 0, 90 * vd.y() + time * vd.x() * -90);
            } else if (g.z() == 1) {
                rot.set(90,  vd.z() * (-90 * time * vd.y()), -90);
            }
        } else if (h.z() == 1) {
            if (g.x() == 1) {
                rot.set(0,   vd.y() * ( 90 + time * vd.x() * -90), 0);
            } else if (g.y() == 1) {
                rot.set(90, -vd.z() * (-90 + time * vd.y() * -90), -90);
            }
        }

        pos = pos.add(0.5, 0.5, 0.5);
        return new PosRotate3D(new Vec3(pos.x(), pos.y(), pos.z()), rot.mul(Mth.DEG_TO_RAD));
    }

    private Path extendPath(Path path1, Path path2, int lowerBoundP1, int upperBoundP1) {
        return ActionablePathNode.combine(path1, path2, lowerBoundP1, upperBoundP1);
    }
}
