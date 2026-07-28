package com.invasion.entity.pathfinding;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.invasion.InvasionMod;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Version of PathNodeNavigator that takes the PathNodeMaker as a parameter rather than storing internally
 */
@Deprecated
public class IMPathNodeNavigator {
    private BlockGetter worldMap;
    private IMPathNodeMaker pathNodeMaker;
    private final BinaryHeap minHeap = new BinaryHeap();

    // PathNodeMaker.pathNodeCache
    private final Int2ObjectMap<Node> pathNodeCache = new Int2ObjectOpenHashMap<>();
    private final Node[] successors = new Node[32];
    private Node finalTarget;
    private float targetRadius;
    private int pathsIndex;
    private float searchRange;
    private int nodeLimit;
    private int nodesOpened;

    private final IMPathNodeMaker.PathBuilder pathBuilder = new IMPathNodeMaker.PathBuilder() {
        @Override
        public void addNode(BlockPos pos, PathAction action) {
            Node node = openPoint(pos, action);
            if (node != null && !node.closed && node.distanceTo(finalTarget) < searchRange) {
                successors[pathsIndex++] = node;
            }
        }
    };

    @Nullable
    public Path createPath(IMPathNodeMaker pather, BlockPos from, BlockPos to, float targetRadius, float maxSearchRange, BlockGetter iblockaccess, int searchDepth, int quickFailDepth) {
        worldMap = iblockaccess;
        pathNodeMaker = pather;
        nodeLimit = searchDepth;
        nodesOpened = 1;
        searchRange = maxSearchRange;
        minHeap.clear();
        pathNodeCache.clear();
        Node start = openPoint(from);
        Node target = openPoint(to);
        finalTarget = target;
        this.targetRadius = targetRadius;
        Path path = addToPath(start, target);
        if (path != null) {
            InvasionMod.LOGGER.debug("Path find success");
        }
        return path;
    }

    /**
     * PathNodeNavigator.findPathToAny
     */
    @Nullable
    private Path addToPath(Node start, Node target) {
        start.g = 0;
        start.h = start.distanceTo(target);
        start.f = start.h;

        minHeap.clear();
        minHeap.insert(start);
        Node previousPoint = start;

        while (!minHeap.isEmpty()) {
            if (nodesOpened > nodeLimit) {
                return createPath(previousPoint, target.asBlockPos(), false);
            }
            Node examiningPoint = minHeap.pop();
            float distanceToTarget = examiningPoint.distanceTo(target);
            if (distanceToTarget < this.targetRadius + 0.1F) {
                return createPath(examiningPoint, target.asBlockPos(), true);
            }
            if (distanceToTarget < previousPoint.distanceTo(target)) {
                previousPoint = examiningPoint;
            }
            examiningPoint.closed = true;

            pathsIndex = 0;
            pathNodeMaker.getSuccessors(worldMap, examiningPoint, pathBuilder);
            int i = pathsIndex;

            for (int j = 0; j < i; j++) {
                Node newPoint = successors[j];

                float actualCost = examiningPoint.g + pathNodeMaker.getPathNodePenalty(examiningPoint, newPoint, this.worldMap);

                if (!newPoint.inOpenSet() || actualCost < newPoint.g) {
                    newPoint.cameFrom = examiningPoint;
                    newPoint.g = actualCost;
                    newPoint.h = calculateDistance(newPoint, target);

                    if (newPoint.inOpenSet()) {
                        minHeap.changeCost(newPoint, newPoint.g + newPoint.h);
                    } else {
                        newPoint.f = newPoint.g + newPoint.h;
                        minHeap.insert(newPoint);
                    }
                }
            }
        }

        return previousPoint == start ? null : createPath(previousPoint, finalTarget.asBlockPos(), false);
    }

    private Path createPath(Node endNode, BlockPos target, boolean reachesTarget) {
        List<Node> list = Lists.<Node>newArrayList();
        Node pathNode = endNode;
        list.add(0, endNode);

        while(pathNode.cameFrom != null) {
            pathNode = pathNode.cameFrom;
            list.add(0, pathNode);
        }

        return new Path(list, target, reachesTarget);
    }

    private float calculateDistance(Node start, Node target) {
        return start.distanceToSqr(target);
    }

    private Node openPoint(BlockPos pos) {
        return openPoint(pos, PathAction.NONE);
    }

    private Node openPoint(BlockPos pos, PathAction action) {
        int hash = Node.createHash(pos.getX(), pos.getY(), pos.getZ());
        Node pathpoint = pathNodeCache.get(hash);
        if (pathpoint == null) {
            pathpoint =  ActionablePathNode.setAction(new Node(pos.getX(), pos.getY(), pos.getZ()), action);
            pathNodeCache.put(hash, pathpoint);
            nodesOpened++;
        }

        return pathpoint;
    }
}
