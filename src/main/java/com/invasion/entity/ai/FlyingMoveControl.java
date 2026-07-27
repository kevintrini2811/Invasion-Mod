package com.invasion.entity.ai;

import com.invasion.entity.EntityIMFlying;
import com.invasion.util.math.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FlyingMoveControl extends ClimbableMoveControl {
	private EntityIMFlying entity;
	private double targetFlySpeed;
	private boolean wantsToBeFlying;
	private boolean needsUpdate;

    protected double targetSpeed;

	public FlyingMoveControl(EntityIMFlying entity) {
		super(entity);
		this.entity = entity;
		this.wantsToBeFlying = false;
	}

	@Override
    public void setWantedPosition(double x, double y, double z, double speed) {
	    super.setWantedPosition(x, y, z, speed);
	    needsUpdate = true;
	}

	@Override
    public void strafe(float forward, float sideways) {
	    super.strafe(forward, sideways);
	    needsUpdate = true;
	}

	public void setHeading(float yaw, float pitch, float idealSpeed, int time) {
		double x = entity.getX() + Math.sin(yaw * Mth.DEG_TO_RAD) * idealSpeed * time;
		double y = entity.getY() + Math.sin(pitch * Mth.DEG_TO_RAD) * idealSpeed * time;
		double z = entity.getZ() + Math.cos(yaw * Mth.DEG_TO_RAD) * idealSpeed * time;
		setWantedPosition(x, y, z, idealSpeed);
	}

	public void setWantsToBeFlying(boolean flag) {
		this.wantsToBeFlying = flag;
	}

	@Override
    public void tick() {
		entity.setZza(0);
		entity.setAcceleration(Vec3.ZERO);
		if ((!needsUpdate) && (entity.getMoveState() != MoveState.FLYING)) {
			entity.setMoveState(MoveState.STANDING);
			entity.setFlyState(FlyState.GROUNDED);
			entity.setXRot(rotlerp(entity.getXRot(), 50, 4));
			return;
		}
		needsUpdate = false;

		if (wantsToBeFlying) {
			if (entity.getFlyState() == FlyState.GROUNDED) {
				entity.setMoveState(MoveState.RUNNING);
				entity.setFlyState(FlyState.TAKEOFF);
			} else if (entity.getFlyState() == FlyState.FLYING) {
				entity.setMoveState(MoveState.FLYING);
			}

		} else if (entity.getFlyState() == FlyState.FLYING) {
			entity.setFlyState(FlyState.LANDING);
		}

		if (entity.getFlyState() == FlyState.FLYING) {
			FlyState result = doFlying();
			if (result == FlyState.GROUNDED)
				entity.setMoveState(MoveState.STANDING);
			else if (result == FlyState.FLYING) {
				entity.setMoveState(MoveState.FLYING);
			}
			entity.setFlyState(result);
		} else if (entity.getFlyState() == FlyState.TAKEOFF) {
			FlyState result = doTakeOff();
			if (result == FlyState.GROUNDED)
				entity.setMoveState(MoveState.STANDING);
			else if (result == FlyState.TAKEOFF)
				entity.setMoveState(MoveState.RUNNING);
			else if (result == FlyState.FLYING) {
				entity.setMoveState(MoveState.FLYING);
			}
			entity.setFlyState(result);
		} else if (entity.getFlyState() == FlyState.LANDING || entity.getFlyState() == FlyState.TOUCHDOWN) {
			FlyState result = doLanding();
			if (result == FlyState.GROUNDED || result == FlyState.TOUCHDOWN) {
				entity.setMoveState(MoveState.RUNNING);
			}
			entity.setFlyState(result);
		} else {
		    entity.setGroundFriction(0);
	        entity.setRoll(rotlerp(entity.getRoll(1), 0, 6));
	        targetSpeed = entity.getSpeed();
	        entity.setXRot(rotlerp(entity.getXRot(), 50, 4));
			super.tick();
		}
	}

	protected FlyState doFlying() {
		this.targetFlySpeed = this.speedModifier;
		return fly();
	}

	protected FlyState fly() {
		entity.setGroundFriction(1);
		Vec3 delta = new Vec3(wantedX, wantedY, wantedZ).subtract(entity.position());

		double dXZSq = delta.horizontalDistanceSqr();
		double distanceSquared = dXZSq + Mth.square(delta.y);

		if (distanceSquared > 0.04D) {
			int timeToTurn = 10;
			float gravity = (float)entity.getGravity();
			float liftConstant = gravity;
			Vec3 acelleration = Vec3.ZERO;
			Vec3 velocity = entity.getDeltaMovement();
			double hSpeedSq = velocity.horizontalDistanceSqr();
			if (hSpeedSq == 0) {
				hSpeedSq = 1.0E-008D;
			}
			double horizontalSpeed = Math.sqrt(hSpeedSq);
			double flySpeed = Math.sqrt(hSpeedSq + Mth.square(velocity.y));

			double desiredYVelocity = delta.y / timeToTurn;
			double dVelY = desiredYVelocity - (velocity.y - gravity);

			float minFlightSpeed = 0.05F;
			if (flySpeed < minFlightSpeed) {
				entity.setYRot(rotlerp(entity.getYRot(), (float) (Math.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG - 90), getTurnRate()));
				if (entity.onGround()) {
					return FlyState.GROUNDED;
				}
			} else {
				double liftForce = flySpeed / (entity.getMaxPoweredFlightSpeed() * entity.getLiftFactor()) * liftConstant;
				double climbForce = liftForce * horizontalSpeed / (Math.abs(velocity.y) + horizontalSpeed);
				double forwardForce = liftForce * Math.abs(velocity.y) / (Math.abs(velocity.y) + horizontalSpeed);
				double turnForce = liftForce;
				double climbAccel;
				if (dVelY < 0.0D) {
					double maxDiveForce = entity.getMaxTurnForce() - gravity;
					climbAccel = -Math.min(Math.min(climbForce, maxDiveForce), -dVelY);
				} else {
					double maxClimbForce = entity.getMaxTurnForce() + gravity;
					climbAccel = Math.min(Math.min(climbForce, maxClimbForce), dVelY);
				}

				float minBankForce = 0.01F;
				if (turnForce < minBankForce) {
					turnForce = minBankForce;
				}

				double desiredXZHeading = Math.atan2(delta.z, delta.x) - 1.570796326794897D;
				double currXZHeading = Math.atan2(velocity.z, velocity.x) - 1.570796326794897D;
				double dXZHeading = MathUtil.boundAnglePiRad(desiredXZHeading - currXZHeading);
				double bankForce = horizontalSpeed * dXZHeading / timeToTurn;
				double maxBankForce = Math.min(turnForce, entity.getMaxTurnForce());
				if (bankForce > maxBankForce)
					bankForce = maxBankForce;
				else if (bankForce < -maxBankForce) {
					bankForce = -maxBankForce;
				}

				double bankXAccel = bankForce * -velocity.z / horizontalSpeed;
				double bankZAccel = bankForce * velocity.x / horizontalSpeed;

				acelleration = acelleration.add(bankXAccel, climbAccel, bankZAccel);
				velocity = velocity.add(bankXAccel, climbAccel, bankZAccel);

				double middlePitch = 15.0D;
				double newPitch;
				if (velocity.y - gravity < 0) {
					double climbForceRatio = acelleration.y / climbForce;
					if (climbForceRatio > 1.0D)
						climbForceRatio = 1.0D;
					else if (climbForceRatio < -1.0D) {
						climbForceRatio = -1.0D;
					}
					double xzSpeed = velocity.horizontalDistance();
					double velPitch = xzSpeed > 0 ? Math.atan(velocity.y / xzSpeed) * Mth.RAD_TO_DEG : -180;
					double pitchInfluence = Math.max(0, (entity.getMaxPoweredFlightSpeed() - Math.abs(velocity.y)) / entity.getMaxPoweredFlightSpeed());
					newPitch = velPitch + 15 * climbForceRatio * pitchInfluence;
				} else {
					double pitchLimit = entity.getMaxPitch();
					double climbForceRatio = Math.min(acelleration.y / climbForce, 1.0D);
					newPitch = middlePitch + (pitchLimit - middlePitch) * climbForceRatio;
				}
				newPitch = rotlerp(entity.getXRot(), (float) newPitch, 1.5F);
				double newYaw = Math.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG - 90;
				newYaw = rotlerp(entity.getYRot(), (float) newYaw, getTurnRate());
				entity.absSnapTo(entity.getX(), entity.getY(), entity.getZ(), (float) newYaw, (float) newPitch);
				double newRoll = 60 * bankForce / turnForce;
				entity.setRoll(rotlerp(entity.getRoll(1), (float) newRoll, 6));
				double horizontalForce = velocity.y > 0 ? -climbAccel : forwardForce;
				int xDirection = velocity.x > 0 ? 1 : -1;
				int zDirection = velocity.z > 0 ? 1 : -1;
				double hComponentX = xDirection * velocity.x / (xDirection * velocity.x + zDirection * velocity.z);

				double xLiftAccel = xDirection * horizontalForce * hComponentX;
				double zLiftAccel = zDirection * horizontalForce * (1 - hComponentX);

				double loss = 0.4D;
				xLiftAccel += xDirection * -Math.abs(bankForce * loss) * hComponentX;
				zLiftAccel += zDirection * -Math.abs(bankForce * loss) * (1 - hComponentX);

				acelleration = acelleration.add(xLiftAccel, 0, zLiftAccel);
			}

			if (flySpeed < this.targetFlySpeed) {
				entity.setThrustEffort(0.6F);
				if (!entity.isThrustOn()) {
					entity.setThrustOn(true);
				}
				acelleration = acelleration.add(calcThrust((dVelY - acelleration.y) / entity.getThrust()));
			} else if (flySpeed > this.targetFlySpeed * 1.8D) {
				entity.setThrustEffort(1.0F);
				if (!entity.isThrustOn()) {
					entity.setThrustOn(true);
				}
				acelleration = acelleration.add(calcThrust((dVelY - acelleration.y) / (entity.getThrust() * 10.0F)).multiply(-1, 1, -1));
			} else if (entity.isThrustOn()) {
				entity.setThrustOn(false);
			}

			entity.setAcceleration(acelleration);
		}
		return FlyState.FLYING;
	}

	protected FlyState doTakeOff() {
		entity.setGroundFriction(0.98F);
		entity.setThrustOn(true);
		entity.setThrustEffort(1);
		targetSpeed = entity.getSpeed();

		tick();
		if (entity.getMoveState() == MoveState.STANDING) {
			return FlyState.GROUNDED;
		}
		if (entity.horizontalCollision) {
			entity.getJumpControl().jump();
		}
		entity.setAcceleration(calcThrust(0));
		double speed = entity.getDeltaMovement().length();

		entity.setXRot(rotlerp(entity.getXRot(), 40, 4));

		float gravity = (float)entity.getGravity();
		float liftConstant = gravity;
		double liftForce = speed / (entity.getMaxPoweredFlightSpeed() * entity.getLiftFactor()) * liftConstant;

		return liftForce > gravity ? FlyState.FLYING : FlyState.TAKEOFF;
	}

	protected FlyState doLanding() {
		entity.setGroundFriction(0.3F);
		BlockPos.MutableBlockPos mutable = entity.blockPosition().mutable();

		for (int i = 1; i < 5; i++) {
			if (!entity.level().isEmptyBlock(mutable.move(Direction.DOWN))) {
				break;
			}
			targetFlySpeed = (speedModifier * (0.66F - (0.4F - (i - 1) * 0.133F)));
		}

		FlyState result = fly();
		entity.setThrustOn(true);
		if (result == FlyState.FLYING && entity.onGround()) {
			if (entity.getDeltaMovement().length() < entity.getLandingSpeedThreshold()) {
				return FlyState.GROUNDED;
			}

			entity.setRoll(rotlerp(entity.getRoll(1), 40, 6));
			return FlyState.TOUCHDOWN;
		}

		return FlyState.LANDING;
	}

	protected Vec3 calcThrust(double desiredVThrustRatio) {
		double vThrustRatio = Mth.clamp(desiredVThrustRatio, entity.getThrustComponentRatioMin(), entity.getThrustComponentRatioMax());
		double hThrust = (1 - vThrustRatio) * entity.getThrust();
		return new Vec3(
		        hThrust * -Math.sin(entity.getYRot() * Mth.DEG_TO_RAD),
		        vThrustRatio * entity.getThrust(),
		        hThrust * Math.cos(entity.getYRot() * Mth.DEG_TO_RAD)
        );
	}
}