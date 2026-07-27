package com.invasion.entity.ai.goal;

import org.joml.Vector3f;

import com.invasion.entity.VultureEntity;
import com.invasion.entity.pathfinding.FlyingNavigation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.invasion.entity.HasAiGoals;

public class PickUpEntityGoal extends net.minecraft.world.entity.ai.goal.Goal {
    private final VultureEntity theEntity;

    private final Vector3f pickupPoint;

    private final float pickupRangeY;
    private final float pickupRangeXZ;

    private final float abortAngleYaw;
    private final float abortAnglePitch;

    private int time;
    private int holdTime = 70;
    private int abortTime;
    private boolean isHoldingEntity;

    public PickUpEntityGoal(VultureEntity entity, Vector3f pickupPoint, float pickupRangeY, float pickupRangeXZ, int abortTime, float abortAngleYaw, float abortAnglePitch) {
        this.theEntity = entity;
        this.pickupPoint = pickupPoint;
        this.pickupRangeY = pickupRangeY;
        this.pickupRangeXZ = pickupRangeXZ;
        this.abortTime = abortTime;
        this.abortAngleYaw = abortAngleYaw;
        this.abortAnglePitch = abortAnglePitch;
    }

    @Override
    public boolean canUse() {
        return theEntity.hasGoal(HasAiGoals.Goal.PICK_UP_TARGET) || theEntity.isVehicle();
    }

    @Override
    public void start() {
        isHoldingEntity = theEntity.isVehicle();
        time = 0;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = theEntity.getTarget();
        if (target != null && target.isAlive()) {
            if (!isHoldingEntity) {
                if (time > abortTime && isLinedUp(target)) {
                    return true;
                }
            } else if (theEntity.isPassengerOfSameVehicle(target)) {
                return true;
            }
        }
        theEntity.transitionAIGoal(HasAiGoals.Goal.NONE);
        theEntity.setClawsForward(false);
        return false;
    }

    @Override
    public void tick() {
        time++;
        if (!isHoldingEntity) {
            LivingEntity target = theEntity.getTarget();
            double dY = target.yo - theEntity.yo;
            if (Math.abs(dY - pickupPoint.y) < pickupRangeY) {
                double dAngle = theEntity.yo * Mth.DEG_TO_RAD;
                double sinF = Math.sin(dAngle);
                double cosF = Math.cos(dAngle);
                double x = pickupPoint.x * cosF - pickupPoint.z * sinF;
                double z = pickupPoint.z * cosF + pickupPoint.x * sinF;

                double dX = target.xo - (x + theEntity.xo);
                double dZ = target.zo - (z + theEntity.zo);
                double dXZ = Math.sqrt(dX * dX + dZ * dZ);

                if (dXZ < pickupRangeXZ) {
                    target.startRiding(theEntity);
                    isHoldingEntity = true;
                    time = 0;
                    theEntity.getNavigation().stop();
                    ((FlyingNavigation)theEntity.getNavigatorNew()).setPitchBias(20, 1.5F);
                }
            }
        } else if (time == 45) {
            ((FlyingNavigation)theEntity.getNavigatorNew()).setPitchBias(0, 0);
        } else if (time > holdTime) {
            theEntity.getTarget().stopRiding();
        }
    }

    private boolean isLinedUp(Entity target) {
        Vec3 delta = target.position().subtract(theEntity.position());
        double dXZ = delta.horizontalDistance();
        double yawToTarget = Math.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG - 90;
        double dYaw = Mth.degreesDifference((float)yawToTarget, theEntity.getYRot());
        if (dYaw < -abortAngleYaw || dYaw > abortAngleYaw) {
            return false;
        }
        double dPitch = Math.atan(delta.y / dXZ) * Mth.RAD_TO_DEG - theEntity.getXRot();
        return dPitch >= -abortAnglePitch && dPitch <= abortAnglePitch;
    }
}