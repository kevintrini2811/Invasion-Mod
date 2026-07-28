package com.invasion.entity.ai.goal;

import com.invasion.InvasionMod;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.NexusEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class AttackNexusGoal<E extends PathfinderMob & NexusEntity> extends Goal {
    private E mob;

    private int cooldown;

    public AttackNexusGoal(E mob) {
        this.mob = mob;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return canContinueToUse();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.hasGoal(HasAiGoals.Goal.BREAK_NEXUS) && mob.findDistanceToNexus() <= 4;
    }

    @Override
    public void start() {
        cooldown = 40;
    }

    @Override
    public void tick() {
        if (--cooldown <= 0) {
            if (mob.findDistanceToNexus() <= 4) {
                mob.swing(InteractionHand.MAIN_HAND);
                mob.getNexus().damage(mob.damageSources().mobAttack(mob), 2);
            }
            cooldown = 20;
            mob.setAggressive(true);
        }
    }

    @Override
    public void stop() {
        InvasionMod.LOGGER.debug("Break Nexus Goal Stop");
        mob.setAggressive(false);
    }
}