package com.invasion.entity.pathfinding;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.path.PathAction;

public class DynamicPathNodeNavigator extends PathFinder {
    private NodeFactory measurer;

    public DynamicPathNodeNavigator(NodeEvaluator pathNodeMaker, int range) {
        super(pathNodeMaker, range);
        measurer = pathNodeMaker instanceof NodeFactory a ? a : NodeFactory.DEFAULT;
    }

    public static <T> T createHeadlessNavigator(NexusEntity entity, int range, Function<PathSupplier, T> supplier) {
        PathfinderMob standin = (PathfinderMob)entity.asEntity().getType().create(entity.asEntity().level(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
        standin.restoreFrom(entity.asEntity());
        NodeEvaluator maker = entity.getNavigatorNew().createNodeMaker();
        var nav = new DynamicPathNodeNavigator(maker, range);
        return supplier.apply((context, from, positions, distance, chunkCacheModifier) -> {
            try {
                if (maker instanceof RootNodeFactory root) {
                    root.setDelegate(context, chunkCacheModifier);
                }
                int i = range + distance;
                standin.setPos(com.invasion.util.math.PosUtils.bottomCenter(from));
                PathNavigationRegion chunkCache = new PathNavigationRegion(standin.level(), from.offset(-i, -i, -i), from.offset(i, i, i));
                return nav.findPath(chunkCache, standin, positions, range, distance, 1);
            } finally {
                if (maker instanceof RootNodeFactory root) {
                    root.setDelegate(NodeFactory.DEFAULT, a -> {});
                }
            }
        });
    }

    @Override
    protected float distance(Node previousNode, Node nextNode) {
        float distance = previousNode.distanceTo(nextNode);
        float distancePenalty = measurer.getDistancePenalty(previousNode, nextNode, null);
        float penalizedDistance = distance * distancePenalty;
        nextNode.costMalus += penalizedDistance - distance;
        return distance;
    }

    public interface PathSupplier {
        Path findPathToAny(NodeFactory context, BlockPos from, Set<BlockPos> positions, int distance, Consumer<CollisionGetter> chunkCacheModifier);

        default Path findPathTo(NodeFactory context, BlockPos from, BlockPos to, int distance, Consumer<CollisionGetter> chunkCacheModifier) {
            return findPathToAny(context, from, Set.of(to), distance, chunkCacheModifier);
        }

        default Path findPathTo(NodeFactory context, BlockPos from, BlockPos to, int distance) {
            return findPathToAny(context, from, Set.of(to), distance, a -> {});
        }
    }

    public interface RootNodeFactory extends NodeFactory, NodeCache {
        void setDelegate(NodeFactory delegate, Consumer<CollisionGetter> chunkCacheModifier);
    }

    public interface NodeCache {
        Node getNode(int x, int y, int z, PathAction action);

        boolean avoidsBlock(CollisionGetter world, BlockPos pos, BlockState state);
    }

    public interface NodeFactory {
        NodeFactory DEFAULT = new NodeFactory() {
            @Override
            public float getDistancePenalty(Node previousNode, Node nextNode, CollisionGetter world) {
                return 1;
            }

            @Override
            public int getSuccessors(int index, Node[] successors, Node node, CollisionGetter world, NodeCache cache) {
                return index;
            }
        };

        float getDistancePenalty(Node previousNode, Node nextNode, CollisionGetter world);

        int getSuccessors(int index, Node[] successors, Node node, CollisionGetter world, NodeCache cache);
    }
}
