package com.invasion.entity.ai.goal;

import com.invasion.entity.EntityIMLiving;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import net.minecraft.world.entity.LivingEntity;

@Deprecated(since = "unused")
public class EntityAILeaderTarget<T extends LivingEntity> extends CustomRangeActiveTargetGoal<T> {

	private int rallyCooldown;

	public EntityAILeaderTarget(EntityIMLiving entity, Class<T> targetType, float distance) {
		this(entity, targetType, distance, true);
	}

	public EntityAILeaderTarget(EntityIMLiving entity, Class<T> targetType, float distance, boolean needsLos) {
		super(entity, targetType, distance, needsLos);
	}

	@Override
    public boolean canUse() {
	    if (rallyCooldown > 0) {
	        rallyCooldown--;
	    }
		return rallyCooldown <= 0 && super.canUse();
	}

	@Override
    public void tick() {
        super.tick();
        if (rallyCooldown > 0) {
            rallyCooldown--;
        }
	}
}