package com.invasion.entity.pathfinding;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.EntityIMLiving;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.NexusAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

@Deprecated
public class IMNavigation implements Navigation {
	protected static final int XZPATH_HORIZONTAL_SEARCH = 1;
	protected static final double ENTITY_TRACKING_TOLERANCE = 0.1D;
	protected static final double MINIMUM_PROGRESS = 0.01D;

	protected final EntityIMLiving theEntity;

	protected PathSource pathSource;

	protected Path path;
	@Nullable
	protected Node activeNode;

	protected Vec3 entityCentre;

	protected Entity pathEndEntity;
	protected Vec3 pathEndEntityLastPos = Vec3.ZERO;

	protected double moveSpeed;
	protected float pathSearchLimit;

	protected boolean noSunPathfind;

	protected int totalTicks;
	protected Vec3 lastPos = Vec3.ZERO;
	private Vec3 holdingPos;
	protected boolean nodeActionFinished = true;
	private boolean canSwim;
	protected boolean waitingForNotify;
	protected boolean actionCleared = true;
	protected double lastDistance;
	protected int ticksStuck;
	private boolean maintainPosOnWait;
	private Status lastActionResult;
	private boolean haltMovement;
	private boolean autoPathToEntity;

    protected Goal currentGoal = Goal.NONE;
    protected Goal prevGoal = Goal.NONE;

	protected final Actor<?> actor;

	public IMNavigation(EntityIMLiving entity, PathSource pathSource) {
		this.theEntity = entity;
		this.pathSource = pathSource;
		actor = createActor(entity);
	}

    @Override
    public NodeEvaluator createNodeMaker() {
        return new IMLandPathNodeMaker();
    }

    @Override
    public Actor<?> getActor() {
	    return actor;
	}

	protected <T extends Entity> Actor<T> createActor(T entity) {
	    return new Actor<>(entity);
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
	    return !nodeActionFinished && !isIdle() ? ActionablePathNode.getAction(activeNode) : PathAction.NONE;
	}

	protected boolean isMaintainingPos() {
		return maintainPosOnWait;
	}

	protected void setNoMaintainPos() {
		maintainPosOnWait = false;
	}

	protected void setMaintainPosOnWait(Vec3 pos) {
		holdingPos = pos;
		maintainPosOnWait = true;
	}

    public void setSpeed(double speed) {
		moveSpeed = speed;
	}

    @Override
    public Entity getTargetEntity() {
		return pathEndEntity;
	}

    public Path getPathToXYZ(Vec3 pos, float targetRadius) {
		return createPath(theEntity, BlockPos.containing(pos), targetRadius);
	}

    public boolean startMovingTo(double x, double y, double z, double speed) {
		ticksStuck = 0;
		Path newPath = getPathToXYZ(new Vec3(x, y, z), theEntity.getNavigation().getMaxDistanceToWaypoint());
		return newPath != null && startMovingAlong(newPath, speed);
	}

    @Nullable
    public Path findPathTo(Entity targetEntity, int distance) {
		return createPath(theEntity, targetEntity.blockPosition(), theEntity.getNavigation().getMaxDistanceToWaypoint());
	}

    public boolean startMovingTo(Entity targetEntity, double speed) {
		Path newPath = findPathTo(targetEntity, (int)(2 * theEntity.distanceTo(targetEntity)));
		if (newPath == null) {
		    return false;
		}

		if (startMovingAlong(newPath, speed)) {
			pathEndEntity = targetEntity;
			return true;
		}

		pathEndEntity = null;
		return false;
	}

    @Override
    public void autoPathToEntity(Entity target) {
		autoPathToEntity = true;
		pathEndEntity = target;
	}

    public boolean startMovingAlong(net.minecraft.world.level.pathfinder.Path newPath, double speed) {
		if (newPath == null) {
			path = null;
			theEntity.onPathSet();
			return false;
		}

		moveSpeed = speed;
		lastDistance = getDistanceToActiveNode();
		ticksStuck = 0;
		resetStatus();

		entityCentre = com.invasion.util.math.PosUtils.bottomCenter(theEntity.blockPosition());

		path = newPath;
		activeNode = path.getNode(path.getNextNodeIndex());

		if (getCurrentWorkingAction() != PathAction.NONE) {
			nodeActionFinished = false;
		} else if (theEntity.getBbWidth() <= 1) {
			path.advance();
			if (!path.isDone()) {
				activeNode = path.getNode(path.getNextNodeIndex());
				if (getCurrentWorkingAction() != PathAction.NONE) {
					nodeActionFinished = false;
				}
			}
		} else {
			//UnstoppableN Custom Code
			//changed < to > this seems to have fixed some stuffs, not sure why
			while (theEntity.position().distanceTo(entityCentre.add(com.invasion.util.math.PosUtils.center(activeNode.asBlockPos()))) > theEntity.getBbWidth()) {
				path.advance();
				if (path.isDone()) {
				    //System.out.println("Finished! : "+ path.getCurrentPathIndex()+" / "+ path.points.length);
				    break;
				}

				activeNode = path.getNode(path.getNextNodeIndex());
				if (getCurrentWorkingAction() != PathAction.NONE) {
					nodeActionFinished = false;
				}
			}
		}

		if (noSunPathfind) {
			removeSunnyPath();
		}

		theEntity.onPathSet();
		return true;
	}

    public Path getCurrentPath() {
		return path;
	}

    @Override
    public boolean isWaitingForTask() {
		return waitingForNotify;
	}

    public void tick() {
	    if (theEntity.getTarget() != null) {
            transitionAIGoal(Goal.TARGET_ENTITY);
        } else if (theEntity.getNexus() != null) {
            transitionAIGoal(Goal.BREAK_NEXUS);
        } else {
            transitionAIGoal(Goal.CHILL);
        }

	    tickPathFinding();
	}

	private void tickPathFinding() {
		totalTicks++;
		if (autoPathToEntity) {
			updateAutoPathToEntity();
		}

		if (isIdle()) {
			noPathFollow();
			return;
		}

		if (isWaitingForTask()) {
			if (isMaintainingPos()) {
				theEntity.getMoveControl().setWantedPosition(holdingPos.x(), holdingPos.y(), holdingPos.z(), moveSpeed);
			}
			return;
		}

		if (nodeActionFinished) {
			double distance = getDistanceToActiveNode();
			if (lastDistance - distance > 0.01D) {
				lastDistance = distance;
				ticksStuck--;
			} else {
				ticksStuck++;
			}

			int pathIndex = path.getNextNodeIndex();
			pathFollow();
			if (isIdle()) {
				return;
			}
			if (path.getNextNodeIndex() != pathIndex) {
				lastDistance = getDistanceToActiveNode();
				ticksStuck = 0;
				activeNode = path.getNode(path.getNextNodeIndex());
				if (getCurrentWorkingAction() != PathAction.NONE) {
					nodeActionFinished = false;
				}
			}
		}

		if (nodeActionFinished) {
			if (!isPositionClearFrom(theEntity.blockPosition(), activeNode.asBlockPos(), theEntity)) {
				if (theEntity.onPathBlocked(path, this)) {
					setDoingTaskAndHoldOnPoint();
				}

			}

			if (!haltMovement) {
				if (pathEndEntity != null && pathEndEntity.getY() - theEntity.getY() <= 0 && theEntity.distanceToSqr(pathEndEntity) < 4.5D) {
					theEntity.getMoveControl().setWantedPosition(
					        pathEndEntity.getX(),
					        pathEndEntity.getY(),
					        pathEndEntity.getZ(), moveSpeed);
				} else {
					theEntity.getMoveControl().setWantedPosition(
					        activeNode.x + entityCentre.x,
					        activeNode.y + entityCentre.y,
					        activeNode.z + entityCentre.z, moveSpeed);
				}
			} else {
				haltMovement = false;
			}

		} else if (handlePathAction(getCurrentWorkingAction())) {
			stop();
		}
	}

	@Override
    public void notifyTask(Status result) {
		waitingForNotify = false;
		lastActionResult = result;
	}

    @Override
    public Status getLastActionResult() {
		return lastActionResult;
	}

    @Override
    public boolean isIdle() {
		return path == null || path.isDone();
	}

    @Override
    public int getStuckTime() {
		return ticksStuck;
	}

    @Override
    public float getLastPathDistanceToTarget() {
		if (isIdle()) {
			if (path != null && path.getTarget() != null) {
				return Mth.sqrt((float) theEntity.blockPosition().distSqr(path.getTarget()));
			}
			return 0;
		}

		if (path.getPreviousNode() == null || path.getTarget() == null) {
			return 0;
		}

		return path.getPreviousNode().distanceTo(path.getTarget());
	}

    public void stop() {
		path = null;
		autoPathToEntity = false;
		resetStatus();
	}

    @Override
    public void haltForTick() {
		haltMovement = true;
	}

	protected Path createPath(EntityIMLiving entity, Entity target, float targetRadius) {
		return createPath(entity, target.blockPosition(), targetRadius);
	}

	protected Path createPath(EntityIMLiving entity, BlockPos pos, float targetRadius) {
		actor.setCurrentTargetPos(pos);
		CollisionGetter terrainCache = getChunkCache(entity.blockPosition(), pos, 16);
		NexusAccess nexus = entity.getNexus();
		if (nexus != null) {
			terrainCache = nexus.getAttackerAI().wrapEntityData(terrainCache);
		}
		float maxSearchRange = 12 + Mth.sqrt((float) entity.blockPosition().distSqr(pos));
		return pathSource.createPath(entity, pos, targetRadius, maxSearchRange, terrainCache);
	}

	protected void pathFollow() {
		Vec3 pos = getPos();
		int maxNextLegIndex = path.getNextNodeIndex() - 1;

		Node nextPoint = path.getNextNode();
		if (nextPoint.y == (int) pos.y && maxNextLegIndex < path.getNodeCount() - 1) {
			maxNextLegIndex++;

			boolean canConsolidate = true;
			int prevIndex = maxNextLegIndex - 2;
			if (prevIndex >= 0 && ActionablePathNode.getAction(path.getNode(prevIndex)) != PathAction.NONE) {
				canConsolidate = false;
			}
			if (canConsolidate && actor.canStandAt(theEntity.level(), theEntity.blockPosition())) {
				while (maxNextLegIndex < path.getNodeCount() - 1
				        && path.getNode(maxNextLegIndex).y == (int) pos.y
				        && ActionablePathNode.getAction(path.getNode(maxNextLegIndex)) == PathAction.NONE) {
					maxNextLegIndex++;
				}
			}

		}

		float reach = Mth.square(theEntity.getBbWidth() * 0.5F);
		for (int j = path.getNextNodeIndex(); j <= maxNextLegIndex; j++) {
			if (pos.distanceToSqr(path.getEntityPosAtNode(theEntity, j)) < reach) {
				path.setNextNodeIndex(j + 1);
			}
		}

		Vec3i size = new Vec3i(
		        (int) Math.ceil(this.theEntity.getBbWidth()),
		        (int) this.theEntity.getBbHeight() + 1,
		        (int) Math.ceil(this.theEntity.getBbWidth())
        );

		while (maxNextLegIndex > path.getNextNodeIndex() && !isDirectPathBetweenPoints(pos, path.getEntityPosAtNode(theEntity, maxNextLegIndex), size)) {
		    maxNextLegIndex--;
		}

		for (int i = path.getNextNodeIndex() + 1; i < maxNextLegIndex; i++) {
			if (ActionablePathNode.getAction(path.getNode(i)) != PathAction.NONE) {
			    maxNextLegIndex = i;
				break;
			}
		}

		if (path.getNextNodeIndex() < maxNextLegIndex) {
			path.setNextNodeIndex(maxNextLegIndex);
		}
	}

	protected void noPathFollow() {
	}

	protected void updateAutoPathToEntity() {
		if (pathEndEntity != null && (isIdle() || (pathEndEntity.position().distanceTo(pathEndEntityLastPos) / (6 + theEntity.position().distanceTo(pathEndEntityLastPos)) > 0.1D))) {
			Path newPath = this.findPathTo(pathEndEntity, theEntity.getSenseRange());
			if (newPath != null && startMovingAlong(newPath, moveSpeed)) {
				pathEndEntityLastPos = pathEndEntity.position();
			}
		}
	}

	protected double getDistanceToActiveNode() {
		return activeNode == null ? 0 : activeNode.asVec3().subtract(theEntity.position()).length();
	}

	protected boolean handlePathAction(PathAction action) {
		this.nodeActionFinished = true;
		return true;
	}

	protected boolean setDoingTaskAndHold() {
		waitingForNotify = true;
		actionCleared = false;
		setMaintainPosOnWait(theEntity.position());
		theEntity.setIsHoldingIntoLadder(true);
		return true;
	}

	protected boolean setDoingTaskAndHoldOnPoint() {
		waitingForNotify = true;
		actionCleared = false;
		setMaintainPosOnWait(activeNode.asVec3());
		theEntity.setIsHoldingIntoLadder(true);
		return true;
	}

	protected void resetStatus() {
		setNoMaintainPos();
		theEntity.setIsHoldingIntoLadder(false);
		nodeActionFinished = true;
		actionCleared = true;
		waitingForNotify = false;
	}

    public Vec3 getPos() {
		return new Vec3(theEntity.getX(), getPathableYPos(), theEntity.getZ());
	}

	protected EntityIMLiving getEntity() {
		return this.theEntity;
	}

    private int getPathableYPos() {
		if (!theEntity.isInWater() || !canSwim) {
			return theEntity.blockPosition().getY();
		}

		final BlockPos.MutableBlockPos mutable = theEntity.blockPosition().mutable();
		final int initialY = mutable.getY();
		int y = initialY - 1;
		BlockState state;
		int steps = 0;

		do {
		    state = theEntity.level().getBlockState(mutable.setY(++y));
			if (++steps > 16) {
				return initialY;
			}
		} while (state.liquid() && y < theEntity.level().getMaxY());

		return y;
	}

	@Nullable
	protected Vec3 findValidPointNear(double x, double z, int min, int max, int verticalRange) {
		double xOffset = x - theEntity.getX();
		double zOffset = z - theEntity.getZ();
		double h = Math.sqrt(xOffset * xOffset + zOffset * zOffset);

		if (h < 0.5D) {
			return null;
		}

		double distance = min + theEntity.level().getRandom().nextInt(max - min);
		int xi = Mth.floor(xOffset * (distance / h) + theEntity.getX());
		int zi = Mth.floor(zOffset * (distance / h) + theEntity.getZ());
		int y = Mth.floor(theEntity.getY());

		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int vertical = 0; vertical < verticalRange; vertical = vertical > 0 ? vertical * -1 : vertical * -1 + 1) {
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					if (actor.canStandAtAndIsValid(theEntity.level(), mutable.set(xi + i, y + vertical, zi + j))) {
						return new Vec3(xi + i, y + vertical, zi + j);
					}
				}
			}
		}

		return null;
	}

	protected void removeSunnyPath() {
		if (theEntity.level().canSeeSky(theEntity.blockPosition())) {
			return;
		}

		for (int i = 0; i < path.getNodeCount(); i++) {
			Node pathpoint = path.getNode(i);

			if (theEntity.level().canSeeSky(pathpoint.asBlockPos())) {
				path.truncateNodes(i - 1);
				return;
			}
		}
	}

	protected boolean isDirectPathBetweenPoints(Vec3 pos1, Vec3 pos2, Vec3i size) {
	    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(pos1.x, pos1.y, pos1.z);

		Vec3 delta = pos2.subtract(pos1);

		if (delta.horizontalDistance() < 1.0E-008D) {
			return false;
		}

		delta = delta.multiply(1, 0, 1).normalize();

		if (!isSafeToStandAt(mutable, size.offset(2, 0, 2), pos1, delta)) {
			return false;
		}

		double xIncrement = 1D / delta.x;
		double zIncrement = 1D / delta.z;
		double xOffset = mutable.getX() * (1 - pos1.x);
		double zOffset = mutable.getZ() * (1 - pos1.z);

		if (delta.x >= 0) {
			xOffset++;
		}

		if (delta.z >= 0) {
			zOffset++;
		}

		xOffset *= xIncrement;
		zOffset *= zIncrement;
		byte xDirection = (byte)Math.signum(delta.x);
		byte zDirection = (byte)Math.signum(delta.z);
		int x2 = Mth.floor(pos2.x);
		int z2 = Mth.floor(pos2.z);

		for (; (x2 - mutable.getX()) * xDirection > 0 || (z2 - mutable.getZ()) * zDirection > 0;) {
			if (xOffset < zOffset) {
				xOffset += xIncrement;
				mutable.move(xDirection, 0, 0);
			} else {
				zOffset += zIncrement;
				mutable.move(0, 0, zDirection);
			}

			if (!isSafeToStandAt(mutable, size, pos1, delta)) {
				return false;
			}
		}

		return true;
	}

    protected boolean isSafeToStandAt(BlockPos.MutableBlockPos pos, Vec3i size, Vec3 entityPosition, Vec3 delta) {
	    pos.move(-size.getX() / 2, 0, -size.getZ() / 2);

		if (!isPositionClear(pos, size, entityPosition, delta.x, delta.z)) {
			return false;
		}

		for (BlockPos p : BlockPos.betweenClosed(pos, pos.offset(size.getX(), 0, size.getZ()))) {
		    Vec3 centerP = com.invasion.util.math.PosUtils.bottomCenter(p).subtract(entityPosition);

            if (centerP.x * delta.x + centerP.z * delta.z >= 0) {
                BlockState block = theEntity.level().getBlockState(p.below());
                if (block.isAir()
                    || block.liquid() && ((block.getFluidState().is(FluidTags.WATER) && !theEntity.isUnderWater()) || block.getFluidState().is(FluidTags.LAVA))
                    || !block.isRedstoneConductor(theEntity.level(), p)) {
                    return false;
                }
            }
		}

		return true;
	}

    protected boolean isPositionClear(BlockPos pos, Vec3i size, Vec3 entityPostion, double vecX, double vecZ) {
	    for (BlockPos p : BlockPos.betweenClosed(pos, pos.offset(size))) {
	        double d = p.getX() + 0.5D - entityPostion.x;
            double d1 = p.getZ() + 0.5D - entityPostion.z;

            if (d * vecX + d1 * vecZ >= 0) {
                BlockState block = theEntity.level().getBlockState(p);

                if (!block.isAir() && block.blocksMotion()) {
                    return false;
                }
            }
	    }
		return true;
	}

	protected boolean isPositionClearFrom(BlockPos from, BlockPos to, EntityIMLiving entity) {
		if (to.getY() > from.getY()) {
			BlockState block = theEntity.level().getBlockState(from.offset(0, Mth.ceil(entity.getBbHeight()), 0));
			if (!block.isAir() && block.blocksMotion()) {
				return false;
			}
		}

		return isPositionClear(to, entity);
	}

	protected boolean isPositionClear(BlockPos pos, EntityIMLiving entity) {
	    return BlockPos.betweenClosedStream(entity.getDimensions(entity.getPose()).makeBoundingBox(com.invasion.util.math.PosUtils.bottomCenter(pos))).allMatch(p -> {
	        BlockState block = theEntity.level().getBlockState(p);
	        return block.isAir() || !block.blocksMotion();
	    });
	}

	protected PathNavigationRegion getChunkCache(BlockPos p1, BlockPos p2, float axisExpand) {
        BoundingBox box = BoundingBox.fromCorners(p1, p2).inflatedBy((int) axisExpand);
        return new PathNavigationRegion(theEntity.level(),
                new BlockPos(box.minX(), box.minY(), box.minZ()),
                new BlockPos(box.maxX(), box.maxY(), box.maxZ())
        );
	}

    @Override
    public void setCanDestroyBlocks(boolean flag) {
        getActor().setCanDestroyBlocks(flag);
    }

    @Override
    public void setCanDigDown(boolean flag) {
        getActor().setCanDestroyBlocks(flag);
    }

    @Override
    public void setCanClimbLadders(boolean flag) {
        getActor().setCanClimb(flag);

    }
}
