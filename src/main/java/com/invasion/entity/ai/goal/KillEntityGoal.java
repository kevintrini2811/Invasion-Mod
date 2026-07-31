package com.invasion.entity.ai.goal;

import com.invasion.entity.NexusEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.server.level.ServerLevel;

@Deprecated
public class KillEntityGoal<T extends LivingEntity> extends MoveToEntityGoal<T> {
    private static final float ATTACK_RANGE = 1;

    private int attackDelay;
    private int nextAttack;

    public <E extends PathfinderMob & NexusEntity> KillEntityGoal(E entity, Class<? extends T> targetClass, int attackDelay) {
        super(entity, targetClass);
        this.attackDelay = attackDelay;
    }

    @Override
    public void tick() {
        super.tick();
        setAttackTime(getAttackTime() - 1);
        Entity target = getTarget();
        if (canAttackEntity(target)) {
            attackEntity(target);
        }
    }

    protected void attackEntity(Entity target) {
        mob.doHurtTarget(getTarget());
        setAttackTime(getAttackDelay());
    }

    protected boolean canAttackEntity(Entity target) {
        if (getAttackTime() > 0) {
            return false;
        }

        double d = (mob.getBbWidth() + ATTACK_RANGE);
        return mob.distanceToSqr(target) < d * d;
    }

    protected int getAttackTime() {
        return nextAttack;
    }

    protected void setAttackTime(int time) {
        nextAttack = time;
    }

    protected int getAttackDelay() {
        return attackDelay;
    }

    protected void setAttackDelay(int time) {
        attackDelay = time;
    }
}