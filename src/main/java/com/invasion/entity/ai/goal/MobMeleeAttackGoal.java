package com.invasion.entity.ai.goal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class MobMeleeAttackGoal extends MeleeAttackGoal {
    private int ticks;

    public MobMeleeAttackGoal(PathfinderMob mob, double speed, boolean pauseWhenMobIdle) {
        super(mob, speed, pauseWhenMobIdle);
    }

    @Override
    public void start() {
        super.start();
        ticks = 0;
    }

    @Override
    public void stop() {
        super.stop();
        mob.setAggressive(false);
    }

    @Override
    public void tick() {
        super.tick();
        mob.setAggressive(++ticks >= 5 && getTicksUntilNextAttack() < getAttackInterval() / 2);
    }
}