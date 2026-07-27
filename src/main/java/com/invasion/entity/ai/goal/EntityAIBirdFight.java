package com.invasion.entity.ai.goal;

import com.invasion.entity.VultureEntity;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.pathfinding.FlyingNavigation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

public class EntityAIBirdFight<T extends LivingEntity> extends MeleeFightGoal<T, VultureEntity> {
    private final VultureEntity theEntity;
    private final PathNavigation navigation;
    private final FlyingNavigation flyingNavigation;
    private boolean wantsToRetreat;
    private boolean buffetedTarget;

    public EntityAIBirdFight(VultureEntity entity, Class<? extends T> targetClass, int attackDelay, float retreatHealthLossPercent) {
        super(entity, targetClass, attackDelay, retreatHealthLossPercent);
        theEntity = entity;
        navigation = entity.getNavigation();
        flyingNavigation = (FlyingNavigation)theEntity.getNavigatorNew();
    }

    @Override
    public void tick() {
        if (getAttackTime() == 0) {
            theEntity.setAttackingWithWings(isInStartMeleeRange());
        }
        super.tick();
    }

    @Override
    public void stop() {
        theEntity.setAttackingWithWings(false);
        super.stop();
    }

    @Override
    public void updatePath() {
        Entity target = mob.getTarget();
        if (target != mob.getNavigatorNew().getTargetEntity()) {
            navigation.stop();
            ((FlyingNavigation)theEntity.getNavigatorNew()).setMovementType(FlyingNavigation.MoveType.PREFER_WALKING);
            Path path = theEntity.getNavigation().createPath(target, Mth.ceil(1.6D * mob.distanceTo(target)));
            if (path != null && path.getNodeCount() > 1.6D * mob.distanceTo(target)) {
                ((FlyingNavigation)theEntity.getNavigatorNew()).setMovementType(FlyingNavigation.MoveType.MIXED);
            }
            flyingNavigation.autoPathToEntity(target);
        }
    }

    @Override
    protected void updateDisengage() {
        if (!wantsToRetreat) {
            if (shouldLeaveMelee()) {
                wantsToRetreat = true;
            }
        } else if (buffetedTarget && mob.hasGoal(HasAiGoals.Goal.MELEE_TARGET)) {
            mob.transitionAIGoal(HasAiGoals.Goal.LEAVE_MELEE);
        }
    }

    @Override
    protected void attackEntity(LivingEntity target) {
        theEntity.doMeleeSound();
        super.attackEntity(target);
        if (wantsToRetreat) {
            doWingBuffetAttack(target);
            buffetedTarget = true;
        }
    }

    protected boolean isInStartMeleeRange() {
        LivingEntity target = mob.getTarget();
        return target != null && mob.closerThan(target, mob.getBbWidth() + 3);
    }

    protected void doWingBuffetAttack(LivingEntity target) {
        target.knockback(2, target.getX() - mob.getX(), target.getZ() - mob.getZ(),
                mob.damageSources().mobAttack(mob), (float) mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        target.level().playLocalSound(target.blockPosition(), SoundEvents.GENERIC_BIG_FALL, target.getSoundSource(), 1, 1, true);
    }
}
