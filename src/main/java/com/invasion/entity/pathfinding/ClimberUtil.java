package com.invasion.entity.pathfinding;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public interface ClimberUtil {
    static boolean isLadder(CollisionGetter world, BlockPos pos) {
        return world.getBlockState(pos).is(Blocks.LADDER);
    }

    static boolean canPositionSupportLadder(LevelReader world, BlockPos.MutableBlockPos pos, Direction facing) {
        BlockState state = world.getBlockState(pos);
        return state.is(Blocks.LADDER)
            || (PathingUtil.isAirOrReplaceable(state) && Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing).canSurvive(world, pos));
    }

    static Stream<Direction> getPossibleLadderOrientations(LevelReader world, BlockPos.MutableBlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!PathingUtil.isAirOrReplaceable(state)) {
            return Stream.empty();
        }
        return Direction.Plane.HORIZONTAL.stream().filter(facing -> canPositionSupportLadder(world, pos, facing));
    }

    static Direction getOrientationFromNeighbors(CollisionGetter world, BlockPos.MutableBlockPos mutable, List<Direction> possibleOrientations) {
        BlockState above = world.getBlockState(mutable.move(Direction.UP));
        BlockState below = world.getBlockState(mutable.move(Direction.DOWN, 2));
        mutable.move(Direction.UP);

        if (above.is(Blocks.LADDER)) {
            Direction aboveDirection = above.getValue(LadderBlock.FACING);
            if (possibleOrientations.contains(aboveDirection)) {
                return aboveDirection;
            }
        }
        if (below.is(Blocks.LADDER)) {
            Direction belowDirection = below.getValue(LadderBlock.FACING);
            if (possibleOrientations.contains(belowDirection)) {
                return belowDirection;
            }
        }

        return possibleOrientations.get(0);
    }

    static int getWallHeight(LevelReader world, BlockPos.MutableBlockPos mutable, Direction facing, int max) {
        return PathingUtil.scanVertically(world, mutable, max, pos -> canPositionSupportLadder(world, pos, facing));
    }

    static int getGapHeight(Level world, BlockPos.MutableBlockPos mutable, int max) {
        int maxY = world.getHeight(Types.WORLD_SURFACE, mutable.getX(), mutable.getZ());
        return PathingUtil.scanVertically(world, mutable, max, pos -> {
            return pos.getY() < maxY && PathingUtil.isAirOrReplaceable(world.getBlockState(pos));
        });
    }

    static int getWallHeightPermittingGaps(Level world, BlockPos.MutableBlockPos mutable, Direction facing, int maxWall, int maxGap) {
        BlockPos initial = mutable.immutable();
        int maxY = world.getHeight(Types.WORLD_SURFACE, mutable.getX(), mutable.getZ());

        int wallHeight = 0;
        int gapHeight = 0;

        int i = 0;
        for (; i < maxY; i++) {
            mutable.set(initial).move(Direction.UP, i);
            boolean isWall = canPositionSupportLadder(world, mutable, facing);
            boolean isGap = PathingUtil.isAirOrReplaceable(world.getBlockState(mutable.relative(facing)));

            if (isWall) {
                gapHeight = 0;
                if (++wallHeight >= maxWall) {
                    break;
                }
            } else if (isGap) {
                wallHeight = 0;
                if (++gapHeight >= maxGap) {
                    break;
                }
            }
        }

        return i;
    }
}
