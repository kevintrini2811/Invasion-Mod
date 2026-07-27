package com.invasion.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.Stunnable;
import com.invasion.entity.ai.ClimbableMoveControl;

public class SprintGoal<T extends PathfinderMob & NexusEntity> extends net.minecraft.world.entity.ai.goal.Goal {
    private static final AttributeModifier SPRINTING_SPEED_BOOST = new AttributeModifier(
            InvasionMod.id("sprinting"), 1.3F, Operation.ADD_MULTIPLIED_BASE
    );

    protected final T theEntity;

    private int updateTimer;
    private int timer;
    private int missingTarget;

    private boolean isExecuting = true;
    private boolean isInWindup;

    protected Vec3 lastPos = Vec3.ZERO;

    public SprintGoal(T entity) {
        theEntity = entity;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (--updateTimer <= 0) {
            updateTimer = 20;
            if ((theEntity.getTarget() != null && theEntity.getSensing().hasLineOfSight(theEntity.getTarget())) || theEntity.isSprinting()) {
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
            double dAngle = Mth.wrapDegrees(Math.atan2(dZ, dX) * Mth.RAD_TO_DEG - 90 - theEntity.getYRot());
            if (dAngle > 60) {
                ((ClimbableMoveControl)theEntity.getMoveControl()).setTurnRate(2);
                missingTarget = 1;
            }

            if (theEntity.distanceToSqr(lastPos) < 0.0009D) {
                crash();
                return;
            }

            lastPos = theEntity.position();
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
        double dAngle = Mth.wrapDegrees(Math.atan2(dZ, dX) * Mth.RAD_TO_DEG - 90 - theEntity.getYRot());
        if (dAngle < 10) {
            isInWindup = true;
            timer = 20;
            theEntity.stopInPlace();
        } else {
            timer = 10;
        }
    }

    protected void sprint() {
        isInWindup = false;
        missingTarget = 0;
        timer = 35;
        AttributeInstance attribute = theEntity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (!attribute.hasModifier(SPRINTING_SPEED_BOOST.id())) {
            attribute.addTransientModifier(SPRINTING_SPEED_BOOST);
        }
        theEntity.setSprinting(true);
        ((ClimbableMoveControl)theEntity.getMoveControl()).setTurnRate(4.9F);
        theEntity.setAggressive(false);
    }

    protected void endSprint() {
        timer = 180;
        theEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPRINTING_SPEED_BOOST.id());
        ((ClimbableMoveControl)theEntity.getMoveControl()).setTurnRate(30);
        theEntity.setSprinting(false);
    }

    protected void crash() {
        if (theEntity instanceof Stunnable i) {
            i.stun(40);
        }
        theEntity.hurt(theEntity.damageSources().generic(), 5);
        theEntity.playSound(InvSounds.ENTITY_CRASH, 1F, 0.6F);
        endSprint();
    }
}
