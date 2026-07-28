package com.invasion.entity.ai.goal;

import com.invasion.entity.HasAiGoals;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.Navigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class NoNexusPathGoal extends PredicatedGoal {
	private static final float PATH_DISTANCE_TRIGGER = 4;

	public <E extends Mob & NexusEntity> NoNexusPathGoal(E entity, Goal goal) {
		super(goal, () -> isLostPathToNexus(entity));
	}

	public static boolean isLostPathToNexus(NexusEntity entity) {
	    Navigation navigation = entity.getNavigatorNew();
	    return navigation != null
            && navigation.hasGoal(HasAiGoals.Goal.BREAK_NEXUS)
            && navigation.getLastPathDistanceToTarget() > PATH_DISTANCE_TRIGGER;
	}
}
