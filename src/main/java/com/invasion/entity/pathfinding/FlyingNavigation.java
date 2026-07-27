package com.invasion.entity.pathfinding;


import org.joml.Vector3f;

import com.invasion.block.BlockMetadata;
import com.invasion.block.DestructableType;
import com.invasion.entity.EntityIMFlying;
import com.invasion.entity.ai.MoveState;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.ai.scaffold.ScaffoldView;
import com.invasion.util.math.MathUtil;

import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Deprecated
public class FlyingNavigation extends IMNavigation {
	private static final int VISION_RESOLUTION_H = 30;
	private static final int VISION_RESOLUTION_V = 20;

	private final EntityIMFlying theEntity;
	private MoveType moveType = MoveType.MIXED;
	private boolean wantsToBeFlying;
	private float targetYaw;
	private float targetPitch;
	private float targetSpeed;
	private float visionDistance = 14;
	private int visionUpdateRate = 3;
	private int timeSinceVision = 3;
	private float[][] retina = new float[VISION_RESOLUTION_H][VISION_RESOLUTION_V];
	private float[][] headingAppeal = new float[28][18];

	private Vec3 finalTarget;
	private boolean isCircling;
	private float circlingHeight;
	private float circlingRadius;
	private float pitchBias;
	private float pitchBiasAmount;
	private int timeLookingForEntity;
	private boolean precisionTarget;
	private float closestDistToTarget;
	private int timeSinceGotCloser;

	public FlyingNavigation(EntityIMFlying entityFlying, PathSource pathSource) {
		super(entityFlying, pathSource);
		theEntity = entityFlying;
		targetYaw = entityFlying.getYRot();
		targetSpeed = entityFlying.getMaxPoweredFlightSpeed();
	}

    @Override
    protected <T extends Entity> Actor<T> createActor(T entity) {
        return new Actor<>(entity) {

            @Override
            public void getSuccessors(BlockGetter terrainMap, Node currentNode, PathBuilder pathFinder) {
                if (!theEntity.getPathFindFlying()) {
                    super.getSuccessors(terrainMap, currentNode, pathFinder);
                    return;
                }

                if (terrainMap.isOutsideBuildHeight(currentNode.asBlockPos())) {
                    return;
                }

                BlockPos.MutableBlockPos mutable = currentNode.asBlockPos().mutable();

                if (getNodeDestructability(terrainMap, mutable.move(Direction.UP)) > DestructableType.UNBREAKABLE) {
                    pathFinder.addNode(mutable.immutable(), PathAction.NONE);
                }

                if (getNodeDestructability(terrainMap, mutable.move(Direction.DOWN, 2)) > DestructableType.UNBREAKABLE) {
                    pathFinder.addNode(mutable.immutable(), PathAction.NONE);
                }

                for (Direction offset : Direction.Plane.HORIZONTAL) {
                    if (getNodeDestructability(terrainMap, mutable.set(currentNode.x, currentNode.y, currentNode.z).move(offset)) > DestructableType.UNBREAKABLE) {
                        pathFinder.addNode(mutable.immutable(), PathAction.NONE);
                    }
                }

                if (canSwimHorizontal()) {
                    for (Direction offset : Direction.Plane.HORIZONTAL) {
                        if (getNodeDestructability(terrainMap, mutable.set(currentNode.x, currentNode.y, currentNode.z).move(offset)) == DestructableType.FLUID) {
                            pathFinder.addNode(mutable.immutable(), PathAction.SWIM);
                        }
                    }
                }
            }

            @Override
            public float getPathNodePenalty(Node prevNode, Node node, BlockGetter terrainMap) {
                float multiplier = 1 + ScaffoldView.of(terrainMap).getMobDensity(node.asBlockPos()) * 3;

                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

                for (int i = -1; i > -6; i--) {
                    BlockState state = terrainMap.getBlockState(mutable.set(node.x, node.y + i, node.z));
                    if (!state.isAir()) {
                        if (!state.isPathfindable(PathComputationType.LAND)) {
                            multiplier += 1 + i * 0.2F;
                            if (!PathingUtil.shouldAvoidBlock(theEntity, mutable) || i < -2) {
                                break;
                            }
                            multiplier += 6 + i * 2;
                            break;
                        }
                    }
                }

                for (Direction offset : Direction.Plane.HORIZONTAL) {
                    for (int j = 1; j <= 2; j++) {
                        BlockState state = terrainMap.getBlockState(mutable.set(node.x, node.y, node.z).move(offset, j));
                        if (!state.isPathfindable(PathComputationType.LAND)) {
                            multiplier += 1.5F - j * 0.5F;
                            if (!PathingUtil.shouldAvoidBlock(theEntity, mutable)) {
                                break;
                            }
                            multiplier += 6 - j * 2;
                            break;
                        }

                    }

                }

                if (ActionablePathNode.getAction(node) == PathAction.SWIM) {
                    multiplier *= (node.y <= prevNode.y && !terrainMap.getBlockState(node.asBlockPos().above()).isAir() ? 3 : 1);
                    return prevNode.distanceTo(node) * 1.3F * multiplier;
                }

                BlockState state = terrainMap.getBlockState(node.asBlockPos());
                return prevNode.distanceTo(node) * BlockMetadata.getCost(state).orElse(state.isPathfindable(PathComputationType.AIR) ? 3.2F : 1) * multiplier;
            }
        };
    }

    public void setCirclingPath(Entity target, float preferredHeight, float preferredRadius) {
        setCirclingPath(target.position(), preferredHeight, preferredRadius);
    }

    public void setMovementType(MoveType moveType) {
		this.moveType = moveType;
	}

    public void enableDirectTarget(boolean enabled) {
		precisionTarget = enabled;
	}

    public void setLandingPath() {
		stop();
		setMovementType(MoveType.PREFER_WALKING);
		setWantsToBeFlying(false);
	}

    public void setCirclingPath(Vec3 pos, float preferredHeight, float preferredRadius) {
		stop();
		this.finalTarget = pos;
		this.circlingHeight = preferredHeight;
		this.circlingRadius = preferredRadius;
		this.isCircling = true;
	}

    public float getDistanceToCirclingRadius() {
		return finalTarget == null ? Float.MAX_VALUE : (float)theEntity.position().distanceTo(finalTarget) - circlingRadius;
	}

    public void setFlySpeed(float speed) {
		targetSpeed = speed;
	}

    public void setPitchBias(float pitch, float biasAmount) {
		pitchBias = pitch;
		pitchBiasAmount = biasAmount;
	}

	@Override
    protected void updateAutoPathToEntity() {
		double dist = theEntity.distanceTo(pathEndEntity);
		if (dist < closestDistToTarget - 1) {
			closestDistToTarget = ((float) dist);
			timeSinceGotCloser = 0;
		} else {
			timeSinceGotCloser++;
		}

		boolean pathUpdate = false;
		boolean needsPathfinder = false;
		if (path != null) {
			double dSq = Mth.square(dist);
			if ((moveType == MoveType.PREFER_FLYING || (moveType == MoveType.MIXED && dSq > 100)) && theEntity.hasLineOfSight(pathEndEntity)) {
				this.timeLookingForEntity = 0;
				pathUpdate = true;
			} else {
				double d1 = Math.sqrt(pathEndEntity.distanceToSqr(pathEndEntityLastPos));
				double d2 = Math.sqrt(theEntity.distanceToSqr(pathEndEntityLastPos));
				if (d1 / d2 > 0.1D) {
					pathUpdate = true;
				}
			}

		} else if (moveType == MoveType.PREFER_WALKING || timeSinceGotCloser > 160 || timeLookingForEntity > 600) {
			pathUpdate = true;
			needsPathfinder = true;
			timeSinceGotCloser = 0;
			timeLookingForEntity = 500;
		} else if (moveType == MoveType.MIXED) {
			double dSq = theEntity.distanceToSqr(pathEndEntity.position());
			if (dSq < 100) {
				pathUpdate = true;
			}

		}

		if (pathUpdate) {
			if (moveType == MoveType.PREFER_FLYING) {
				if (needsPathfinder) {
					theEntity.setPathfindFlying(true);
					path = createPath(theEntity, pathEndEntity, 0);
					if (path != null) {
						setWantsToBeFlying(true);
						startMovingAlong(path, moveSpeed);
					}

				} else {
					setWantsToBeFlying(true);
					resetStatus();
				}
			} else if (moveType == MoveType.MIXED) {
				theEntity.setPathfindFlying(false);
				Path path = createPath(theEntity, pathEndEntity, 0);
				if ((path != null) && (path.getNodeCount() < dist * 1.8D)) {
					setWantsToBeFlying(false);
					startMovingAlong(path, moveSpeed);
				} else if (needsPathfinder) {
					theEntity.setPathfindFlying(true);
					path = createPath(theEntity, pathEndEntity, 0);
					setWantsToBeFlying(true);
					if (path != null) {
						startMovingAlong(path, moveSpeed);
					} else {
						resetStatus();
					}
				} else {
					setWantsToBeFlying(true);
					resetStatus();
				}
			} else {
				setWantsToBeFlying(false);
				theEntity.setPathfindFlying(false);
				Path path = createPath(theEntity, pathEndEntity, 0);
				if (path != null) {
					startMovingAlong(path, moveSpeed);
				}
			}
			pathEndEntityLastPos = pathEndEntity.position();
		}
	}

	@Override
    public void autoPathToEntity(Entity target) {
		super.autoPathToEntity(target);
		this.isCircling = false;
	}

	@Override
    public boolean startMovingTo(Entity targetEntity, double speed) {
		if (moveType != MoveType.PREFER_WALKING) {
			stop();
			pathEndEntity = targetEntity;
			finalTarget = pathEndEntity.position();
			isCircling = false;
			return true;
		}

		theEntity.setPathfindFlying(false);
		return super.startMovingTo(targetEntity, speed);
	}

	@Override
    public boolean startMovingTo(double x, double y, double z, double speed) {
		if (moveType != MoveType.PREFER_WALKING) {
			stop();
			finalTarget = new Vec3(x, y, z);
			isCircling = false;
			return true;
		}

		theEntity.setPathfindFlying(false);
		return super.startMovingTo(x, y, z, speed);
	}

	@Override
    public void stop() {
		super.stop();
		pathEndEntity = null;
		isCircling = false;
	}

    public boolean isCircling() {
		return isCircling;
	}

	@Override
    protected void pathFollow() {
		Vec3 vec3d = getPos();
		int maxNextLeg = path.getNodeCount();

		float fa = Mth.square(theEntity.getBbWidth() * 0.5F);
		for (int j = path.getNextNodeIndex(); j < maxNextLeg; j++) {
			if (vec3d.distanceToSqr(path.getEntityPosAtNode(theEntity, j)) < fa) {
				path.setNextNodeIndex(j + 1);
			}
		}
	}

	@Override
    protected void noPathFollow() {
		if (theEntity.getMoveState() != MoveState.FLYING && theEntity.getAIGoal() == Goal.CHILL) {
			setWantsToBeFlying(false);
			return;
		}

		if (moveType == MoveType.PREFER_FLYING)
			setWantsToBeFlying(true);
		else if (moveType == MoveType.PREFER_WALKING) {
			setWantsToBeFlying(false);
		}
		if (++timeSinceVision >= visionUpdateRate) {
			timeSinceVision = 0;
			if (!precisionTarget || pathEndEntity == null) {
				updateHeading();
			} else {
				updateHeadingDirectTarget(pathEndEntity);
			}
			theEntity.setTargetPos(convertToVector(targetYaw, targetPitch, targetSpeed).toVector3f());
		}
		Vector3f target = new Vector3f(theEntity.getTargetPos());
		this.theEntity.getMoveControl().setWantedPosition(target.x, target.y, target.z, targetSpeed);
	}

	protected Vec3 convertToVector(float yaw, float pitch, float idealSpeed) {
		int time = visionUpdateRate + 20;
		double x = theEntity.getX() + -Math.sin(yaw * Mth.DEG_TO_RAD) * idealSpeed * time;
		double y = theEntity.getY() + Math.sin(pitch * Mth.DEG_TO_RAD) * idealSpeed * time;
		double z = theEntity.getZ() + Math.cos(yaw * Mth.DEG_TO_RAD) * idealSpeed * time;
		return new Vec3(x, y, z);
	}

	protected void updateHeading() {
		float pixelDegreeH = 10;
		float pixelDegreeV = 11;
		for (int i = 0; i < VISION_RESOLUTION_H; i++) {
			double nextAngleH = i * pixelDegreeH + 0.5D * pixelDegreeH - 150 + theEntity.getYRot();
			for (int j = 0; j < 20; j++) {
				double nextAngleV = j * pixelDegreeV + 0.5D * pixelDegreeV - 110;
				double y = theEntity.getY() + Math.sin(nextAngleV * Mth.DEG_TO_RAD) * visionDistance;
				double distanceXZ = Math.cos(nextAngleV * Mth.DEG_TO_RAD) * visionDistance;
				double x = theEntity.getX() + -Math.sin(nextAngleH * Mth.DEG_TO_RAD) * distanceXZ;
				double z = theEntity.getZ() + Math.cos(nextAngleH * Mth.DEG_TO_RAD) * distanceXZ;

				BlockHitResult object = this.theEntity.level().clip(new ClipContext(theEntity.position().add(0, 1, 0), new Vec3(x, y, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, theEntity));
				if ((object != null) && object.getType() == Type.BLOCK) {
					this.retina[i][j] = (float)object.getLocation().distanceTo(theEntity.position());
				} else {
					this.retina[i][j] = (this.visionDistance + 1);
				}
			}

		}

		for (int i = 1; i < 29; i++) {
			for (int j = 1; j < 19; j++) {
				float appeal = retina[i][j];
				appeal += retina[(i - 1)][(j - 1)];
				appeal += retina[(i - 1)][j];
				appeal += retina[(i - 1)][(j + 1)];
				appeal += retina[i][(j - 1)];
				appeal += retina[i][(j + 1)];
				appeal += retina[(i + 1)][(j - 1)];
				appeal += retina[(i + 1)][j];
				appeal += retina[(i + 1)][(j + 1)];
				appeal /= 9;
				headingAppeal[(i - 1)][(j - 1)] = appeal;
			}

		}

		if (this.isCircling) {
			double dX = finalTarget.x - theEntity.getX();
			double dY = finalTarget.y - theEntity.getY();
			double dZ = finalTarget.z - theEntity.getZ();
			double dXZ = Math.sqrt(dX * dX + dZ * dZ);

			if ((dXZ > 0) && (dXZ > this.circlingRadius * 0.6D)) {
				double intersectRadius = Math.abs((circlingRadius - dXZ) * 2) + 8;
				if (intersectRadius > this.circlingRadius * 1.8D) {
					intersectRadius = dXZ + 5;
				}

				float preferredYaw1 = (float) (Math.acos((dXZ * dXZ - circlingRadius * circlingRadius + intersectRadius * intersectRadius) / (2 * dXZ) / intersectRadius) * Mth.RAD_TO_DEG);
				float preferredYaw2 = -preferredYaw1;

				double dYaw = Math.atan2(dZ, dX) * Mth.RAD_TO_DEG - 90;
				preferredYaw1 = (float) (preferredYaw1 + dYaw);
				preferredYaw2 = (float) (preferredYaw2 + dYaw);

				float preferredPitch = (float) (Math.atan((dY + circlingHeight) / intersectRadius) * Mth.RAD_TO_DEG);

				float yawBias = (float) (1.5D * Math.abs(dXZ - circlingRadius) / circlingRadius);
				float pitchBias = (float) (1.9D * Math.abs((dY + circlingHeight) / circlingHeight));

				doHeadingBiasPass(this.headingAppeal, preferredYaw1, preferredYaw2, preferredPitch, yawBias, pitchBias);
			} else {
				float yawToTarget = (float) (Math.atan2(dZ, dX) * Mth.RAD_TO_DEG - 90);
				yawToTarget += 180;
				float preferredPitch = (float) (Math.atan((dY + circlingHeight) / Math.abs(circlingRadius - dXZ)) * Mth.RAD_TO_DEG);
				float yawBias = (float) (0.5D * Math.abs(dXZ - circlingRadius) / circlingRadius);
				float pitchBias = (float) (0.9D * Math.abs((dY + circlingHeight) / circlingHeight));
				doHeadingBiasPass(this.headingAppeal, yawToTarget, yawToTarget, preferredPitch, yawBias, pitchBias);
			}
		} else if (pathEndEntity != null) {
			Vec2 angles = MathUtil.toPolar(pathEndEntity.position().subtract(theEntity.position()));
			doHeadingBiasPass(headingAppeal, angles.y, angles.y, angles.x, 20.6F, 20.6F);
		}

		if (this.pathEndEntity == null) {
			float dOldYaw = Mth.degreesDifference(targetYaw, theEntity.getYRot());
			float dOldPitch = targetPitch;
			float approxLastTargetX = Mth.clamp(dOldYaw / pixelDegreeH + 14, 0, 28);
			float approxLastTargetY = Mth.clamp(dOldPitch / pixelDegreeV + 9, 0, 18);
			float statusQuoBias = 0.4F;
			float falloffDist = 30;
			for (int i = 0; i < 28; i++) {
				float dXSq = (approxLastTargetX - i) * (approxLastTargetX - i);
				for (int j = 0; j < 18; j++) {
					float dY = approxLastTargetY - j;
					headingAppeal[i][j] *= (float)(1 + statusQuoBias - statusQuoBias * Math.sqrt(dXSq + dY * dY) / falloffDist);
				}
			}
		}

		if (pitchBias != 0) {
			doHeadingBiasPass(headingAppeal, 0, 0, pitchBias, 0, pitchBiasAmount);
		}

		if (!wantsToBeFlying) {
		    FloatFloatPair landingInfo = appraiseLanding();
			if (landingInfo.secondFloat() < 4) {
				if (landingInfo.firstFloat() >= 0.9F)
					doHeadingBiasPass(headingAppeal, 0, 0, -45, 0, 3.5F);
				else if (landingInfo.firstFloat() >= 0.65F) {
					doHeadingBiasPass(headingAppeal, 0, 0, -15, 0, 0.4F);
				}

			} else if (landingInfo.firstFloat() >= 0.52F) {
				doHeadingBiasPass(this.headingAppeal, 0, 0, -15, 0, 0.8F);
			}

		}

		IntIntPair bestPixel = chooseCoordinate();
		targetYaw = (theEntity.getYRot() - 150 + (bestPixel.firstInt() + 1) * pixelDegreeH + 0.5F * pixelDegreeH);
		targetPitch = (-110 + (bestPixel.secondInt() + 1) * pixelDegreeV + 0.5F * pixelDegreeV);
	}

	protected void updateHeadingDirectTarget(Entity target) {
	    Vec2 angles = MathUtil.toPolar(target.position().subtract(theEntity.position()));
		this.targetYaw = angles.x;
		this.targetPitch = angles.y;
	}

	protected IntIntPair chooseCoordinate() {
		int bestPixelX = 0;
		int bestPixelY = 0;
		for (int i = 0; i < 28; i++) {
			for (int j = 0; j < 18; j++) {
				if (headingAppeal[bestPixelX][bestPixelY] < headingAppeal[i][j]) {
					bestPixelX = i;
					bestPixelY = j;
				}
			}
		}
		return IntIntPair.of(bestPixelX, bestPixelY);
	}

	protected void setTarget(double x, double y, double z) {
		theEntity.setTargetPos(new Vec3(x, y, z).toVector3f());
	}

	protected Vector3f getTarget() {
		return new Vector3f(theEntity.getTargetPos());
	}

	protected void doHeadingBiasPass(float[][] array, float preferredYaw1, float preferredYaw2, float preferredPitch, float yawBias, float pitchBias) {
		float pixelDegreeH = 10;
		float pixelDegreeV = 11;
		for (int i = 0; i < array.length; i++) {
			double nextAngleH = (i + 1) * pixelDegreeH + 0.5D * pixelDegreeH - 150 + theEntity.getYRot();
			double dYaw1 = Mth.wrapDegrees(preferredYaw1 - nextAngleH);
			double dYaw2 = Mth.wrapDegrees(preferredYaw2 - nextAngleH);
			double yawBiasAmount = 1 + Math.min(Math.abs(dYaw1), Math.abs(dYaw2)) * yawBias / 180;
			for (int j = 0; j < array[0].length; j++) {
				double nextAngleV = (j + 1) * pixelDegreeV + 0.5D * pixelDegreeV - 110;
				double pitchBiasAmount = 1 + Math.abs(Mth.wrapDegrees(preferredPitch - nextAngleV)) * pitchBias / 180;
				array[i][j] /= yawBiasAmount * pitchBiasAmount;
			}
		}
	}

	private void setWantsToBeFlying(boolean flag) {
		wantsToBeFlying = flag;
		theEntity.getMoveControl().setWantsToBeFlying(flag);
	}

	private FloatFloatPair appraiseLanding() {
		float safety = 0;
		float distance = 0;
		int landingResolution = 3;
		double nextAngleH = theEntity.getYRot();
		for (int i = 0; i < landingResolution; i++) {
			double nextAngleV = -90 + i * VISION_RESOLUTION_H / landingResolution;
			double y = theEntity.getY() + Math.sin(nextAngleV * Mth.DEG_TO_RAD) * 64;
			double distanceXZ = Math.cos(nextAngleV * Mth.DEG_TO_RAD) * 64;
			double x = theEntity.getX() + -Math.sin(nextAngleH * Mth.DEG_TO_RAD) * distanceXZ;
			double z = theEntity.getZ() + Math.cos(nextAngleH * Mth.DEG_TO_RAD) * distanceXZ;
			Vec3 origin = theEntity.position();
			BlockHitResult hit = theEntity.level().clip(new ClipContext(origin, new Vec3(x, y, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, theEntity));
			if (hit != null && hit.getType() == Type.BLOCK) {
				BlockState Block = theEntity.level().getBlockState(hit.getBlockPos());
				if (!actor.avoidsBlock(Block)) {
					safety += 0.7F;
				}
				if (hit.getDirection() == Direction.UP) {
					safety += 0.3F;
				}
				distance = (float)hit.getLocation().distanceTo(theEntity.position());
			} else {
				distance += 64;
			}
		}
		return FloatFloatPair.of(safety / landingResolution, distance / landingResolution);
	}

	public enum MoveType {
        PREFER_WALKING, MIXED, PREFER_FLYING;
    }
}
