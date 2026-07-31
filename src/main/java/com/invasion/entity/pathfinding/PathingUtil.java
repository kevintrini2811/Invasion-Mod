package com.invasion.entity.pathfinding;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public interface PathingUtil {
    Set<BlockPathTypes> AVOIDED_TYPES = Set.of(
            BlockPathTypes.DAMAGE_CAUTIOUS,
            BlockPathTypes.UNPASSABLE_RAIL
    );
    Set<BlockPathTypes> FIRE_DAMAGE_TYPES = Set.of(
            BlockPathTypes.DAMAGE_FIRE,
            BlockPathTypes.DANGER_FIRE,
            BlockPathTypes.LAVA
    );
    Set<BlockPathTypes> WATER_DAMAGE_TYPES = Set.of(
            BlockPathTypes.WATER,
            BlockPathTypes.WATER_BORDER
    );

    static boolean hasAdjacentLadder(BlockGetter world, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (Direction offset : Direction.Plane.HORIZONTAL) {
            if (isLadder(world.getBlockState(mutable.set(pos).move(offset)))) {
                return true;
            }
        }
        return false;
    }

    static boolean isLadder(BlockState state) {
        return state.is(BlockTags.CLIMBABLE);
    }

    static boolean isAirOrReplaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    static boolean shouldAvoidBlock(Mob entity, BlockPos pos) {
        if (entity.isInvulnerable()) {
            return false;
        }

        BlockState state = entity.level().getBlockState(pos);

        if (state.is(Blocks.END_PORTAL_FRAME) || state.is(BlockTags.PORTALS)) {
            return true;
        }

        BlockPathTypes type = WalkNodeEvaluator.getBlockPathTypeStatic(
                entity.level(), pos.mutable());
        return AVOIDED_TYPES.contains(type)
                || (!entity.fireImmune() && FIRE_DAMAGE_TYPES.contains(type))
                || (!entity.canBreatheUnderwater() && WATER_DAMAGE_TYPES.contains(type));
    }

    static int scanVertically(LevelReader world, BlockPos.MutableBlockPos mutable, int max, Predicate<BlockPos.MutableBlockPos> test) {
        int initialY = mutable.getY();
        int height = 0;
        while (height < max && test.test(mutable.setY(initialY + height))) {
            height++;
        }
        return height;
    }
}
