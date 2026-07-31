package com.invasion.entity.ai.goal;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.VultureEntity;
import net.minecraft.world.entity.LivingEntity;

public class WingAttackGoal<T extends LivingEntity> extends EntityAIMeleeAttack<T, VultureEntity> {
    private VultureEntity mob;

    public WingAttackGoal(VultureEntity entity, Class<? extends T> targetClass, int attackDelay) {
        super(entity, targetClass, attackDelay);
        mob = entity;
    }

    @Override
    public void tick() {
        if (getAttackTime() == 0) {
            mob.setAttackingWithWings(isInStartMeleeRange());
        }
        super.tick();
    }

    @Override
    public void stop() {
        mob.setAttackingWithWings(false);
    }

    protected boolean isInStartMeleeRange() {
        @Nullable
        LivingEntity target = mob.getTarget();
        return target != null && mob.closerThan(target, mob.getBbWidth() + 3);
    }
}