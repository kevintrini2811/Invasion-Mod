package com.invasion.entity.pathfinding;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathComputationType;
import com.invasion.block.BlockMetadata;
import com.invasion.block.DestructableType;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.ai.scaffold.ScaffoldView;
import com.invasion.util.math.PosUtils;

@Deprecated
public class Actor<T extends Entity> implements IMPathNodeMaker {
    protected final T entity;

    private boolean canClimb;
    private boolean canDig;
    private boolean canMin;
    private boolean canSwim;

    private Optional<BlockPos> currentTargetPos = Optional.empty();

    public Actor(T entity) {
        this.entity = entity;
    }

    public Optional<BlockPos> getCurrentTargetPos() {
        return currentTargetPos;
    }

    public void setCurrentTargetPos(BlockPos pos) {
        currentTargetPos = Optional.of(pos);
    }

    public boolean getCanClimb() {
        return canClimb;
    }

    public void setCanClimb(boolean flag) {
        canClimb = flag;
    }

    public boolean getCanDigDown() {
        return canDig;
    }

    public boolean canDestroyBlocks() {
        return canMin && entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    public void setCanDestroyBlocks(boolean flag) {
        canMin = flag;
    }

    public boolean canSwimHorizontal() {
        return canSwim;
    }

    public boolean canSwimVertical() {
        return canSwim;
    }

    public void setCanSwim(boolean flag) {
        canSwim = flag;
    }

    public boolean avoidsBlock(BlockState state) {
        return !entity.isInvulnerable()
                && ((!entity.fireImmune() && WalkNodeEvaluator.isBurningBlock(state))
                    || state.is(Blocks.BEDROCK)
                    || state.is(Blocks.CACTUS)
            );
    }

    public boolean isBlockDestructible(BlockGetter world, BlockPos pos, BlockState state) {
        if (state.getDestroySpeed(world, pos) < 0 || state.is(Blocks.COMMAND_BLOCK) || state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK)) {
            return false;
        }
        if (state.isAir() || !canDestroyBlocks() || BlockMetadata.isIndestructible(state) || PathingUtil.hasAdjacentLadder(world, pos)) {
            return false;
        }
        return true;
    }

    @Override
    public float getPathNodePenalty(Node prevNode, Node node, BlockGetter terrainMap) {
        float multiplier = 1 + (ScaffoldView.of(terrainMap).getMobDensity(node.asBlockPos()) * 3);

        if (node.y > prevNode.y && getNodeDestructability(terrainMap, node.asBlockPos()) == DestructableType.DESTRUCTABLE) {
            multiplier += 2;
        }

        if (PathingUtil.hasAdjacentLadder(terrainMap, node.asBlockPos())) {
            multiplier += 5;
        }

        if (ActionablePathNode.getAction(node) == PathAction.SWIM) {
            multiplier *= node.y <= prevNode.y && !terrainMap.getBlockState(node.asBlockPos()).isAir() ? 3 : 1;
            return prevNode.distanceTo(node) * 1.3F * multiplier;
        }

        BlockState state = terrainMap.getBlockState(node.asBlockPos());
        return prevNode.distanceTo(node) * BlockMetadata.getCost(state).orElse(state.isRedstoneConductor(terrainMap, node.asBlockPos()) ? 3.2F : 1) * multiplier;
    }

    @Override
    public void getSuccessors(BlockGetter terrainMap, Node currentNode, PathBuilder pathFinder) {
        if (entity.level().isOutsideBuildHeight(currentNode.asBlockPos())) {
            return;
        }

        calcPathOptionsVertical(terrainMap, currentNode, pathFinder);

        PathAction action = ActionablePathNode.getAction(currentNode);

        if (action == PathAction.DIG && !canStandAt(terrainMap, currentNode.asBlockPos())) {
            return;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int height = Mth.ceil(entity.maxUpStep());
        for (int i = 1; i <= height; i++) {
            if (getNodeDestructability(terrainMap, mutable.set(currentNode.x, currentNode.y, currentNode.z).move(Direction.UP, i)) == DestructableType.UNBREAKABLE) {
                height = i - 1;
            }
        }

        int maxFall = 8;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (action != PathAction.NONE) {
                if (facing == Direction.EAST && action == PathAction.LADDER_UP_NX) {
                    height = 0;
                }
                if (facing == Direction.WEST && action == PathAction.LADDER_UP_PX) {
                    height = 0;
                }
                if (facing == Direction.SOUTH && action == PathAction.LADDER_UP_NZ) {
                    height = 0;
                }
                if (facing == Direction.NORTH && action == PathAction.LADDER_UP_PZ) {
                    height = 0;
                }
            }
            int currentY = currentNode.y + height;
            boolean passedLevel = false;
            do {
                int yOffset = getNextLowestSafeYOffset(terrainMap,
                        mutable.set(currentNode.x, currentY, currentNode.z).move(facing),
                        maxFall + currentY - currentNode.y
                );
                if (yOffset > 0) {
                    break;
                }
                if (yOffset > -maxFall) {
                    pathFinder.addNode(mutable.move(Direction.UP, yOffset).immutable(), PathAction.NONE);
                }

                currentY += yOffset - 1;

                if ((!passedLevel) && (currentY <= currentNode.y)) {
                    passedLevel = true;
                    if (currentY != currentNode.y) {
                        addAdjacent(terrainMap, currentNode.asBlockPos().relative(facing), currentNode, pathFinder);
                    }

                }

            } while (currentY >= currentNode.y);
        }

        if (canSwimHorizontal()) {
            for (Direction offset : Direction.Plane.HORIZONTAL) {
                if (getNodeDestructability(terrainMap, mutable.set(currentNode.x, currentNode.y, currentNode.z).move(offset)) == DestructableType.FLUID) {
                    pathFinder.addNode(mutable.immutable(), PathAction.SWIM);
                }
            }
        }
    }

    protected void calcPathOptionsVertical(BlockGetter terrainMap, Node currentNode, PathBuilder pathFinder) {
        int collideUp = getNodeDestructability(terrainMap, currentNode.asBlockPos().above());
        PathAction pathAction = ActionablePathNode.getAction(currentNode);
        if (collideUp > DestructableType.UNBREAKABLE) {
            BlockState state = terrainMap.getBlockState(currentNode.asBlockPos().above());
            if (state.is(BlockTags.CLIMBABLE)) {
                Direction facing = state.getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(null);
                PathAction action = PathAction.getLadderActionForDirection(facing);

                if (pathAction == PathAction.NONE) {
                    pathFinder.addNode(currentNode.asBlockPos().above(), action);
                } else if (pathAction.getType() == PathAction.Type.LADDER && pathAction.getOrientation() != Direction.UP) {
                    if (action == pathAction) {
                        pathFinder.addNode(currentNode.asBlockPos().above(), action);
                    }
                } else {
                    pathFinder.addNode(currentNode.asBlockPos().above(), action);
                }
            } else if (getCanClimb()) {
                if (isAdjacentSolidBlock(terrainMap, currentNode.asBlockPos().above())) {
                    pathFinder.addNode(currentNode.asBlockPos().above(), PathAction.NONE);
                }
            }
        }
        int below = getNodeDestructability(terrainMap, currentNode.asBlockPos().below());
        int above = getNodeDestructability(terrainMap, currentNode.asBlockPos().above());
        if (getCanDigDown()) {
            if (below == DestructableType.DESTRUCTABLE) {
                pathFinder.addNode(currentNode.asBlockPos().below(), PathAction.DIG);
            } else if (below == DestructableType.TERRAIN) {
                int yOffset = getNextLowestSafeYOffset(terrainMap, currentNode.asBlockPos().below(), 5);
                if (yOffset <= 0) {
                    pathFinder.addNode(currentNode.asBlockPos().above(yOffset - 1), PathAction.NONE);
                }
            }
        }

        if (canSwimVertical()) {
            if (below == -1) {
                pathFinder.addNode(currentNode.asBlockPos().below(), PathAction.SWIM);
            }
            if (above == -1) {
                pathFinder.addNode(currentNode.asBlockPos().above(), PathAction.SWIM);
            }
        }
    }

    protected final void addAdjacent(BlockGetter terrainMap, BlockPos pos, Node currentNode, PathBuilder pathFinder) {
        if (getNodeDestructability(terrainMap, pos) <= DestructableType.UNBREAKABLE) {
            return;
        }
        if (getCanClimb()) {
            if (isAdjacentSolidBlock(terrainMap, pos)) {
                pathFinder.addNode(pos, PathAction.NONE);
            }
        } else if (terrainMap.getBlockState(pos).is(BlockTags.CLIMBABLE)) {
            pathFinder.addNode(pos, PathAction.NONE);
        }
    }

    protected final boolean isAdjacentSolidBlock(BlockGetter terrainMap, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        int collisionWidth = Mth.floor(entity.getBbWidth() + 1.0F);
        for (Vec3i offset
                : collisionWidth == 1 ? PosUtils.OFFSET_ADJACENT
                : collisionWidth == 2 ? PosUtils.OFFSET_ADJACENT_2
                : PosUtils.ZERO) {
            BlockState state = terrainMap.getBlockState(mutable.set(pos).offset(offset));
            if (!state.isAir() && !state.isPathfindable(entity.level(), pos, PathComputationType.LAND)) {
                return true;
            }
        }
        return false;
    }

    protected final int getNextLowestSafeYOffset(BlockGetter world, BlockPos pos, int maxOffsetMagnitude) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; !world.isOutsideBuildHeight(i + pos.getY()) && i < maxOffsetMagnitude; i--) {
            mutable.set(pos).move(Direction.UP, i);
            if (canStandAtAndIsValid(world, mutable) || (canSwimHorizontal() && getNodeDestructability(world, mutable) == DestructableType.FLUID)) {
                return i;
            }
        }
        return 1;
    }

    public final boolean canStandAt(BlockGetter world, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosedStream(entity.getDimensions(entity.getPose()).makeBoundingBox(com.invasion.util.math.PosUtils.bottomCenter(pos))).toList()) {
            BlockState state = world.getBlockState(p);
            if ((!state.isAir() && !state.isPathfindable(entity.level(), pos, PathComputationType.LAND)) || avoidsBlock(state)) {
                return false;
            }
        }
        return canStandOnBlock(world, pos.below());
    }

    public boolean canStandAtAndIsValid(BlockGetter world, BlockPos pos) {
        return getNodeDestructability(world, pos) > DestructableType.UNBREAKABLE && canStandAt(world, pos);
    }

    protected final boolean canStandOnBlock(BlockGetter world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.entityCanStandOn(world, pos, entity) && !avoidsBlock(state);
    }

    protected final int getNodeDestructability(BlockGetter terrainMap, BlockPos pos) {
        boolean destructibleFlag = false;
        boolean liquidFlag = false;

        for (BlockPos p : BlockPos.betweenClosedStream(entity.getDimensions(entity.getPose()).makeBoundingBox(com.invasion.util.math.PosUtils.bottomCenter(pos))).toList()) {
            BlockState state = terrainMap.getBlockState(p);
            if (!state.isAir()) {
                if (state.liquid()) {
                    liquidFlag = true;
                } else if (!state.isPathfindable(entity.level(), pos, PathComputationType.LAND)) {
                    if (!isBlockDestructible(terrainMap, p, state)) {
                        return DestructableType.UNBREAKABLE;
                    }
                    destructibleFlag = true;
                } else {
                    state = terrainMap.getBlockState(p.below());
                    if (state.is(BlockTags.WOODEN_FENCES)) {
                        return isBlockDestructible(terrainMap, pos, state) ? DestructableType.BREAKABLE_BARRIER : DestructableType.UNBREAKABLE;
                    }
                }

                if (avoidsBlock(state)) {
                    return DestructableType.REPELLANT;
                }
            }
        }
        return destructibleFlag ? DestructableType.DESTRUCTABLE : liquidFlag ? DestructableType.FLUID : DestructableType.TERRAIN;
    }
}
