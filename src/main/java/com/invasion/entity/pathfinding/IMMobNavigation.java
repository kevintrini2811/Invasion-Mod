package com.invasion.entity.pathfinding;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.entity.Stunnable;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.test.PathingDebugger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class IMMobNavigation extends GroundPathNavigation implements Navigation {
    static final int MAX_WAIT_TIME = 2000;
    private Goal currentGoal = Goal.NONE;
    private Goal prevGoal = Goal.NONE;

    private int waitingForNotify;
    private int activeTaskNodeIndex = -1;
    private int completedTaskNodeIndex = -1;
    private Status lastActionResult = Status.SUCCESS;
    private boolean continuingEngineerBridge;
    @Nullable
    private BlockPos lastCompletedBridgeTarget;
    private int lastLoggedBridgePath;
    private int lastLoggedBridgeNode = -1;
    private PathAction lastLoggedBridgeAction = PathAction.NONE;

    private int haltingTicks;
    private int stuckTime;
    private boolean climbingLadder;
    private boolean gravityBeforeLadder;
    private int ladderColumnX;
    private int ladderColumnZ;
    private int ladderExitY;
    private boolean holdingAtLadderTop;

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
        if (isDone()) {
            return PathAction.NONE;
        }

        PathAction action = path.getNextNode() instanceof ActionablePathNode node
                ? node.getAction()
                : PathAction.NONE;
        if (action == PathAction.NONE
                && continuingEngineerBridge
                && mob instanceof PigmanEngineerEntity
                && requiresBridgeAt(path.getNextNodePos())) {
            return PathAction.BRIDGE;
        }
        return action;
	}

    private boolean requiresBridgeAt(BlockPos feetPos) {
        var feetState = mob.level().getBlockState(feetPos);
        if (!feetState.getFluidState().isEmpty()) {
            return true;
        }

        var belowState = mob.level().getBlockState(feetPos.below());
        return belowState.isAir() || !belowState.getFluidState().isEmpty();
    }

    @Override
    public boolean isWaitingForTask() {
		return waitingForNotify > 0;
	}

	@Override
    public void notifyTask(Status result) {
	    waitingForNotify = 0;
        lastActionResult = result;
        if (continuingEngineerBridge && mob instanceof PigmanEngineerEntity) {
            InvasionMod.LOGGER.warn(
                    "[EngineerBridge] build finished: entity={}, path={}, node={}, result={}, pos={}",
                    mob.getId(),
                    path == null ? 0 : System.identityHashCode(path),
                    activeTaskNodeIndex,
                    result,
                    mob.blockPosition()
            );
        }
        completedTaskNodeIndex = activeTaskNodeIndex;
        activeTaskNodeIndex = -1;
        // Time spent deliberately standing still for a terrain job must not
        // trigger the goal's stuck-path recovery immediately afterwards.
        stuckTime = 0;
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
        if (climbingLadder) {
            climbLadder();
            return;
        }
        if (continuingEngineerBridge && mob instanceof PigmanEngineerEntity) {
            // Bridge construction owns movement until solid ground is
            // reached. Combat targets otherwise replace the bridge path and
            // make the engineer walk off its last plank.
            mob.setTarget(null);
            if (isDone() && lastCompletedBridgeTarget != null) {
                holdAtLastBridgeTarget();
            }
        }
        tickObjectives();
        tickFollowing();

        stuckTime++;

        if (mob instanceof Stunnable l && l.isStunned()) {
            return;
        }

        if (haltingTicks > 0 || waitingForNotify > 0) {
            haltingTicks = Math.max(0, haltingTicks - 1);
            waitingForNotify = Math.max(0, waitingForNotify - 1);
            mob.setXxa(0);
            mob.setZza(0);
            mob.setSpeed(0);
            mob.setDeltaMovement(
                    mob.getDeltaMovement().x * 0.25D,
                    mob.onClimbable() ? Math.max(0, mob.getDeltaMovement().y) : mob.getDeltaMovement().y,
                    mob.getDeltaMovement().z * 0.25D);
            mob.setShiftKeyDown(mob.onClimbable());
            mob.fallDistance = 0;
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
        if (climbingLadder) {
            climbLadder();
            return;
        }

        BlockPos ladderPos = getTargetedLadder();
        if (ladderPos != null) {
            beginLadderClimb(ladderPos);
            climbLadder();
            return;
        }

	    mob.setShiftKeyDown(false);
	    if (mob instanceof NexusEntity e) {
            e.setIsHoldingIntoLadder(false);
        }

        PathAction currentAction = getCurrentWorkingAction();
        int nodeIndex = getPath().getNextNodeIndex();
        logEngineerBridgeTransition(currentAction, nodeIndex);

        if (currentAction != PathAction.NONE
                && completedTaskNodeIndex == nodeIndex
                && lastActionResult != Status.SUCCESS) {
            stop();
            return;
        }

        if (currentAction.getType() == PathAction.Type.BRIDGE
                && completedTaskNodeIndex == nodeIndex) {
            BlockPos moveTarget = getCompletedActionMoveTarget(
                    currentAction, getPath().getNextNodePos());
            if (!captureCompletedBridgeTarget(moveTarget)) {
                moveToCompletedBridgeTarget(moveTarget);
                return;
            }

            // End this bridge step without letting vanilla immediately start
            // moving towards the next, still unsupported path node.
            stopHorizontalMovement();
            lastCompletedBridgeTarget = moveTarget;
            getPath().setNextNodeIndex(nodeIndex + 1);
            return;
        }

        // Legacy NavigatorEngy resolves a build action before allowing the
        // path to advance past that node. Modern vanilla navigation otherwise
        // considers a nearby actionable node reached and silently skips it.
        if (currentAction != PathAction.NONE
                && currentAction.getType() != PathAction.Type.CLIMB
                && completedTaskNodeIndex != nodeIndex) {
            handlePathAction(currentAction);
            return;
        }

        // Finishing a terrain job does not necessarily mean that the engineer
        // has reached its actionable path node.
        if (currentAction != PathAction.NONE
                && completedTaskNodeIndex == nodeIndex
                && !hasReachedCompletedActionNode(
                        currentAction,
                        getCompletedActionMoveTarget(currentAction, getPath().getNextNodePos()))) {
            moveToCompletedActionNode(
                    getCompletedActionMoveTarget(currentAction, getPath().getNextNodePos()));
            return;
        }

	    super.followThePath();

        currentAction = getCurrentWorkingAction();
	    if (currentAction.getType() == PathAction.Type.CLIMB) {
            handlePathAction(currentAction);
	    }
	}

    @Nullable
    private BlockPos getTargetedLadder() {
        if (getPath() == null || getPath().isDone()) {
            return null;
        }
        BlockPos nodePos = getPath().getNextNodePos();
        if (mob.level().getBlockState(nodePos).is(Blocks.LADDER)) {
            return nodePos;
        }
        if (mob.level().getBlockState(nodePos.below()).is(Blocks.LADDER)) {
            return nodePos.below();
        }
        return null;
    }

    private void beginLadderClimb(BlockPos ladderPos) {
        climbingLadder = true;
        gravityBeforeLadder = mob.isNoGravity();
        ladderColumnX = ladderPos.getX();
        ladderColumnZ = ladderPos.getZ();

        BlockPos.MutableBlockPos scan = ladderPos.mutable();
        int topY = ladderPos.getY();
        for (int offset = 1; offset <= 32; offset++) {
            scan.set(ladderPos).move(Direction.UP, offset);
            if (!mob.level().getBlockState(scan).is(Blocks.LADDER)) {
                break;
            }
            topY = scan.getY();
        }
        ladderExitY = topY + 1;
        mob.setNoGravity(true);
    }

    private void climbLadder() {
        double targetX = ladderColumnX + 0.5D;
        double targetZ = ladderColumnZ + 0.5D;

        if (holdingAtLadderTop) {
            if (!shouldHoldAtLadderTop()) {
                finishLadderClimb();
                return;
            }
            holdAtLadderTop(targetX, targetZ);
            return;
        }

        // Own all movement while climbing. Centering the mob in the ladder
        // cell and disabling gravity prevents sideways knockback and the
        // between-tick slide that made the previous implementation fall. The
        // column stays locked until its physical top, regardless of which
        // path node vanilla selects in the meantime.
        mob.setPos(targetX, mob.getY(), targetZ);
        mob.setDeltaMovement(0, 0.2D, 0);
        mob.setXxa(0);
        mob.setZza(0);
        mob.fallDistance = 0;
        mob.setShiftKeyDown(false);
        mob.setJumping(false);

        advanceReachedLadderNodes();

        if (mob.getY() >= ladderExitY - 0.05D) {
            mob.setPos(targetX, ladderExitY - 0.05D, targetZ);
            mob.setDeltaMovement(0, 0, 0);
            advanceReachedLadderNodes();
            if (shouldHoldAtLadderTop()) {
                holdingAtLadderTop = true;
                holdAtLadderTop(targetX, targetZ);
            } else {
                finishLadderClimb();
            }
        }
    }

    private boolean shouldHoldAtLadderTop() {
        return getAIGoal() == Goal.BREAK_NEXUS
                && mob instanceof NexusEntity nexusMob
                && nexusMob.hasNexus()
                && nexusMob.findDistanceToNexus() <= 4;
    }

    private void holdAtLadderTop(double targetX, double targetZ) {
        mob.setPos(targetX, ladderExitY - 0.05D, targetZ);
        mob.setDeltaMovement(0, 0, 0);
        mob.setXxa(0);
        mob.setZza(0);
        mob.fallDistance = 0;
        mob.setShiftKeyDown(true);
        mob.setJumping(false);
    }

    private void advanceReachedLadderNodes() {
        if (getPath() == null) {
            return;
        }
        while (!getPath().isDone()) {
            BlockPos nodePos = getPath().getNextNodePos();
            boolean belongsToColumn =
                    nodePos.getX() == ladderColumnX
                            && nodePos.getZ() == ladderColumnZ;
            boolean isLadderNode =
                    belongsToColumn
                            && (mob.level().getBlockState(nodePos)
                                            .is(Blocks.LADDER)
                                    || mob.level().getBlockState(nodePos.below())
                                            .is(Blocks.LADDER));
            if (!isLadderNode || nodePos.getY() > mob.getY() + 0.1D) {
                return;
            }
            getPath().setNextNodeIndex(getPath().getNextNodeIndex() + 1);
        }
    }

    private void finishLadderClimb() {
        if (!climbingLadder) {
            return;
        }
        climbingLadder = false;
        holdingAtLadderTop = false;
        mob.setNoGravity(gravityBeforeLadder);
        mob.fallDistance = 0;
    }

    @Override
    public void stop() {
        if (!holdingAtLadderTop || !shouldHoldAtLadderTop()) {
            finishLadderClimb();
        }
        super.stop();
    }

    private void logEngineerBridgeTransition(PathAction action, int nodeIndex) {
        if (!(mob instanceof PigmanEngineerEntity)
                || (!continuingEngineerBridge && action.getType() != PathAction.Type.BRIDGE)) {
            return;
        }

        int pathId = System.identityHashCode(getPath());
        if (pathId != lastLoggedBridgePath
                || nodeIndex != lastLoggedBridgeNode
                || action != lastLoggedBridgeAction) {
            lastLoggedBridgePath = pathId;
            lastLoggedBridgeNode = nodeIndex;
            lastLoggedBridgeAction = action;
            BlockPos nodePos = getPath().getNextNodePos();
            InvasionMod.LOGGER.warn(
                    "[EngineerBridge] path state: entity={}, path={}, node={}/{}, action={}, nodePos={}, mobPos={}, requiresBridge={}",
                    mob.getId(),
                    pathId,
                    nodeIndex,
                    getPath().getNodeCount(),
                    action,
                    nodePos,
                    mob.blockPosition(),
                    requiresBridgeAt(nodePos)
            );
        }
    }

    private boolean hasReachedCompletedActionNode(PathAction action, BlockPos nodePos) {
        double targetX = nodePos.getX() + 0.5D;
        double targetZ = nodePos.getZ() + 0.5D;
        double horizontalDistanceSqr = Mth.square(mob.getX() - targetX)
                + Mth.square(mob.getZ() - targetZ);

        if (action.getType() == PathAction.Type.BRIDGE) {
            return horizontalDistanceSqr < 0.09D
                    && Math.abs(mob.getY() - nodePos.getY()) < 0.6D;
        }

        return horizontalDistanceSqr < 0.36D
                && Math.abs(mob.getY() - nodePos.getY()) < 1.0D;
    }

    private BlockPos getCompletedActionMoveTarget(PathAction action, BlockPos nodePos) {
        // In water or lava the path node itself is replaced by the plank, so
        // the engineer must move onto the block above it. Across air, the
        // plank sits below the path node and the original target is correct.
        if (action.getType() == PathAction.Type.BRIDGE
                && mob.level().getBlockState(nodePos)
                        .isCollisionShapeFullBlock(mob.level(), nodePos)) {
            return nodePos.above();
        }
        return nodePos;
    }

    private boolean captureCompletedBridgeTarget(BlockPos target) {
        double targetX = target.getX() + 0.5D;
        double targetZ = target.getZ() + 0.5D;
        double horizontalDistanceSqr = Mth.square(mob.getX() - targetX)
                + Mth.square(mob.getZ() - targetZ);

        // Capture the final part of the step while the entity is still safely
        // supported by the old or diagonal corner planks. This prevents
        // movement inertia from carrying it over the far edge.
        if (horizontalDistanceSqr <= 0.49D
                && Math.abs(mob.getY() - target.getY()) < 1.25D) {
            mob.setPos(targetX, target.getY(), targetZ);
            stopHorizontalMovement();
            mob.fallDistance = 0;
            return true;
        }
        return false;
    }

    private void moveToCompletedBridgeTarget(BlockPos target) {
        Vec3 movement = mob.getDeltaMovement();
        double horizontalSpeedSqr = movement.x * movement.x + movement.z * movement.z;
        if (horizontalSpeedSqr > 0.0144D) {
            double scale = 0.12D / Math.sqrt(horizontalSpeedSqr);
            mob.setDeltaMovement(
                    movement.x * scale,
                    Math.max(movement.y, -0.05D),
                    movement.z * scale
            );
        }
        mob.fallDistance = 0;
        mob.getMoveControl().setWantedPosition(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                0.55D
        );
    }

    private void moveToCompletedActionNode(BlockPos nodePos) {
        mob.fallDistance = 0;
        mob.getMoveControl().setWantedPosition(
                nodePos.getX() + 0.5D,
                nodePos.getY(),
                nodePos.getZ() + 0.5D,
                speedModifier
        );
    }

    private void stopHorizontalMovement() {
        Vec3 movement = mob.getDeltaMovement();
        mob.setXxa(0);
        mob.setZza(0);
        mob.setSpeed(0);
        mob.setDeltaMovement(0, movement.y, 0);
        // Replace the previous MOVE_TO operation as well; zeroing velocity
        // alone does not prevent MoveControl from accelerating again later in
        // the same tick.
        mob.getMoveControl().setWantedPosition(
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                0
        );
    }

    private void holdAtLastBridgeTarget() {
        double targetX = lastCompletedBridgeTarget.getX() + 0.5D;
        double targetZ = lastCompletedBridgeTarget.getZ() + 0.5D;
        if (Mth.square(mob.getX() - targetX)
                + Mth.square(mob.getZ() - targetZ) < 1.0D) {
            mob.setPos(targetX, lastCompletedBridgeTarget.getY(), targetZ);
        }
        stopHorizontalMovement();
        mob.fallDistance = 0;
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
            int nodeIndex = getPath().getNextNodeIndex();
            if (completedTaskNodeIndex == nodeIndex) {
                return;
            }
            if (action.getType() == PathAction.Type.BRIDGE
                    && mob instanceof PigmanEngineerEntity) {
                continuingEngineerBridge = true;
            }
            InvasionMod.LOGGER.debug("Handling path action {}", action);
            if (mob instanceof NexusEntity e && e.handlePathAction(getPath().getNextNodePos(), action, this)) {
                activeTaskNodeIndex = nodeIndex;
                waitingForNotify = MAX_WAIT_TIME;
            } else {
                lastActionResult = Status.SUCCESS;
                completedTaskNodeIndex = nodeIndex;
            }
        }
	}

	@Override
    public Vec3 getTempMobPos() {
	    return super.getTempMobPos();
	}

    @Override
    public boolean moveTo(Path path, double speed) {
        if (mob instanceof PigmanEngineerEntity && isWaitingForTask()) {
            InvasionMod.LOGGER.warn(
                    "[EngineerBridge] rejected path replacement during build: entity={}, currentPath={}, requestedPath={}, node={}, pos={}",
                    mob.getId(),
                    getPath() == null ? 0 : System.identityHashCode(getPath()),
                    System.identityHashCode(path),
                    getPath() == null ? -1 : getPath().getNextNodeIndex(),
                    mob.blockPosition()
            );
            return false;
        }
        if (continuingEngineerBridge
                && mob instanceof NexusEntity nexusMob
                && nexusMob.hasNexus()
                && path.getTarget() != null
                && path.getTarget().distManhattan(nexusMob.getNexus().getOrigin()) > 2) {
            return false;
        }
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
                if (continuingEngineerBridge && mob instanceof PigmanEngineerEntity) {
                    InvasionMod.LOGGER.warn(
                            "[EngineerBridge] path replaced: entity={}, oldPath={}, newPath={}, nodes={}, target={}, pos={}",
                            mob.getId(),
                            previousPath == null ? 0 : System.identityHashCode(previousPath),
                            System.identityHashCode(currentPath),
                            currentPath.getNodeCount(),
                            currentPath.getTarget(),
                            mob.blockPosition()
                    );
                }
                activeTaskNodeIndex = -1;
                completedTaskNodeIndex = -1;
                PathingDebugger.sendPathToClients(mob, currentPath, 0.5F);
            }
        }
    }

    @Override
    public void recomputePath() {
        if (mob instanceof PigmanEngineerEntity && isWaitingForTask()) {
            InvasionMod.LOGGER.warn(
                    "[EngineerBridge] rejected automatic recomputation during build: entity={}, path={}, node={}, pos={}",
                    mob.getId(),
                    path == null ? 0 : System.identityHashCode(path),
                    path == null ? -1 : path.getNextNodeIndex(),
                    mob.blockPosition()
            );
            return;
        }
        super.recomputePath();
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
