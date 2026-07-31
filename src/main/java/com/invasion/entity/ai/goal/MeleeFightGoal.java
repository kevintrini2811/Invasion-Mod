package com.invasion.entity.ai.goal;

import com.invasion.entity.HasAiGoals;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.Navigation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class MeleeFightGoal<T extends LivingEntity, E extends PathfinderMob & NexusEntity> extends EntityAIMeleeAttack<T, E> {
	private int time;
	private float startingHealth;
	private int damageDealt;
	private int invulnCount;
	private float retreatHealthLossPercent;

	public MeleeFightGoal(E entity, Class<? extends T> targetClass, int attackDelay, float retreatHealthLossPercent) {
		super(entity, targetClass, attackDelay);
		this.retreatHealthLossPercent = retreatHealthLossPercent;
	}

	@Override
    public boolean canUse() {
		return mob.hasGoal(HasAiGoals.Goal.MELEE_TARGET) && hasValidTarget();
	}

	@Override
    public boolean canContinueToUse() {
		return (mob.hasGoal(HasAiGoals.Goal.MELEE_TARGET) || isWaitingForTransition()) && hasValidTarget();
	}

	private boolean hasValidTarget() {
	    LivingEntity target = mob.getTarget();
	    return target != null && target.isAlive()
                && target.getClass().isAssignableFrom(getTargetClass());
	}

	@Override
    public void start() {
		time = 0;
		startingHealth = mob.getHealth();
		damageDealt = 0;
		invulnCount = 0;
	}

	@Override
    public void tick() {
		updateDisengage();
		updatePath();
		super.tick();
		if (damageDealt > 0 || startingHealth - mob.getHealth() > 0) {
			time++;
		}
	}

	public void updatePath() {
		Navigation nav = mob.getNavigatorNew();
		if (mob.getTarget() != nav.getTargetEntity()) {
			mob.getNavigation().stop();
			nav.autoPathToEntity(mob.getTarget());
		}
	}

	protected void updateDisengage() {
		if (mob.hasGoal(HasAiGoals.Goal.MELEE_TARGET) && shouldLeaveMelee()) {
			mob.transitionAIGoal(HasAiGoals.Goal.LEAVE_MELEE);
		}
	}

	protected boolean isWaitingForTransition() {
		return mob.hasGoal(HasAiGoals.Goal.LEAVE_MELEE) && mob.getPrevAIGoal() == HasAiGoals.Goal.MELEE_TARGET;
	}

	@Override
    protected void attackEntity(LivingEntity target) {
		float h = target.getHealth();
		super.attackEntity(target);
		h -= target.getHealth();
		if (h <= 0) {
			invulnCount++;
		}
		damageDealt += (int)h;
	}

	protected boolean shouldLeaveMelee() {
		float damageReceived = startingHealth - mob.getHealth();
		return (time > 40 && damageReceived > mob.getMaxHealth() * retreatHealthLossPercent)
	        || (time > 100 && damageReceived - damageDealt > mob.getMaxHealth() * 0.66F * retreatHealthLossPercent)
	        || invulnCount >= 2;
	}
}
