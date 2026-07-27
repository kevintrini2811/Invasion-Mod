package com.invasion.entity.ai.goal;

import com.invasion.entity.VultureEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.server.level.ServerLevel;
import com.invasion.entity.HasAiGoals;

public class FlyingStrikeGoal extends Goal {
    private final VultureEntity theEntity;

    public FlyingStrikeGoal(VultureEntity entity) {
        theEntity = entity;
    }

    @Override
    public boolean canUse() {
        return theEntity.hasAnyGoal(HasAiGoals.Goal.FLYING_STRIKE, HasAiGoals.Goal.SWOOP);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (theEntity.hasGoal(HasAiGoals.Goal.FLYING_STRIKE)) {
            doStrike();
        }
    }

    private void doStrike() {
        LivingEntity target = theEntity.getTarget();
        if (target == null) {
            theEntity.transitionAIGoal(HasAiGoals.Goal.NONE);
            return;
        }

        float flyByChance = 1;
        float tackleChance = 0;
        float pickUpChance = 0;
        if (theEntity.getClawsForward()) {
            flyByChance = 0.5F;
            tackleChance = 100;
            pickUpChance = 1;
        }

        float pE = flyByChance + tackleChance + pickUpChance;
        float r = theEntity.getRandom().nextFloat();
        if (r <= flyByChance / pE) {
            doFlyByAttack(target);
            theEntity.transitionAIGoal(HasAiGoals.Goal.STABILISE);
            theEntity.setClawsForward(false);
        } else if (r <= (flyByChance + tackleChance) / pE) {
            theEntity.transitionAIGoal(HasAiGoals.Goal.TACKLE_TARGET);
            theEntity.setClawsForward(false);
        } else {
            theEntity.transitionAIGoal(HasAiGoals.Goal.PICK_UP_TARGET);
        }
    }

    private void doFlyByAttack(LivingEntity entity) {
        this.theEntity.doHurtTarget((ServerLevel) theEntity.level(), entity);
    }
}
