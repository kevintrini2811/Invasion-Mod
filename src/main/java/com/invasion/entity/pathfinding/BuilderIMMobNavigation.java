package com.invasion.entity.pathfinding;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import org.jetbrains.annotations.Nullable;

import com.invasion.block.BlockMetadata;
import com.invasion.block.InvBlocks;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.ai.scaffold.ScaffoldView;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class BuilderIMMobNavigation extends IMMobNavigation {
    private static final int MAX_LADDER_TOWER_HEIGHT = 4;
    private static final int MAX_LADDERABLE_WALL_HEIGHT = 16;
    private static final int WORK_FOUND_COOLDOWN = 60;
    private static final int JOBLESS_COOLDOWN = 140;

    private int jobRequestTimer;
    private boolean waitingForJob;

    private final NexusEntity nexusEntity;

    public <T extends Mob & NexusEntity> BuilderIMMobNavigation(T entity) {
        super(entity);
        this.nexusEntity = entity;
    }

    @Override
    public NodeEvaluator createNodeMaker() {
        var nodeMaker = new NodeMaker();
        nodeMaker.setCanPassDoors(true);
        nodeMaker.setCanOpenDoors(true);
        nodeMaker.setCanFloat(true);
        nodeMaker.setCanClimbLadders(true);
        return nodeMaker;
    }

    @Override
    protected Path createPath(Set<BlockPos> positions, int range, boolean useHeadPos, int distance, float followRange) {
        return super.createPath(positions, range, useHeadPos, distance, followRange);
    }

    @Override
    protected void tickObjectives() {
        super.tickObjectives();
        if (!waitingForJob && nexusEntity.hasNexus()) {
            jobRequestTimer = Math.max(0, jobRequestTimer - 1);
            int yDifference = nexusEntity.getNexus().getOrigin().getY() - mob.blockPosition().getY();
            int weight = yDifference > 1 ? Math.max(6000 / yDifference, 1) : 1;
            if (getAIGoal() == Goal.BREAK_NEXUS && (getLastPathDistanceToTarget() > 2 && jobRequestTimer <= 0 || mob.getRandom().nextInt(weight) == 0)) {
                waitingForJob = true;
                nexusEntity.getNexus().getAttackerAI().requestBuildJob(nexusEntity, target -> {
                    waitingForJob = false;
                    jobRequestTimer = target.isPresent() ? WORK_FOUND_COOLDOWN : JOBLESS_COOLDOWN;
                    target.ifPresent(pos -> {
                        moveTo(createPath(pos, nexusEntity.asEntity().blockPosition().distManhattan(pos)), 1);
                    });
                });
            }
        }
    }

    @Nullable
    private static Direction getInitialLadderOrientation(CollisionGetter world, BlockPos.MutableBlockPos mutable) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (canPositionSupportLadder(world, mutable, facing)) {
                return facing;
            }
        }
        return Direction.UP;
    }

    public static boolean canPositionSupportLadder(BlockGetter world, BlockPos.MutableBlockPos pos, Direction side, int height) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        while (height >= 0) {
            if (!canPositionSupportLadder(world, pos.set(x, y + height--, z), side)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canPositionSupportLadder(BlockGetter world, BlockPos.MutableBlockPos pos, Direction side) {
        return world.getBlockState(pos.move(side)).isFaceSturdy(world, pos, side);
    }

    class NodeMaker extends IMLandPathNodeMaker {

        private final Long2ObjectMap<List<Direction>> ladderOrientationPossibilities = new Long2ObjectOpenHashMap<>();
        private final Object2ObjectMap<Direction, Long2ObjectMap<Long>> climbableObstacleHeights = new Object2ObjectOpenHashMap<>();

        @Override
        public void done() {
            super.done();
            ladderOrientationPossibilities.clear();
            climbableObstacleHeights.clear();
        }

        protected final List<Direction> getPossibleLadderOrientations(BlockPos pos) {
            return ladderOrientationPossibilities.computeIfAbsent(pos.asLong(), l -> ClimberUtil.getPossibleLadderOrientations(level, pos.mutable()).toList());
        }

        protected final long getWallHeightPermittingGaps(Direction orientation, BlockPos pos) {
            return climbableObstacleHeights.computeIfAbsent(orientation, o -> new Long2ObjectOpenHashMap<Long>())
                    .computeIfAbsent(pos.asLong(), l -> (long)ClimberUtil.getWallHeightPermittingGaps(level, pos.mutable(), orientation, MAX_LADDER_TOWER_HEIGHT, MAX_LADDER_TOWER_HEIGHT));
        }

        @Override
        protected void populateChunkCacheData(NexusAccess nexus, CollisionGetter view) {
            super.populateChunkCacheData(nexus, view);
            nexus.getAttackerAI().addScaffoldDataTo(view);
        }

        @Override
        public float getDistancePenalty(Node previousNode, Node nextNode, CollisionGetter world) {
            world = currentContext.level();

            BlockState block = world.getBlockState(nextNode.asBlockPos());
            PathAction action = ActionablePathNode.getAction(nextNode);
            float materialMultiplier = !block.isAir() && canBuildOnBlock(world, nextNode.asBlockPos()) ? 3.2F : 1;

            if (action.getType() == PathAction.Type.BRIDGE) {
                return 1.7F * materialMultiplier;
            }

            if (action.getType() == PathAction.Type.SCAFFOLD) {
                return 0.5F;
            }

            if (action.getType() == PathAction.Type.LADDER && action.getOrientation() != Direction.UP) {
                return 1.3F * materialMultiplier;
            }

            if (action.getType() == PathAction.Type.TOWER) {
                return 1.4F;
            }

            float multiplier = 1 + ScaffoldView.of(world).getMobDensity(nextNode.asBlockPos());

            if (block.isAir() || block.canBeReplaced()) {
                return multiplier;
            }

            if (block.is(Blocks.LADDER)) {
                return 0.7F * multiplier;
            }

            if (!block.is(InvBlocks.NEXUS_CORE) && !block.isRedstoneConductor(world, nextNode.asBlockPos())) {
                return 3.2F;
            }

            return super.getDistancePenalty(previousNode, nextNode, world);
        }

        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            PathType type = super.getPathType(context, x, y, z);
            if (type != PathType.WALKABLE && !getPossibleLadderOrientations(new BlockPos.MutableBlockPos(x, y, z)).isEmpty()) {
                return PathType.WALKABLE;
            }
            return type;
        }

        @Override
        protected Node findAcceptedNode(int x, int y, int z, int maxYStep, double feetY, Direction direction, PathType nodeType) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            boolean isHorizontal = direction.getAxis().isHorizontal();
            boolean isOnLadder = previousNodeAction.getType() == PathAction.Type.LADDER;
            boolean isScaffoldPosition = ScaffoldView.of(level).isScaffoldPosition(mutable.set(x, y, z));

            if (isHorizontal && isOnLadder && !isScaffoldPosition) {
                return null;
            }

            Node node = super.findAcceptedNode(x, y, z, maxYStep, feetY, direction, nodeType);

            if (isScaffoldPosition && ActionablePathNode.getAction(node) == PathAction.NONE) {
                return ActionablePathNode.setActionIfNotPresent(node, PathAction.SCAFFOLD_UP);
            }

            boolean isNoAction = ActionablePathNode.getAction(node) == PathAction.NONE;
            boolean isClimb = ActionablePathNode.getAction(node).getType() == PathAction.Type.CLIMB;

            if (isNoAction && isHorizontal) {

                if (isBridgableGap(mutable.set(x, y, z))) {
                    return getBridgeNode(x, y, z);
                }

                // Get possible sides that can hold a ladder
                List<Direction> possibleOrientations = getPossibleLadderOrientations(mutable.set(x, y, z));

                if (possibleOrientations.isEmpty()) {
                    return node;
                }

                // Get the initial orientation from vertical neighbours
                Direction ladderOrientation = ClimberUtil.getOrientationFromNeighbors(level, mutable.set(x, y, z), possibleOrientations);
                long ladderHeight = getWallHeightPermittingGaps(ladderOrientation, mutable.set(x, feetY, z));
                Direction optimalOrientation = ladderOrientation;

                // Measure wall heights and pick the side that gets us the farthest
                for (Direction alternate : possibleOrientations) {
                    if (alternate != ladderOrientation) {
                        long alternateHeight = getWallHeightPermittingGaps(alternate, mutable.set(x, feetY, z));
                        if (alternateHeight > 0 && alternateHeight < MAX_LADDER_TOWER_HEIGHT && alternateHeight > ladderHeight) {
                            optimalOrientation = alternate;
                            ladderHeight = alternateHeight;
                        }
                    }
                }

                // Only add if it's valid and above the jump height
                if (optimalOrientation.getAxis() != Direction.Axis.Y && ladderHeight > maxYStep) {
                    return getLadderNode(x, y, z, PathAction.getLadderActionForDirection(optimalOrientation), 1.25F);
                }

                return node;
            }

            if (!isHorizontal && !isClimb) {
                Direction ladderOrientation = getRememberedLadderOrientation();

                if (ladderOrientation.getAxis().isHorizontal()) {
                    // Check if we can continue laddering
                    int ascentionHeight = ClimberUtil.getGapHeight(level, mutable.set(x, feetY, z).move(ladderOrientation.getOpposite()), MAX_LADDERABLE_WALL_HEIGHT);

                    if (ClimberUtil.canPositionSupportLadder(level, mutable.set(x, y, z), ladderOrientation)) {
                        if (ascentionHeight <= 0 || ascentionHeight > MAX_LADDER_TOWER_HEIGHT) {
                            return null;
                        }

                        return getLadderNode(x, y, z, PathAction.getLadderActionForDirection(ladderOrientation), 1.25F);
                    }

                    if (canPositionFitTower(mutable.set(x, y, z), ladderOrientation)) {
                        // If there is no supporting block, but we're below the required height, build our own
                        return getLadderNode(x, y, z, PathAction.getTowerActionForDirection(ladderOrientation), 1.75F);
                    }
                }
            }

            return node;
        }

        private boolean canPositionFitTower(BlockPos.MutableBlockPos pos, Direction ladderOrientation) {
            return ladderOrientation.getAxis().isHorizontal()
                && PathingUtil.isAirOrReplaceable(level.getBlockState(pos))
                && PathingUtil.isAirOrReplaceable(level.getBlockState(pos.move(ladderOrientation.getOpposite())));
        }

        private Direction getRememberedLadderOrientation() {
            if (previousNodeAction.getType() == PathAction.Type.LADDER || previousNodeAction.getType() == PathAction.Type.TOWER) {
                return previousNodeAction.getOrientation();
            }

            BlockState stateAtNode = level.getBlockState(previousNodePosition);

            // Sicherstellen, dass es wirklich eine Leiter ist
            if (stateAtNode.getBlock() instanceof LadderBlock && stateAtNode.hasProperty(LadderBlock.FACING)) {
                return stateAtNode.getValue(LadderBlock.FACING);
            }

            // Alles andere (z.B. Vines) -> keine spezielle Ausrichtung, einfach UP
            return Direction.UP;
        }


        private Node getBridgeNode(int x, int y, int z) {
            Node node = getNode(x, y, z);
            node.type = PathType.WALKABLE;
            node.costMalus = 1.5F;
            return ActionablePathNode.setAction(node, PathAction.BRIDGE);
        }

        private Node getLadderNode(int x, int y, int z, PathAction action, float penalty) {
            Node node = getNode(x, y, z);
            node.type = PathType.WALKABLE;
            node.costMalus = penalty;
            return ActionablePathNode.setAction(node, action);
        }

        private boolean isBridgableGap(BlockPos.MutableBlockPos mutable) {
            int originalY = mutable.getY();
            try {
                PathType type = getPathTypeStatic(mob, mutable.setY(originalY - 1));
                return type == PathType.OPEN || type == PathType.WATER || type == PathType.LAVA;
            } finally {
                mutable.setY(originalY);
            }
        }

        private boolean canBuildOnBlock(CollisionGetter world, BlockPos pos) {
            return world.getBlockState(pos).isAir() || !BlockMetadata.isIndestructible(world.getBlockState(pos)) || PathingUtil.hasAdjacentLadder(world, pos);
        }
    }
}
