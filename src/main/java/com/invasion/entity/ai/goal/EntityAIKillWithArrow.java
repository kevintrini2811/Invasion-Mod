package com.invasion.entity.ai.goal;

import com.invasion.entity.NexusEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Items;

/**
 * AI for an entity to shoot arrows.
 * Is this even being used?
 */
@Deprecated
public class EntityAIKillWithArrow<T extends LivingEntity> extends KillEntityGoal<T> {
	private float attackRangeSq;

	public <E extends PathfinderMob & NexusEntity> EntityAIKillWithArrow(E entity, Class<? extends T> targetClass, int attackDelay, float attackRange) {
		super(entity, targetClass, attackDelay);
		this.attackRangeSq = (attackRange * attackRange);
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
		if (target instanceof LivingEntity l && mob instanceof RangedAttackMob attacker) {
		    attacker.performRangedAttack(l, 1);
		} else {
    		Arrow projectile = new Arrow(mob.level(), mob, Items.ARROW.getDefaultInstance(), null);
            double dX = target.getX() - mob.getX();
            double dY = target.getY(0.3333333333333333) - projectile.getY();
            double dZ = target.getZ() - mob.getZ();
            double horLength = Math.sqrt(dX * dX + dZ * dZ);
            projectile.shoot(dX, dY + horLength * 0.2F, dZ, 1.6F, 14 - mob.level().getDifficulty().getId() * 4);
            mob.playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
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
