package com.invasion.entity.ai.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathComputationType;

/**
 * The class name is a misnomer.
 * This causes zombies to crouch when they have a block over their head.
 */
public class StoopGoal extends Goal {
    private final Mob theEntity;
    private int updateTimer;
    private boolean stopStoop = true;

    public StoopGoal(Mob entity) {
        theEntity = entity;
    }

    @Override
    public boolean canUse() {
        if (--updateTimer > 0) {
            return false;
        }

        updateTimer = 10;
        return isObstructed();
    }

    @Override
    public boolean canContinueToUse() {
        return !stopStoop;
    }

    @Override
    public void start() {
        theEntity.setShiftKeyDown(true);
        stopStoop = false;
    }

    @Override
    public void tick() {
        if (--updateTimer <= 0) {
            updateTimer = 10;
        }
        if (!isObstructed()) {
            theEntity.setShiftKeyDown(false);
            stopStoop = true;
        }
    }

    private boolean isObstructed() {
        return !theEntity.level()
                .getBlockState(theEntity.blockPosition().above(2))
                .isPathfindable(PathComputationType.LAND);
    }
}
