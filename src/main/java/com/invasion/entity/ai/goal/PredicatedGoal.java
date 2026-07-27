package com.invasion.entity.ai.goal;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

public class PredicatedGoal extends Goal {
    private final Goal goal;
    private final BooleanSupplier predicate;

    public PredicatedGoal(Goal goal, BooleanSupplier predicate) {
        this.goal = goal;
        this.predicate = predicate;
        setFlags(goal.getFlags());
    }

    @Override
    public boolean canUse() {
        return predicate.getAsBoolean() && goal.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return predicate.getAsBoolean() && goal.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return !predicate.getAsBoolean() || goal.isInterruptable();
    }

    @Override
    public void start() {
        goal.start();
    }

    @Override
    public void stop() {
        goal.stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return goal.requiresUpdateEveryTick();
    }

    @Override
    public void tick() {
        goal.tick();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o != null && getClass() == o.getClass() ? goal.equals(((PredicatedGoal)o).goal) : false;
    }

    @Override
    public int hashCode() {
        return goal.hashCode();
    }
}
