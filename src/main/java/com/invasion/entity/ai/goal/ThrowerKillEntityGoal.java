package com.invasion.entity.ai.goal;

import com.invasion.entity.ThrowerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ThrowerKillEntityGoal<T extends LivingEntity> extends KillEntityGoal<T> {
    private boolean melee;
    private final ThrowerEntity theEntity;
    private int maxBoulderAmount = 3;

    public ThrowerKillEntityGoal(ThrowerEntity entity, Class<? extends T> targetClass, int attackDelay, float throwRange, float launchSpeed) {
        super(entity, targetClass, attackDelay);
        this.theEntity = entity;
    }

    @Override
    protected void attackEntity(Entity target) {
        if (melee) {
            setAttackTime(getAttackDelay());
            super.attackEntity(target);
        } else {
            setAttackTime(getAttackDelay() * 2);
            int distance = Math.round(theEntity.distanceTo(target));
            int missDistance = Math.round((float) Math.ceil(distance / 10));

            for (int i = 1; i <= theEntity.getRandom().nextInt(maxBoulderAmount); i++) {
                double x = (target.getX() - missDistance) + theEntity.getRandom().nextInt((missDistance + 1) * 2);
                double y = (target.getY() - missDistance + 1) + theEntity.getRandom().nextInt((missDistance + 1) * 2);
                double z = (target.getZ() - missDistance) + theEntity.getRandom().nextInt((missDistance + 1) * 2);

                theEntity.throwProjectile(new Vec3(x, y, z));
            }
        }
    }

    @Override
    protected boolean canAttackEntity(Entity target) {
        melee = super.canAttackEntity(target);
        if (melee) {
            return true;
        }
        if (!theEntity.canThrow()) {
            return false;
        }

        double dXY = theEntity.position().subtract(target.position()).horizontalDistance();
        return getAttackTime() <= 0 && theEntity.getSensing().hasLineOfSight(target) && theEntity.getThrowPower(dXY) <= 1;
    }
}