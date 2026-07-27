package com.invasion.entity.ai.goal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import com.invasion.block.InvBlocks;
import com.invasion.entity.IMEndermanEntity;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

public final class CarryBlockingBlockGoal extends Goal {
    private final IMEndermanEntity mob;
    private BlockPos target;
    private BlockState expectedState;
    private int pickupTime;

    public CarryBlockingBlockGoal(IMEndermanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.isCarryingBlock() || mob.tickCount % 5 != 0 || mob.getNavigation().isDone()
                || !((ServerLevel) mob.level()).getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }
        BlockPos next = mob.getNavigation().getPath().getNextNodePos();
        Optional<BlockPos> obstacle = BlockPos.betweenClosedStream(
                        mob.getDimensions(mob.getPose()).makeBoundingBox(
                                com.invasion.util.math.PosUtils.bottomCenter(next)))
                .filter(pos -> !mob.level().getBlockState(pos).is(InvBlocks.NEXUS_CORE))
                .filter(pos -> IMLandPathNodeMaker.canMineBlock(mob, pos))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos ->
                        mob.distanceToSqr(com.invasion.util.math.PosUtils.center(pos))));
        if (obstacle.isEmpty()) {
            return false;
        }
        target = obstacle.get();
        expectedState = mob.level().getBlockState(target);
        return !expectedState.isAir();
    }

    @Override
    public void start() {
        pickupTime = 20;
        mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.isCarryingBlock() && target != null && pickupTime > 0
                && mob.level().getBlockState(target) == expectedState
                && mob.distanceToSqr(com.invasion.util.math.PosUtils.center(target)) <= 9;
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(com.invasion.util.math.PosUtils.center(target));
        if (pickupTime % 5 == 0) {
            mob.swing(InteractionHand.MAIN_HAND);
        }
        if (--pickupTime == 0 && mob.level().getBlockState(target) == expectedState) {
            int flags = Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
            if (mob.level().setBlock(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), flags)) {
                mob.setCarriedBlock(expectedState);
            }
        }
    }
}
