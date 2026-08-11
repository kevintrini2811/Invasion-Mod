package com.invasion.entity.ai.goal;

import java.util.EnumSet;

import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.Stunnable;
import com.invasion.entity.ai.ClimbableMoveControl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SprintGoal<T extends PathAwareEntity & NexusEntity> extends net.minecraft.entity.ai.goal.Goal {
    private static final EntityAttributeModifier SPRINTING_SPEED_BOOST = new EntityAttributeModifier(
            InvasionMod.id("sprinting"), 2.3F, Operation.ADD_MULTIPLIED_BASE
    );

    protected final T theEntity;

    private int updateTimer;
    private int timer;
    private int missingTarget;

    private boolean isExecuting = true;
    private boolean isInWindup;

    protected Vec3d lastPos = Vec3d.ZERO;

    public SprintGoal(T entity) {
        theEntity = entity;
        setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (--updateTimer <= 0) {
            updateTimer = 20;
            if ((theEntity.getTarget() != null && theEntity.getVisibilityCache().canSee(theEntity.getTarget())) || theEntity.isSprinting()) {
                return true;
            }

            isExecuting = false;
            return false;
        }

        return isExecuting;
    }

    @Override
    public void start() {
        isExecuting = true;
        timer = 60;
    }

    @Override
    public void tick() {
        if (theEntity.isSprinting()) {
            Entity target = theEntity.getTarget();
            if (!theEntity.isSprinting() || target == null || (missingTarget > 0 && ++missingTarget > 20)) {
                endSprint();
                return;
            }

            double dX = target.getX() - theEntity.getX();
            double dZ = target.getZ() - theEntity.getZ();
            double dAngle = MathHelper.wrapDegrees(Math.atan2(dZ, dX) * MathHelper.DEGREES_PER_RADIAN - 90 - theEntity.getYaw());
            if (dAngle > 60) {
                setTurnRate(2);
                missingTarget = 1;
            }

            if (theEntity.squaredDistanceTo(lastPos) < 0.0009D) {
                crash();
                return;
            }

            lastPos = theEntity.getPos();
        }

        if (--timer <= 0) {
            if (!isInWindup) {
                if (!theEntity.isSprinting()) {
                    startSprint();
                } else {
                    endSprint();
                }
            } else {
                sprint();
            }
        }
    }

    protected void startSprint() {
        Entity target = theEntity.getTarget();
        if ((target == null) || (target.getY() - theEntity.getY() >= 1)) {
            return;
        }
        double dX = target.getX() - theEntity.getX();
        double dZ = target.getZ() - theEntity.getZ();
        double dAngle = MathHelper.wrapDegrees(Math.atan2(dZ, dX) * MathHelper.DEGREES_PER_RADIAN - 90 - theEntity.getYaw());
        if (dAngle < 10) {
            isInWindup = true;
            timer = 20;
            theEntity.stopMovement();
        } else {
            timer = 10;
        }
    }

    protected void sprint() {
        isInWindup = false;
        missingTarget = 0;
        timer = 35;
        EntityAttributeInstance attribute = theEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (!attribute.hasModifier(SPRINTING_SPEED_BOOST.id())) {
            attribute.addTemporaryModifier(SPRINTING_SPEED_BOOST);
        }
        theEntity.setSprinting(true);
        setTurnRate(4.9F);
        theEntity.setAttacking(false);
    }

    protected void endSprint() {
        timer = 180;
        theEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).removeModifier(SPRINTING_SPEED_BOOST.id());
        setTurnRate(30);
        theEntity.setSprinting(false);
    }

    private void setTurnRate(float turnRate) {
        if (theEntity.getMoveControl() instanceof ClimbableMoveControl control) {
            control.setTurnRate(turnRate);
        }
    }

    protected void crash() {
        if (theEntity instanceof Stunnable i) {
            i.stun(40);
        }
        theEntity.damage(theEntity.getDamageSources().generic(), 5);
        theEntity.playSound(InvSounds.ENTITY_CRASH, 1F, 0.6F);
        endSprint();
    }
}
