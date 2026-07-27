package com.invasion.entity.ai.goal;

import com.invasion.entity.SpiderEggEntity;
import com.invasion.entity.NexusEntity;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public class LayEggGoal extends Goal {
    private static final int EGG_LAY_TIME = 45;
    private static final int INITIAL_EGG_DELAY = 25;
    private static final int NEXT_EGG_DELAY = 230;
    private static final int EGG_HATCH_TIME = 125;

    private final PathfinderMob theEntity;
    private final Supplier<List<Entity>> offspringSupplier;

    private int time;
    private boolean isLaying;
    private int eggCount;

    public LayEggGoal(PathfinderMob entity, int eggs, Supplier<List<Entity>> offspringSupplier) {
        theEntity = entity;
        eggCount = eggs;
        this.offspringSupplier = offspringSupplier;
    }

    public void addEggs(int eggs) {
        eggCount += eggs;
    }

    @Override
    public boolean canUse() {
        boolean visibleTarget = theEntity.getTarget() != null
            && theEntity.getSensing().hasLineOfSight(theEntity.getTarget());
        boolean attackingNexus = theEntity instanceof NexusEntity nexus && nexus.hasNexus();
        return eggCount > 0 && (visibleTarget || attackingNexus);
    }

    @Override
    public void start() {
        time = INITIAL_EGG_DELAY;
    }

    @Override
    public void tick() {
        if (--time <= 0) {
            if (!isLaying) {
                isLaying = true;
                time = EGG_LAY_TIME;
            } else {
                isLaying = false;
                eggCount--;
                time = NEXT_EGG_DELAY;
                layEgg();
            }
        }
    }

    private void layEgg() {
        theEntity.level().addFreshEntity(new SpiderEggEntity(theEntity, offspringSupplier.get(), EGG_HATCH_TIME));
    }
}
