package com.invasion.entity.ai.goal;

import com.invasion.entity.NexusEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Arrow;

@Deprecated
public class EntityAIKillWithArrow<T extends LivingEntity>
        extends KillEntityGoal<T> {
    private final float attackRangeSq;

    public <E extends PathfinderMob & NexusEntity> EntityAIKillWithArrow(
            E entity, Class<? extends T> targetClass, int attackDelay,
            float attackRange) {
        super(entity, targetClass, attackDelay);
        attackRangeSq = attackRange * attackRange;
    }

    @Override
    public void start() {
        super.start();
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        mob.setAggressive(false);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (mob.distanceToSqr(target) < 36 && mob.hasLineOfSight(target)) {
            navigation.haltForTick();
        }
    }

    @Override
    protected void attackEntity(Entity target) {
        setAttackTime(getAttackDelay());
        if (target instanceof LivingEntity living
                && mob instanceof RangedAttackMob attacker) {
            attacker.performRangedAttack(living, 1);
        } else {
            Arrow projectile = new Arrow(mob.level(), mob);
            double dx = target.getX() - mob.getX();
            double dy = target.getY(0.3333333333333333) - projectile.getY();
            double dz = target.getZ() - mob.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            projectile.shoot(dx, dy + horizontal * 0.2F, dz, 1.6F,
                    14 - mob.level().getDifficulty().getId() * 4);
            mob.playSound(SoundEvents.SKELETON_SHOOT, 1,
                    1 / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
            mob.level().addFreshEntity(projectile);
        }
    }

    @Override
    protected boolean canAttackEntity(Entity target) {
        return getAttackTime() <= 0
                && mob.distanceToSqr(target) < attackRangeSq
                && mob.hasLineOfSight(target);
    }
}
