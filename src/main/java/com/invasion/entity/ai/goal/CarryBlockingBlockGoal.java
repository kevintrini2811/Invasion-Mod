package com.invasion.entity.ai.goal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import com.invasion.block.InvBlocks;
import com.invasion.entity.IMEndermanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public final class CarryBlockingBlockGoal extends Goal {
    private final IMEndermanEntity mob;
    private BlockPos target;
    private BlockState expectedState;

    public CarryBlockingBlockGoal(IMEndermanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.isCarryingBlock() || mob.tickCount % 5 != 0
                || !((ServerLevel) mob.level()).getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }
        BlockPos next = findNextProbePosition();
        if (next == null) {
            return false;
        }
        Optional<BlockPos> obstacle = BlockPos.betweenClosedStream(
                mob.getDimensions(mob.getPose()).makeBoundingBox(
                                com.invasion.util.math.PosUtils.bottomCenter(next)))
                .filter(pos -> canCarry(mob.level().getBlockState(pos)))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos ->
                        mob.distanceToSqr(com.invasion.util.math.PosUtils.center(pos))));
        if (obstacle.isEmpty()) {
            return false;
        }
        target = obstacle.get();
        expectedState = mob.level().getBlockState(target);
        return canCarry(expectedState);
    }

    private static boolean canCarry(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(InvBlocks.NEXUS_CORE)) {
            return false;
        }
        if (state.getBlock() instanceof SnowLayerBlock) {
            return state.getValue(SnowLayerBlock.LAYERS)
                    == SnowLayerBlock.MAX_HEIGHT;
        }
        return !(state.getBlock() instanceof VegetationBlock)
                && !(state.getBlock() instanceof GrowingPlantBlock)
                && !state.is(BlockTags.CLIMBABLE)
                && !state.is(BlockTags.REPLACEABLE_BY_TREES)
                && !state.is(BlockTags.CROPS)
                && !state.is(BlockTags.FLOWERS)
                && !state.is(Blocks.CACTUS)
                && !state.is(Blocks.SUGAR_CANE)
                && !state.is(Blocks.BAMBOO)
                && !state.is(Blocks.BAMBOO_SAPLING)
                && !state.is(Blocks.CHORUS_PLANT)
                && !state.is(Blocks.CHORUS_FLOWER);
    }

    private BlockPos findNextProbePosition() {
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            return path.getNextNodePos();
        }

        Vec3 objective = null;
        LivingEntity attackTarget = mob.getTarget();
        if (attackTarget != null) {
            objective = attackTarget.position();
        } else if (mob.hasNexus()) {
            objective = com.invasion.util.math.PosUtils.center(mob.getNexus().getOrigin());
        }
        if (objective == null) {
            return null;
        }

        Vec3 direction = objective.subtract(mob.position());
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontalLength < 0.01) {
            return null;
        }
        return BlockPos.containing(
                mob.getX() + direction.x / horizontalLength,
                mob.getY(),
                mob.getZ() + direction.z / horizontalLength);
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(
                com.invasion.util.math.PosUtils.center(target));
        mob.swing(InteractionHand.MAIN_HAND);
        if (mob.level().getBlockState(target) != expectedState) {
            return;
        }
        int flags = Block.UPDATE_NEIGHBORS
                | Block.UPDATE_CLIENTS
                | Block.UPDATE_SUPPRESS_DROPS;
        if (mob.level().setBlock(
                target, Blocks.AIR.defaultBlockState(), flags)) {
            mob.setCarriedBlock(expectedState);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
