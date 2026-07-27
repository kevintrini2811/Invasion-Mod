package com.invasion.entity.pathfinding;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.Stunnable;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.test.PathingDebugger;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class IMMobNavigation extends GroundPathNavigation implements Navigation {
    static final int MAX_WAIT_TIME = 2000;
    private Goal currentGoal = Goal.NONE;
    private Goal prevGoal = Goal.NONE;

    private int waitingForNotify;
    private Status lastActionResult = Status.SUCCESS;

    private int haltingTicks;
    private int stuckTime;

    @Nullable
    private Entity followingEntity;
    private Vec3 lastFollowingEntityPos = Vec3.ZERO;

    @Deprecated
    private final Actor<?> actor;

    public IMMobNavigation(Mob entity) {
        this(entity, null);
    }

	public IMMobNavigation(Mob entity, @SuppressWarnings("deprecation") @Nullable Actor<?> actor) {
	    super(entity, entity.level());
	    this.actor = actor;
	}

	public static void haltNavigation(PathfinderMob entity, int ticks) {
	    if (entity.getNavigation() instanceof IMMobNavigation navigation) {
	        navigation.haltForTick();
	    }
	}

    @Override
    protected PathFinder createPathFinder(int range) {
        this.nodeEvaluator = createNodeMaker();
        return new DynamicPathNodeNavigator(nodeEvaluator, range);
    }

    @Override
    public NodeEvaluator createNodeMaker() {
        var nodeMaker = new IMLandPathNodeMaker();
        nodeMaker.setCanPassDoors(true);
        nodeMaker.setCanOpenDoors(true);
        nodeMaker.setCanFloat(true);
        nodeMaker.setCanClimbLadders(true);
        return nodeMaker;
    }

    @Override
    public Entity getTargetEntity() {
        return followingEntity;
    }

    @Deprecated
    @Override
    public Actor<?> getActor() {
        return actor;
    }

    @Override
    public Goal getAIGoal() {
        return currentGoal;
    }

    @Override
    public Goal getPrevAIGoal() {
        return prevGoal;
    }

    @Override
    public Goal transitionAIGoal(Goal newGoal) {
        prevGoal = currentGoal;
        currentGoal = newGoal;
        return newGoal;
    }

    @Override
    public PathAction getCurrentWorkingAction() {
	    return isDone() || !(path.getNextNode() instanceof ActionablePathNode node) ? PathAction.NONE : node.getAction();
	}

    @Override
    public boolean isWaitingForTask() {
		return waitingForNotify > 0;
	}

	@Override
    public void notifyTask(Status result) {
	    waitingForNotify = 0;
        lastActionResult = result;
	}

    @Override
    public Status getLastActionResult() {
        return lastActionResult;
    }

    @Override
    public float getLastPathDistanceToTarget() {
        if (isDone()) {
            if (path != null && path.getTarget() != null) {
                return Mth.sqrt((float) mob.blockPosition().distSqr(path.getTarget()));
            }
            return 0;
        }

        if (path.getPreviousNode() == null) {
            return 0;
        }

        return path.getPreviousNode().distanceTo(path.getTarget());
    }

    @Override
    public void tick() {
        tickObjectives();
        tickFollowing();

        stuckTime++;

        if (mob instanceof Stunnable l && l.isStunned()) {
            return;
        }

        if (haltingTicks > 0 || waitingForNotify > 0) {
            haltingTicks = Math.max(0, haltingTicks - 1);
            waitingForNotify = Math.max(0, (waitingForNotify / 2) - 1);
        } else {
            if (mob instanceof NexusEntity e
                    && getCurrentWorkingAction() == PathAction.NONE) {
                if (!isDone()
                        && !path.isDone()
                        && path.getNextNodeIndex() < path.getNodeCount() - 1
                        && ActionablePathNode.getAction(path.getNode(path.getNextNodeIndex() + 1)) == PathAction.NONE
                ) {
                    Vec3 currentPos = path.getNextNode().asVec3();
                    Vec3 nextPos = path.getEntityPosAtNode(mob, path.getNextNodeIndex() + 1);
                    if (!isClearForMovementBetween(mob, currentPos, nextPos, false)) {
                        if (e.onPathBlocked(path, this)) {
                            waitingForNotify = MAX_WAIT_TIME;
                        }
                    }
                }
            }

            super.tick();
        }
    }

    // TODO: Shouldn't this be a goal instead?
    protected void tickObjectives() {
        if (mob.getTarget() != null) {
            transitionAIGoal(Goal.TARGET_ENTITY);
        } else if (mob instanceof IHasNexus i && i.hasNexus()) {
            transitionAIGoal(Goal.BREAK_NEXUS);
        } else {
            transitionAIGoal(Goal.CHILL);
        }
    }

    // TODO: Shouldn't this be a goal instead?
    protected void tickFollowing() {
        if (followingEntity != null) {
            if (!followingEntity.isAlive()) {
                followingEntity = null;
            } else {
                if (isDone() || (followingEntity.position().distanceTo(lastFollowingEntityPos) / (6 + mob.position().distanceTo(lastFollowingEntityPos)) > 0.1D)) {
                    Path newPath = createPath(followingEntity, (int)mob.distanceTo(followingEntity) + 1);
                    if (newPath != null && moveTo(newPath, 1)) {
                        lastFollowingEntityPos = followingEntity.position();
                    }
                }
            }
        }
    }

	@Override
    protected void followThePath() {
	    super.followThePath();
	    mob.setShiftKeyDown(false);
	    if (mob instanceof NexusEntity e) {
            e.setIsHoldingIntoLadder(false);
        }
	    PathAction currentAction = getCurrentWorkingAction();
	    if (currentAction != PathAction.NONE) {
            handlePathAction(currentAction);
	    }
	}

	protected void handlePathAction(PathAction action) {
        if (action.getType() == PathAction.Type.CLIMB) {
            Vec3 targetPosition = com.invasion.util.math.PosUtils.center(mob.blockPosition().relative(action.getOrientation()));
            mob.getMoveControl().setWantedPosition(targetPosition.x, targetPosition.y, targetPosition.z, 1);
            if (action.getOrientation() == Direction.UP) {
                mob.getJumpControl().jump();
            } else {
                if (mob instanceof NexusEntity e) {
                    e.setIsHoldingIntoLadder(true);
                }
                mob.fallDistance = 0;
                mob.setJumping(false);
            }
        } else {
            InvasionMod.LOGGER.info("Handling path action {}", action);
            if (mob instanceof NexusEntity e && e.handlePathAction(getPath().getNextNodePos(), action, this)) {
                waitingForNotify = MAX_WAIT_TIME;
            }
        }
	}

	@Override
    public Vec3 getTempMobPos() {
	    return super.getTempMobPos();
	}

    @Override
    public boolean moveTo(Path path, double speed) {
        @Nullable Path previousPath = getPath();
        try {
            stuckTime = 0;
            return super.moveTo(path, speed);
        } finally {
            if (mob instanceof NexusEntity n) {
                n.onPathSet();
            }
            Path currentPath = getPath();
            if (currentPath != null && currentPath != previousPath) {
                PathingDebugger.sendPathToClients(mob, currentPath, 0.5F);
            }
        }
    }

    @Override
    public int getStuckTime() {
        return stuckTime;
    }

    @Override
    public void haltForTick() {
        haltingTicks = Math.max(haltingTicks, 1);
    }

    @Override
    public void autoPathToEntity(Entity target) {
        followingEntity = target;
    }

    @Override
    public void setCanDestroyBlocks(boolean flag) {
        ((IMLandPathNodeMaker)getNodeEvaluator()).setCanDestroyBlocks(flag);
    }

    @Override
    public void setCanDigDown(boolean flag) {
        ((IMLandPathNodeMaker)getNodeEvaluator()).setCanDigDown(flag);
    }

    @Override
    public void setCanClimbLadders(boolean flag) {
        ((IMLandPathNodeMaker)getNodeEvaluator()).setCanClimbLadders(flag);
    }

    @Override
    public boolean isIdle() {
        return isDone();
    }
}
