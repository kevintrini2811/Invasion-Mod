package com.invasion.nexus.ai.scaffold;

import com.invasion.entity.pathfinding.DynamicPathNodeNavigator;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.ai.AttackerAI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;

public class ScaffoldNodeFactory implements DynamicPathNodeNavigator.NodeFactory {
    private static final int MIN_SCAFFOLD_HEIGHT = 4;

    private final ScaffoldList scaffolds;
    private final int minDistance;

    public ScaffoldNodeFactory(AttackerAI scaffoldManager) {
        scaffolds = scaffoldManager.getScaffolds();
        minDistance = scaffoldManager.getScaffoldSpacing();
    }

    @Override
    public float getDistancePenalty(Node previousNode, Node nextNode, CollisionGetter world) {
        PathAction action = ActionablePathNode.getAction(nextNode);
        PathAction prevAction = ActionablePathNode.getAction(previousNode);
        BlockState state = world.getBlockState(nextNode.asBlockPos());
        float materialMultiplier = state.isRedstoneConductor(world, nextNode.asBlockPos()) ? 2.2F : 1.0F;
        if (action == PathAction.SCAFFOLD_UP) {
            if (prevAction != PathAction.SCAFFOLD_UP) {
                materialMultiplier *= 3.4F;
            }
            return 0.85F * materialMultiplier;
        }
        if (action == PathAction.BRIDGE) {
            if (prevAction == PathAction.SCAFFOLD_UP) {
                materialMultiplier = 0;
            }
            return 1.1F * materialMultiplier;
        }
        if (action.getType() == PathAction.Type.LADDER && action.isHorizontal()) {
            return 1.5F * materialMultiplier;
        }

        return 1;
    }

    @Override
    public int getSuccessors(int index, Node[] successors, Node node, CollisionGetter world, DynamicPathNodeNavigator.NodeCache nodeCache) {
        BlockPos pos = node.asBlockPos();
        BlockPos positionAbove = pos.above();
        BlockState stateAbove = world.getBlockState(positionAbove);
        if (ActionablePathNode.getAction(node.cameFrom) == PathAction.SCAFFOLD_UP && !nodeCache.avoidsBlock(world, positionAbove, stateAbove)) {
            Node n = nodeCache.getNode(node.x, node.y + 1, node.z, PathAction.SCAFFOLD_UP);
            if (!n.closed) {
                n.type = PathType.WALKABLE;
                n.costMalus = n.type.getMalus();
                successors[index++] = n;
            }
            return index;
        }

        for (int sl = scaffolds.size() - 1; sl >= 0; sl--) {
            if (scaffolds.get(sl).getNode().pos().closerThan(pos, minDistance)) {
                return index;
            }
        }

        if (stateAbove.isAir()) {
            BlockPos.MutableBlockPos mutable = pos.mutable();
            if (world.getBlockState(mutable.move(Direction.DOWN, 2)).isRedstoneConductor(world, mutable)) {
                for (int i = 1; i < MIN_SCAFFOLD_HEIGHT; i++) {
                    if (world.getBlockState(mutable.set(pos).move(Direction.UP, i)).isAir()) {
                        return index;
                    }
                }

                Node n = nodeCache.getNode(node.x, node.y + 1, node.z, PathAction.SCAFFOLD_UP);
                if (!n.closed) {
                    n.type = PathType.WALKABLE;
                    n.costMalus = n.type.getMalus();
                    successors[index++] = n;
                }
            }
        }

        return index;
    }
}
