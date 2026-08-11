package com.invasion.entity.ai.goal;

import com.invasion.InvasionMod;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.entity.ZombieBuilderEntity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.Hand;

public class AttackNexusGoal<E extends PathAwareEntity & NexusEntity> extends Goal {
    private E mob;

    private int cooldown;

    public AttackNexusGoal(E mob) {
        this.mob = mob;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public boolean canStart() {
        return shouldContinue();
    }

    @Override
    public boolean shouldContinue() {
        return (mob.getTarget() == null
                    || mob instanceof PigmanEngineerEntity
                    || mob instanceof ZombieBuilderEntity)
                && mob.hasGoal(HasAiGoals.Goal.BREAK_NEXUS)
                && mob.findDistanceToNexus() <= 4;
    }

    @Override
    public void start() {
        cooldown = 40;
    }

    @Override
    public void tick() {
        if (--cooldown <= 0) {
            if (mob.findDistanceToNexus() <= 4) {
                mob.swingHand(Hand.MAIN_HAND);
                mob.getNexus().damage(mob.getDamageSources().mobAttack(mob), 2);
            }
            cooldown = 20;
            mob.setAttacking(true);
        }
    }

    @Override
    public void stop() {
        InvasionMod.LOGGER.info("Break Nexus Goal Stop");
        mob.setAttacking(false);
    }
}
