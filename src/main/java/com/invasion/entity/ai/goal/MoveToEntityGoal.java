package com.invasion.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.pathfinding.Navigation;

public class MoveToEntityGoal<T extends LivingEntity> extends Goal {
	protected final PathfinderMob mob;
	protected final Navigation navigation;
	private final Class<? extends T> targetClass;

	private T target;
	private boolean running;
	private Vec3 lastTargetPos;
	private int cooldown;
	private int pathFailedCount;

	@SuppressWarnings("unchecked")
    public <E extends PathfinderMob & NexusEntity> MoveToEntityGoal(E entity) {
		this(entity, (Class<T>)LivingEntity.class);
	}

	public <E extends PathfinderMob & NexusEntity> MoveToEntityGoal(E entity, Class<? extends T> target) {
		this.targetClass = target;
		this.mob = entity;
		navigation = entity.getNavigatorNew();
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

	@Override
    @SuppressWarnings("unchecked")
    public boolean canUse() {
		if (--cooldown <= 0) {
		    LivingEntity target = mob.getTarget();
			if (target != null && (targetClass.isAssignableFrom(mob.getTarget().getClass()))) {
				this.target = (T)target;
				return true;
			}
		}
		return false;
	}

	@Override
    public boolean canContinueToUse() {
	    LivingEntity target = mob.getTarget();
		return target != null && target == this.target;
	}

	@Override
    public void start() {
		running = true;
		setPath();
	}

	@Override
    public void stop() {
		running = false;
	}

	@Override
    public void tick() {
		if (--cooldown <= 0 && !navigation.isWaitingForTask() && running && target.distanceToSqr(lastTargetPos) > 1.8) {
			setPath();
		}
		if (pathFailedCount > 3) {
			mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1);
		}
	}

	@Deprecated
	protected void setTargetMoves(boolean flag) {
		this.running = flag;
	}

	protected T getTarget() {
		return target;
	}

	protected void setPath() {
		if (mob.getNavigation().moveTo(target, 1)) {
			if (navigation.getLastPathDistanceToTarget() > 3) {
				cooldown = 30 + mob.level().random.nextInt(10);
				if (mob.getNavigation().getPath().getNodeCount() > 2) {
					pathFailedCount = 0;
				} else {
					pathFailedCount++;
				}
			} else {
				cooldown = 10 + mob.level().random.nextInt(10);
				pathFailedCount = 0;
			}
		} else {
			pathFailedCount++;
			cooldown = 40 * pathFailedCount + mob.level().random.nextInt(10);
		}

		lastTargetPos = target.position();
	}
}