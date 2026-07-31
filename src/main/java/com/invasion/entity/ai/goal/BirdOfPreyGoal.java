package com.invasion.entity.ai.goal;

import com.invasion.entity.EntityIMFlying;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.ai.MoveState;
import com.invasion.entity.pathfinding.FlyingNavigation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class BirdOfPreyGoal extends Goal {
    private static final int PATIENCE = 600;
    private static final int MIN_ATTACK_DISTANCE = 10;

    private final EntityIMFlying mob;

    private int timeWithGoal;
    private HasAiGoals.Goal lastGoal;

    public BirdOfPreyGoal(EntityIMFlying entity) {
        mob = entity;
        lastGoal = entity.getAIGoal();
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        timeWithGoal = 0;
    }

    @Override
    public void tick() {
        timeWithGoal++;

        if (mob.getAIGoal() != lastGoal) {
            lastGoal = mob.getAIGoal();
            timeWithGoal = 0;
        }

        LivingEntity lastTarget = mob.getTarget();

        if (lastTarget == null) {
            if (!mob.hasNexus()) {
                if (!mob.hasGoal(HasAiGoals.Goal.BREAK_NEXUS)) {
                    mob.transitionAIGoal(HasAiGoals.Goal.BREAK_NEXUS);
                }
            } else if (!mob.hasGoal(HasAiGoals.Goal.CHILL)) {
                mob.transitionAIGoal(HasAiGoals.Goal.CHILL);
                mob.getNavigation().stop();
                ((FlyingNavigation)mob.getNavigatorNew()).setMovementType(FlyingNavigation.MoveType.PREFER_WALKING);
                ((FlyingNavigation)mob.getNavigatorNew()).setLandingPath();
            }
        } else if (mob.hasAnyGoal(HasAiGoals.Goal.CHILL, HasAiGoals.Goal.NONE)) {
            mob.transitionAIGoal(shouldAttackTarget(lastTarget) ? HasAiGoals.Goal.MELEE_TARGET : HasAiGoals.Goal.STAY_AT_RANGE);
        }

        if (!mob.hasGoal(HasAiGoals.Goal.STAY_AT_RANGE) && mob.hasGoal(HasAiGoals.Goal.MELEE_TARGET) && timeWithGoal > PATIENCE) {
            mob.transitionAIGoal(HasAiGoals.Goal.STAY_AT_RANGE);
        }
    }

    private boolean shouldAttackTarget(LivingEntity target) {
        return mob.getMoveState() != MoveState.FLYING
                && mob.distanceTo(target) < MIN_ATTACK_DISTANCE
                && mob.getRandom().nextFloat() > 0.3F;
    }
}