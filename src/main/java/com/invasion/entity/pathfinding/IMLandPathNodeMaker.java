package com.invasion.entity.pathfinding;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import com.invasion.Debug;
import com.invasion.InvasionMod;
import com.invasion.block.BlockMetadata;
import com.invasion.entity.pathfinding.DynamicPathNodeNavigator.NodeFactory;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.ai.scaffold.ScaffoldView;

public class IMLandPathNodeMaker extends WalkNodeEvaluator implements DynamicPathNodeNavigator.RootNodeFactory, DynamicPathNodeNavigator.NodeCache {
    private boolean canClimbLadders;
    private boolean canMineBlocks;
    private boolean canDigDown;

    private DynamicPathNodeNavigator.NodeFactory delegate = DynamicPathNodeNavigator.NodeFactory.DEFAULT;
    private Consumer<CollisionGetter> chunkCacheModifier = a -> {};

    protected PathAction previousNodeAction = PathAction.NONE;
    protected BlockPos previousNodePosition = BlockPos.ZERO;

    public boolean canDestroyBlocks() {
        return canMineBlocks && (mob == null || ((ServerLevel) mob.level()).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING));
    }

    public void setCanDestroyBlocks(boolean flag) {
        canMineBlocks = flag;
    }

    public boolean getCanDigDown() {
        return canDigDown;
    }

    public void setCanDigDown(boolean flag) {
        canDigDown = flag;
    }

    public boolean getCanClimbLadders() {
        return canClimbLadders;
    }

    public void setCanClimbLadders(boolean flag) {
        canClimbLadders = flag;
    }

    @Override
    public void prepare(PathNavigationRegion cachedWorld, Mob entity) {
        super.prepare(cachedWorld, entity);
        if (entity instanceof IHasNexus nexusHolder && nexusHolder.hasNexus()) {
            populateChunkCacheData(nexusHolder.getNexus(), cachedWorld);
        }
    }

    protected void populateChunkCacheData(NexusAccess nexus, CollisionGetter view) {
        this.chunkCacheModifier.accept(view);
    }

    @Override
    public void setDelegate(NodeFactory delegate, Consumer<CollisionGetter> chunkCacheModifier) {
        this.delegate = delegate;
    }

    @Override
    public Node getNode(int x, int y, int z, PathAction action) {
        return ActionablePathNode.setAction(getNode(x, y, z), action);
    }

    @Override
    public float getDistancePenalty(Node previousNode, Node nextNode, CollisionGetter world) {
        return delegate.getDistancePenalty(previousNode, nextNode, level);
    }

    @Override
    public int getNeighbors(Node[] successors, Node node) {
        previousNodePosition = node.asBlockPos();
        previousNodeAction = ActionablePathNode.getAction(node);
        int index = getSuccessors(super.getNeighbors(successors, node), successors, node, level, this);
        if (Debug.DEBUG_PATHFINDING) {
            for (int i = 0; i < index; i++) {
                /*if (ActionablePathNode.getAction(successors[i]) != PathAction.NONE) {
                    successors[i].type = PathNodeType.WALKABLE;
                }*/
                if (successors[i].closed) {
                    InvasionMod.LOGGER.warn("{} Looping path detected at ({}) {} was returned in a previous iteration", mob, i, successors[i]);
                } else {
                    for (int j = 0; j < index; j++) {
                        if (i != j && successors[i].hashCode() == successors[j].hashCode()) {
                            InvasionMod.LOGGER.warn("{} Looping path detected at ({}) {} was repeated in this iteration and collides with ({}) {}", mob, i, successors[i], j, successors[j]);
                        }
                    }
                }
            }
        }
        return index;
    }

    @Override
    public int getSuccessors(int index, Node[] successors, Node node, CollisionGetter world, DynamicPathNodeNavigator.NodeCache cache) {

        BlockPathTypes currentNodeType = getCachedBlockType(mob, node.x, node.y, node.z);
        double prevY = getFloorLevel(node.asBlockPos());

        int stepHeight = Mth.floor(Math.max(1, mob.maxUpStep()));

        for (Direction direction : Direction.Plane.VERTICAL) {
            Node n = findAcceptedNode(node.x, node.y + direction.getStepY(), node.z, stepHeight, prevY, direction, currentNodeType);
            if (isValidVerticalSuccessor(n, node, direction)) {
                successors[index++] = n;
            }
        }

        return delegate.getSuccessors(index, successors, node, world, cache);
    }

    protected boolean isValidVerticalSuccessor(@Nullable Node node, Node successor, Direction direction) {
        return node != null && !node.closed && node.costMalus != 0 && ActionablePathNode.getAction(node) != PathAction.NONE;
    }

    @Override
    public BlockPathTypes getBlockPathType(net.minecraft.world.level.BlockGetter context,
            int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        BlockPathTypes type = getBlockPathTypeStatic(context, pos);
        if (getCanClimbLadders() && PathingUtil.isLadder(context.getBlockState(pos))) {
            return BlockPathTypes.WALKABLE;
        }
        if (canDestroyBlocks() && type == BlockPathTypes.BLOCKED
                && !context.getBlockState(pos.move(Direction.UP))
                        .isPathfindable(context, pos, PathComputationType.LAND)) {
            return BlockPathTypes.WALKABLE;
        }
        return type;
    }

    protected boolean isNoActionNode(Node node) {
        return node != null && ActionablePathNode.getAction(node) == PathAction.NONE;
    }

    @Nullable
    @Override
    protected Node findAcceptedNode(int x, int y, int z, int maxYStep, double feetY, Direction direction, BlockPathTypes nodeType) {
        @Nullable
        Node node = super.findAcceptedNode(x, y, z, maxYStep, feetY, direction, nodeType);
        if (canDestroyBlocks() && node != null && getBlockPathTypeStatic(level, new BlockPos.MutableBlockPos(node.x, node.y, node.z)) == BlockPathTypes.BLOCKED) {
            BlockPos pos = node.asBlockPos();
            BlockState state = level.getBlockState(pos);
            if (canMineBlock(level, pos, state)
                    && !level.getBlockState(pos.above())
                            .isPathfindable(level, pos.above(), PathComputationType.LAND)) {
                node.type = BlockPathTypes.WALKABLE;
                node.costMalus = getBlockStrength(pos, state);
                return ActionablePathNode.setAction(node, PathAction.DIG);
            }
        }

        if (node != null && node.type == BlockPathTypes.WALKABLE) {
            BlockPos pos = node.asBlockPos();
            float mobDensityMultiplier = 1 + (ScaffoldView.of(level).getMobDensity(pos) * 3);
            node.costMalus += getWalkableNodePathingPenalty(level, pos, mobDensityMultiplier);
        }

        if (node != null) {
            boolean canClimb = getCanClimbLadders();
            boolean canDigDown = getCanDigDown();
            BlockPos pos = new BlockPos(x, y, z);

            if (canClimb && PathingUtil.isLadder(mob.level().getBlockState(pos))) {
                ActionablePathNode.setAction(node, previousNodeAction.getType() == PathAction.Type.CLIMB ? previousNodeAction
                            : PathAction.getClimbing(direction.getAxis().isVertical() ? direction
                            : node.y < (int)feetY ? Direction.DOWN
                            : Direction.UP
                ));
            }

            if (direction == Direction.DOWN && canDigDown && ActionablePathNode.getAction(node) == PathAction.NONE) {
                BlockState nextState = mob.level().getBlockState(pos);
                boolean isDiggable = canDigDown && direction == Direction.DOWN && canMineBlock(mob.level(), pos, nextState);

                if (isDiggable) {
                    int gapHeight = 0;
                    int magDropHeight = mob.getMaxFallDistance();
                    for (int i = 0; i <= magDropHeight; i++) {
                        if (PathingUtil.isAirOrReplaceable(mob.level().getBlockState(pos.below(1 + i)))) {
                            gapHeight++;
                        }
                    }
                    if (gapHeight > 0 && gapHeight <= magDropHeight) {
                        node.costMalus = getBlockStrength(pos, nextState);
                        ActionablePathNode.setAction(node, PathAction.DIG);
                    }
                }
            }
        }

        return node;
    }

    protected float getWalkableNodePathingPenalty(CollisionGetter world, BlockPos pos, float mobDensityMultiplier) {
        BlockState state = level.getBlockState(pos);
        return BlockMetadata.getCost(state).orElse(state.isRedstoneConductor(world, pos) ? 3.2F : 1) * mobDensityMultiplier;
    }

    @Override
    protected boolean canStartAt(BlockPos pos) {
        return super.canStartAt(pos)
                || (canDestroyBlocks()
                        && canMineBlock(level, pos, level.getBlockState(pos))
                        && !level.getBlockState(pos.above())
                                .isPathfindable(level, pos.above(), PathComputationType.LAND));
    }

    public float getBlockStrength(BlockPos pos, BlockState state) {
        return BlockMetadata.getStrength(pos, state, level);
    }

    public boolean canMineBlock(CollisionGetter world, BlockPos pos, BlockState state) {
        if (state.isAir() || !canDestroyBlocks() || BlockMetadata.isIndestructible(state) || PathingUtil.hasAdjacentLadder(world, pos)) {
            return false;
        }
        return state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.isRedstoneConductor(world, pos);
    }

    public boolean avoidsBlock(Mob entity, CollisionGetter world, BlockPos pos, BlockState state) {
        return PathingUtil.shouldAvoidBlock(entity, pos);
    }

    @Override
    public final boolean avoidsBlock(CollisionGetter world, BlockPos pos, BlockState state) {
        return avoidsBlock(mob, world, pos, state);
    }

    protected boolean canWalkOn(BlockPathTypes type) {
        return type != BlockPathTypes.DAMAGE_FIRE && type != BlockPathTypes.DANGER_FIRE && type != BlockPathTypes.LAVA && type != BlockPathTypes.STICKY_HONEY;
    }

    public static boolean canMineBlock(PathfinderMob entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        if (entity.getNavigation().getNodeEvaluator() instanceof IMLandPathNodeMaker maker) {
            return maker.canMineBlock(entity.level(), pos, state);
        }
        if (entity.getNavigation() instanceof PathNavigateAdapter adapter) {
            return adapter.getNewNavigator().getActor().isBlockDestructible(entity.level(), pos, state);
        }
        return false;
    }

    public static boolean avoidsBlock(PathfinderMob entity, BlockPos pos) {
        if (entity.getNavigation().getNodeEvaluator() instanceof IMLandPathNodeMaker maker) {
            return maker.avoidsBlock(entity, entity.level(), pos, entity.level().getBlockState(pos));
        }
        return PathingUtil.shouldAvoidBlock(entity, pos);
    }
}
