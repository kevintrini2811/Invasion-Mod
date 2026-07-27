package com.invasion.entity.ai.goal.target;

import com.invasion.entity.NexusEntity;
import com.invasion.util.FloatSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class CustomRangeActiveTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
	private final FloatSupplier range;

    public CustomRangeActiveTargetGoal(Mob entity, Class<T> targetType, float range) {
        this(entity, targetType, range, true);
    }

    public CustomRangeActiveTargetGoal(Mob entity, Class<T> targetType, float range, boolean checkVisibility) {
        this(entity, targetType, () -> range, checkVisibility);
    }

	public CustomRangeActiveTargetGoal(Mob entity, Class<T> targetType, FloatSupplier range) {
		this(entity, targetType, range, true);
	}

	public CustomRangeActiveTargetGoal(Mob entity, Class<T> targetType, FloatSupplier range, boolean checkVisibility) {
	    super(entity, targetType, 10, checkVisibility, false, (target, serverLevel) -> {
            if (entity instanceof NexusEntity nexusEntity && nexusEntity.hasNexus()) {
                return entity.distanceTo(target) < nexusEntity.findDistanceToNexus() * 0.5;
            }
	        return true;
	    });
		this.range = range;
	}

	protected final Mob getEntity() {
		return  mob;
	}

    @Override
    protected double getFollowDistance() {
        return range == null ? 0 : range.getAsFloat();
    }

    @Override
    protected void findTarget() {
        targetConditions.range(getFollowDistance());
        super.findTarget();
    }
}
