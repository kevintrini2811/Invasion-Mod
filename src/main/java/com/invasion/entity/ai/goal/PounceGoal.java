package com.invasion.entity.ai.goal;

import com.invasion.entity.NexusSpiderEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class PounceGoal extends Goal {
    private final NexusSpiderEntity theEntity;
    private final float minPower;
    private final float maxPower;

    private boolean isPouncing;
    private int pounceTimer;
    private int cooldown;

    private int airborneTime;

    public PounceGoal(NexusSpiderEntity entity, float minPower, float maxPower, int cooldown) {
        this.theEntity = entity;
        this.minPower = minPower;
        this.maxPower = maxPower;
        this.cooldown = cooldown;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = theEntity.getTarget();
        return --pounceTimer <= 0
                && target != null
                && theEntity.getSensing().hasLineOfSight(target)
                && theEntity.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        return isPouncing;
    }

    @Override
    public void start() {
        if (pounce(theEntity.getTarget().getEyePosition())) {
            airborneTime = 0;
            isPouncing = true;
            theEntity.getNavigatorNew().haltForTick();
        } else {
            isPouncing = false;
        }
    }

    @Override
    public void tick() {
        theEntity.getNavigatorNew().haltForTick();
        if (airborneTime > 20 && theEntity.onGround()) {
            isPouncing = false;
            pounceTimer = cooldown;
            airborneTime = 0;
            theEntity.getNavigation().stop();
        } else {
            airborneTime++;
        }
    }

    protected boolean pounce(Vec3 pos) {
        Vec3 delta = pos.subtract(theEntity.position());
        double dXZ = delta.horizontalDistance();
        double a = Math.atan(delta.y / dXZ);

        if (Math.abs(a) > 0.4853981633974483D) {
            double radius = (dXZ / ((1 - Math.tan(a)) / Math.cos(a))) * theEntity.getGravity();
            double power = 1D / Math.sqrt(1D / radius);

            if (power > minPower && power < maxPower) {
                double distance = Mth.SQRT_OF_TWO * dXZ;
                theEntity.push(
                        (power * delta.x / distance),
                        (power * dXZ / distance),
                        (power * delta.z / distance)
                );
                theEntity.lookAt(Anchor.EYES, pos);
                return true;
            }
        }
        return false;
    }
}