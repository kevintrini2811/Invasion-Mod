package com.invasion.entity.ai.goal;

import com.invasion.entity.EntityIMFlying;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.pathfinding.FlyingNavigation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

public class FlyingCircleTargetGoal extends Goal {
    private static final int ATTACK_SEARCH_TIME = 400;
    private EntityIMFlying mob;

    private int patienceTime;
    private int patience;
    private float preferredHeight;
    private float preferredRadius;

    public FlyingCircleTargetGoal(EntityIMFlying entity, int patience, float preferredHeight, float preferredRadius) {
        mob = entity;
        this.patience = patience;
        this.preferredHeight = preferredHeight;
        this.preferredRadius = preferredRadius;
    }

    @Override
    public boolean canUse() {
        return mob.hasGoal(HasAiGoals.Goal.STAY_AT_RANGE) && hasValidTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.hasOrIsBetweenGoals(HasAiGoals.Goal.STAY_AT_RANGE, HasAiGoals.Goal.FIND_ATTACK_OPPORTUNITY) && hasValidTarget();
    }

    private boolean hasValidTarget() {
        Entity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        FlyingNavigation nav = (FlyingNavigation)mob.getNavigatorNew();
        nav.setMovementType(FlyingNavigation.MoveType.PREFER_FLYING);
        nav.setCirclingPath(mob.getTarget(), this.preferredHeight, this.preferredRadius);

        int extraTime = Math.max(0, (int) (4 * nav.getDistanceToCirclingRadius()));
        this.patienceTime = (extraTime + mob.getRandom().nextInt(this.patience) + this.patience / 3);
    }

    @Override
    public void tick() {
        if (mob.hasGoal(HasAiGoals.Goal.STAY_AT_RANGE)) {
            if (--patienceTime <= 0) {
                mob.transitionAIGoal(HasAiGoals.Goal.FIND_ATTACK_OPPORTUNITY);
                patienceTime = ATTACK_SEARCH_TIME;
            }
        } else if (mob.isBetweenGoals(HasAiGoals.Goal.STAY_AT_RANGE, HasAiGoals.Goal.FIND_ATTACK_OPPORTUNITY)) {
            patienceTime--;
        }
    }
}