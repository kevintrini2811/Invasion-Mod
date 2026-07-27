package com.invasion.entity.ai.goal.target;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

public class RetaliateGoal extends HurtByTargetGoal {
	public RetaliateGoal(PathfinderMob entity) {
		super(entity);
		setAlertOthers();
	}
}