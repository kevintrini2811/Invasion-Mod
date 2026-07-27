package com.invasion.entity.ai.goal;

import com.invasion.entity.EntityIMFlying;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.ai.MoveState;
import com.invasion.entity.pathfinding.FlyingNavigation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class FlyingTackleGoal extends Goal {
    private final EntityIMFlying theEntity;

    public FlyingTackleGoal(EntityIMFlying entity) {
        this.theEntity = entity;
    }

    @Override
    public boolean canUse() {
        return theEntity.hasGoal(HasAiGoals.Goal.TACKLE_TARGET);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = theEntity.getTarget();
        if (target == null || !target.isAlive()) {
            theEntity.transitionAIGoal(HasAiGoals.Goal.NONE);
            return false;
        }

        return theEntity.hasGoal(HasAiGoals.Goal.TACKLE_TARGET);
    }

    @Override
    public void start() {
        LivingEntity target = theEntity.getTarget();
        if (target != null) {
            ((FlyingNavigation)theEntity.getNavigatorNew()).setMovementType(FlyingNavigation.MoveType.PREFER_WALKING);
        }
    }

    @Override
    public void tick() {
        if (theEntity.getMoveState() != MoveState.FLYING) {
            theEntity.transitionAIGoal(HasAiGoals.Goal.MELEE_TARGET);
        }
    }
}