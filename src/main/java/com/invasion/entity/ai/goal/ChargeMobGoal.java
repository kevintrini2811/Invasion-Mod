package com.invasion.entity.ai.goal;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.EntityIMZombiePigman;
import com.invasion.entity.NexusEntity;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

public class ChargeMobGoal<T extends LivingEntity> extends MoveToEntityGoal<T> {
    @Nullable
    protected LivingEntity target;

    protected Vec3 chargePos = Vec3.ZERO;

    protected float speed;
    protected int windup;
    protected boolean hasAttacked;

    protected int chargeDelay = 100;
    protected int runTime = 15;

    public <E extends PathfinderMob & NexusEntity> ChargeMobGoal(E entity, Class<? extends T> targetClass, float f) {
        super(entity, targetClass);
        this.speed = f;
    }

    @Override
    public boolean canUse() {

        if (chargeDelay > 0) {
            chargeDelay--;
            return false;
        }

        target = mob.getTarget();
        if (target == null || target.isRemoved() || target.isDeadOrDying() || !mob.onGround()) {
            return false;
        }
        double distance = Math.sqrt(mob.distanceToSqr(target));
        if (distance < 5 || distance > 20) {
            return false;
        }

        chargePos = findChargePoint(mob, target, 6);

        return mob.getRandom().nextInt(1) == 0;
    }

    @Override
    public void start() {
        windup = (15 + mob.getRandom().nextInt(25));
    }

    @Override
    public boolean canContinueToUse() {
        if (windup == 0 && runTime > 0) {
            runTime--;
        }
        return windup > 0 || runTime > 0;
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(chargePos.x, chargePos.y, chargePos.z, 10.0F, /* mob.getTurnRate()*/ 10);
        if (windup > 0) {
            if (--windup == 0) {
                mob.getNavigation().moveTo(chargePos.x(), chargePos.y(), chargePos.z(), speed);
            } else {
                mob.walkAnimation.setSpeed(mob.walkAnimation.speed() + 0.8F);
                if (mob instanceof EntityIMZombiePigman pig) {
                    pig.setCharging(true);
                }
            }
        }

        if (!hasAttacked && mob.distanceToSqr(chargePos) <= Mth.square(mob.getBbWidth() * 2.1F)) {
            hasAttacked = true;
            mob.doHurtTarget((ServerLevel) mob.level(), target);
        }
    }

    @Override
    public void stop() {
        windup = 0;
        target = null;
        hasAttacked = false;
        chargeDelay = 100;
        runTime = 15;
        if (mob instanceof EntityIMZombiePigman pig) {
            pig.setCharging(false);
        }
    }

    protected Vec3 findChargePoint(Entity attacker, Entity target, double overshoot) {
        Vec3 pos = mob.position();
        Vec3 delta = target.position().subtract(pos).multiply(1, 0, 1);
        float theta = (float) Math.atan2(delta.z(), delta.x());
        double distance = delta.length() + overshoot;
        // Cylindrical to Cartesian
        return pos.add(
                distance * Mth.cos(theta),
                0,
                distance * Mth.sin(theta)
        );
    }
}
