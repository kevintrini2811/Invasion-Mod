package com.invasion.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.invasion.entity.VultureEntity;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.ai.MoveState;
import com.invasion.entity.pathfinding.FlyingNavigation;

public class SwoopGoal extends Goal {
    private static final int INITIAL_LINEUP_TIME = 25;

    private final VultureEntity theEntity;

    private float minDiveClearanceY;

    @Nullable
    private LivingEntity swoopTarget;
    private float diveAngle;
    private float diveHeight;
    private float strikeDistance;
    private float minHeight = 6;
    private float minXZDistance = 10;
    private float maxSteepness = 40;
    private float finalRunLength = 4;
    private float finalRunArcLimit = 15;
    private int time;
    private boolean isCommittedToFinalRun;
    private boolean endSwoop;

    public SwoopGoal(VultureEntity entity) {
        theEntity = entity;
        strikeDistance = entity.getBbWidth() + 1.5F;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (theEntity.hasGoal(HasAiGoals.Goal.FIND_ATTACK_OPPORTUNITY) && theEntity.getTarget() != null) {
            swoopTarget = theEntity.getTarget();
            Vec3 delta = swoopTarget.position().subtract(theEntity.position());
            double dXZ = delta.horizontalDistance();
            if (-delta.y < minHeight || dXZ < minXZDistance) {
                return false;
            }
            double pitchToTarget = Math.atan(delta.y / dXZ) * Mth.RAD_TO_DEG;
            if (pitchToTarget > maxSteepness) {
                return false;
            }
            finalRunLength = Mth.clamp((float) (dXZ * 0.42D), 4, 18);
            diveAngle = (float) Math.atan((dXZ - finalRunLength) / delta.y) * Mth.RAD_TO_DEG;
            if (swoopTarget != null && isSwoopPathClear(swoopTarget, diveAngle)) {
                diveHeight = (float) -delta.y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return theEntity.getTarget() == swoopTarget && !endSwoop && theEntity.getMoveState() == MoveState.FLYING;
    }

    @Override
    public void start() {
        time = 0;
        theEntity.transitionAIGoal(HasAiGoals.Goal.SWOOP);
        ((FlyingNavigation)theEntity.getNavigatorNew()).setMovementType(FlyingNavigation.MoveType.PREFER_FLYING);
        theEntity.getNavigation().moveTo(swoopTarget, theEntity.getMaxPoweredFlightSpeed());
        theEntity.doScreech();
    }

    @Override
    public void stop() {
        endSwoop = false;
        isCommittedToFinalRun = false;
        ((FlyingNavigation)theEntity.getNavigatorNew()).enableDirectTarget(false);
        if (theEntity.hasGoal(HasAiGoals.Goal.SWOOP)) {
            theEntity.transitionAIGoal(HasAiGoals.Goal.NONE);
            theEntity.setClawsForward(false);
        }
    }

    @Override
    public void tick() {
        time++;
        if (!isCommittedToFinalRun) {
            if (theEntity.distanceTo(swoopTarget) < finalRunLength) {
                ((FlyingNavigation)theEntity.getNavigatorNew()).setPitchBias(0, 1);
                if (isFinalRunLinedUp()) {
                    theEntity.setClawsForward(true);
                    ((FlyingNavigation)theEntity.getNavigatorNew()).enableDirectTarget(true);
                    isCommittedToFinalRun = true;
                } else {
                    theEntity.transitionAIGoal(HasAiGoals.Goal.NONE);
                    endSwoop = true;
                }
            } else if (time > INITIAL_LINEUP_TIME) {
                double dYp = -(swoopTarget.getY() - theEntity.getY());
                if (dYp < 2.9) {
                    dYp = 0;
                }
                ((FlyingNavigation)theEntity.getNavigatorNew()).setPitchBias(diveAngle * (float) (dYp / diveHeight), (float) (0.6D * (dYp / diveHeight)));
            }

        } else if (theEntity.distanceTo(swoopTarget) < strikeDistance) {
            theEntity.transitionAIGoal(HasAiGoals.Goal.FLYING_STRIKE);
            ((FlyingNavigation)theEntity.getNavigatorNew()).enableDirectTarget(false);
            endSwoop = true;
        } else {
            double yawToTarget = Math.atan2(
                    swoopTarget.getZ() - theEntity.getZ(),
                    swoopTarget.getX() - theEntity.getX()
            ) * Mth.RAD_TO_DEG - 90;
            if (Math.abs(Mth.degreesDifference((float) yawToTarget, theEntity.getYRot())) > 90) {
                theEntity.transitionAIGoal(HasAiGoals.Goal.NONE);
                ((FlyingNavigation)theEntity.getNavigatorNew()).enableDirectTarget(false);
                theEntity.setClawsForward(false);
                endSwoop = true;
            }
        }
    }

    private boolean isSwoopPathClear(LivingEntity target, float diveAngle) {
        double dRayY = 2;
        int hitCount = 0;
        double lowestCollide = theEntity.getY();
        for (double y = theEntity.getY() - dRayY; y > target.getY(); y -= dRayY) {
            double dist = Math.tan(90 + diveAngle) * (theEntity.getY() - y);
            BlockHitResult collide = theEntity.level().clip(new ClipContext(new Vec3(
                    -Math.sin(theEntity.getYRot() * Mth.DEG_TO_RAD) * dist,
                    y,
                    Math.cos(theEntity.getYRot() * Mth.DEG_TO_RAD) * dist
            ), target.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, theEntity));
            if (collide != null && collide.getType() != Type.MISS) {
                if (hitCount == 0) {
                    lowestCollide = y;
                }
                hitCount++;
            }
        }

        return isAcceptableDiveSpace(theEntity.getY(), lowestCollide, hitCount);
    }

    private boolean isFinalRunLinedUp() {
        Vec3 delta = swoopTarget.position().subtract(theEntity.position());
        double dXZ = delta.horizontalDistance();
        double yawToTarget = Math.atan2(delta.x, delta.z) * Mth.RAD_TO_DEG - 90;
        double dYaw = Mth.degreesDifference((float) yawToTarget, theEntity.getYRot());
        if (dYaw < -finalRunArcLimit || dYaw > finalRunArcLimit) {
            return false;
        }
        double dPitch = Math.atan(delta.x / dXZ) * Mth.RAD_TO_DEG - theEntity.getXRot();
        return dPitch >= -finalRunArcLimit && dPitch <= finalRunArcLimit;
    }

    protected boolean isAcceptableDiveSpace(double entityPosY, double lowestCollideY, int hitCount) {
        return entityPosY - lowestCollideY >= minDiveClearanceY;
    }
}