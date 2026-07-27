package com.invasion.nexus.ai.scaffold;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.DynamicPathNodeNavigator;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.ai.AttackerAI;
import com.invasion.nexus.test.PathingDebugger;

public class ScaffoldGenerator {
    private final ScaffoldNodeFactory nodeFactory;

    public ScaffoldGenerator(AttackerAI scaffoldManager) {
        this.nodeFactory = new ScaffoldNodeFactory(scaffoldManager);
    }

    public List<ScaffoldNode> generateScaffolds(NexusEntity entity) {
        return DynamicPathNodeNavigator.createHeadlessNavigator(entity, 12, pathSource -> {
            return findCheapestScaffolds(entity, pathSource, entity.blockPosition());
        });
    }

    private List<ScaffoldNode> findCheapestScaffolds(NexusEntity entity, DynamicPathNodeNavigator.PathSupplier pathSupplier, BlockPos pos) {
        BlockPos nexusPos = entity.getNexus().getOrigin();

        @Nullable
        Path basePath = pathSupplier.findPathTo(nodeFactory, pos, nexusPos, 12);
        if (basePath == null) {
            return List.of();
        }
        List<ScaffoldNode> scaffoldPositions = getNodes(entity, basePath);
        PathingDebugger.sendPathToClients(entity.asEntity(), basePath, 2);

        return switch (scaffoldPositions.size()) {
            case 0 -> List.of();
            case 1 -> scaffoldPositions;
            default -> findCheapestReachable(scaffoldPositions, pathSupplier, nexusPos, pos).orElseGet(() -> {
                return findNonTerminating(scaffoldPositions, pathSupplier, nexusPos, pos);
            });
        };
    }

    private Optional<List<ScaffoldNode>> findCheapestReachable(List<ScaffoldNode> nodes, DynamicPathNodeNavigator.PathSupplier pathSupplier, BlockPos nexusPos, BlockPos pos) {
        return nodes.stream()
            .map(node -> Pair.of(node, pathSupplier.findPathTo(nodeFactory, pos, nexusPos, 12, chunk -> {
                ScaffoldView.of(chunk).addScaffoldPosition(node.pos());
            })))
            .filter(pair -> isTerminating(pair.getSecond(), nexusPos))
            .sorted(Comparator.comparing(pair -> pair.getSecond().getEndNode().g))
            .map(Pair::getFirst)
            .findFirst()
            .map(List::of);
    }

    private List<ScaffoldNode> findNonTerminating(List<ScaffoldNode> scaffoldPositions, DynamicPathNodeNavigator.PathSupplier pathSupplier, BlockPos nexusPos, BlockPos pos) {
        List<ScaffoldNode> copy = new ArrayList<>(scaffoldPositions);
        return scaffoldPositions.stream().filter(scaffold -> {
            @Nullable
            Path path = pathSupplier.findPathTo(nodeFactory, pos, nexusPos, 12, chunk -> {
                for (ScaffoldNode segment : copy) {
                    if (segment != scaffold) {
                        ScaffoldView.of(chunk).addScaffoldPosition(segment.pos());
                    }
                }
            });
            return path != null && !isTerminating(path, nexusPos);
        }).toList();
    }

    private boolean isTerminating(@Nullable Path path, BlockPos target) {
        return path != null && path.getEndNode() != null && path.getTarget().equals(target);
    }

    private List<ScaffoldNode> getNodes(NexusEntity entity, Path path) {
        List<ScaffoldNode> scaffoldPositions = new ArrayList<>();
        int startHeight = 0;
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);
            if (i == 0) {
                if (ActionablePathNode.getAction(node) == PathAction.SCAFFOLD_UP) {
                    startHeight = node.y - 1;
                }
            } else if (ActionablePathNode.getAction(node) != PathAction.SCAFFOLD_UP) {
                BlockPos pos = new BlockPos(node.cameFrom.x, startHeight, node.cameFrom.z);
                scaffoldPositions.add(new ScaffoldNode(pos, calculateOrientation(entity.asEntity().level(), pos), node.y - startHeight));
            }
        }

        return scaffoldPositions;
    }

    private Direction calculateOrientation(Level world, BlockPos pos) {
        int mostBlocks = 0;
        Direction highestDirection = Direction.EAST;
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (Direction offset : Direction.Plane.HORIZONTAL) {
            int blockCount = 0;
            for (int height = 0; height < pos.getY(); height++) {
                if (world.getBlockState(mutable.set(pos).move(Direction.UP, height).move(offset)).isCollisionShapeFullBlock(world, mutable)) {
                    blockCount++;
                }
                if (world.getBlockState(mutable.set(pos).move(Direction.UP, height).move(offset, 2)).isCollisionShapeFullBlock(world, mutable)) {
                    blockCount++;
                }
            }
            if (blockCount > mostBlocks) {
                highestDirection = offset;
            }
        }
        return highestDirection;
    }
}
