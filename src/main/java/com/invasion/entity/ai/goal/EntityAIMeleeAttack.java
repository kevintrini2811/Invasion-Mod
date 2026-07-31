package com.invasion.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.server.level.ServerLevel;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.NexusEntity;

public class EntityAIMeleeAttack<T extends LivingEntity, E extends PathfinderMob & NexusEntity> extends Goal {
	protected final E mob;
	private final Class<? extends T> targetClass;
	private float attackRange = 0.6F;
	private int attackDelay;
	private int nextAttack;

	public EntityAIMeleeAttack(E entity, Class<? extends T> targetClass, int attackDelay) {
		this.mob = entity;
		this.targetClass = targetClass;
		this.attackDelay = attackDelay;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
    public boolean canUse() {
		LivingEntity target = mob.getTarget();
		return target != null && target.isAlive() && mob.hasGoal(HasAiGoals.Goal.MELEE_TARGET)
		        && mob.distanceToSqr(target) < (attackRange + mob.getBbWidth() + target.getBbWidth()) * 4
		        && target.getClass().isAssignableFrom(targetClass);
	}

	@Override
    public void tick() {
		LivingEntity target = mob.getTarget();
		if (canAttackEntity(target)) {
			attackEntity(target);
		}
		setAttackTime(getAttackTime() - 1);
	}

	public Class<? extends T> getTargetClass() {
		return this.targetClass;
	}

	protected void attackEntity(LivingEntity target) {
		mob.doHurtTarget(target);
		setAttackTime(getAttackDelay());
	}

	protected boolean canAttackEntity(LivingEntity target) {
		return getAttackTime() <= 0 && mob.distanceToSqr(target.position()) < Mth.square(mob.getBbWidth() + attackRange);
	}

	protected int getAttackTime() {
		return nextAttack;
	}

	protected void setAttackTime(int time) {
		nextAttack = time;
	}

	protected int getAttackDelay() {
		return attackDelay;
	}

	protected void setAttackDelay(int time) {
		attackDelay = time;
	}
}