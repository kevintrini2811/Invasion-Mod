package com.invasion.entity;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.invasion.entity.ai.FlyState;
import com.invasion.entity.ai.FlyingEntityLookControl;
import com.invasion.entity.ai.FlyingMoveControl;
import com.invasion.entity.ai.MoveState;
import com.invasion.entity.pathfinding.FlyingNavigation;
import com.invasion.entity.pathfinding.Navigation;
import com.invasion.entity.pathfinding.PathCreator;
import com.invasion.util.math.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class EntityIMFlying extends EntityIMLiving implements Animatable {
    private static final EntityDataAccessor<Vector3fc> TARGET_POS = SynchedEntityData.defineId(EntityIMFlying.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> THRUSTING = SynchedEntityData.defineId(EntityIMFlying.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> THRUST_EFFORT = SynchedEntityData.defineId(EntityIMFlying.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FLY_STATE = SynchedEntityData.defineId(EntityIMFlying.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOVE_STATE = SynchedEntityData.defineId(EntityIMFlying.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGLES = SynchedEntityData.defineId(EntityIMFlying.class, EntityDataSerializers.INT);

	private float liftFactor = 0.4F;
	private float maxPoweredFlightSpeed = 0.28F;
	private float thrust = 0.08F;
	private float thrustComponentRatioMin = 0;
	private float thrustComponentRatioMax = 0.1F;
	private float maxTurnForce = (float)(getDefaultGravity() * 3);

    private float rotationRoll;
    private float prevRotationRoll;

	private float optimalPitch = 52;
	private float maxRunSpeed = 0.45F;

	private Vec3 accelleration = Vec3.ZERO;

	private boolean flyPathfind = true;
	private boolean debugFlying = true;

    protected float airResistance = DEFAULT_AIR_RESISTANCE;
    protected float groundFriction = DEFAULT_GROUND_FRICTION;

	public EntityIMFlying(EntityType<? extends EntityIMFlying> type, Level world) {
		super(type, world);
		moveControl = new FlyingMoveControl(this);
		lookControl = new FlyingEntityLookControl(this);
	}

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_POS, new Vector3f());
        builder.define(THRUSTING, false);
        builder.define(THRUST_EFFORT, 1F);
        builder.define(FLY_STATE, FlyState.GROUNDED.ordinal());
        builder.define(MOVE_STATE, MoveState.STANDING.ordinal());
        builder.define(ANGLES, 0);
    }

    public Vector3fc getTargetPos() {
        return entityData.get(TARGET_POS);
    }

    public void setTargetPos(Vector3fc target) {
        entityData.set(TARGET_POS, target);
    }

	@Override
    protected Navigation createIMNavigation() {
	    return new FlyingNavigation(this, new PathCreator(800, 200));
	}

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

	public FlyState getFlyState() {
		return FlyState.of(entityData.get(FLY_STATE));
	}

    public float getRoll(float tickDelta) {
        return Mth.rotLerp(tickDelta, prevRotationRoll, rotationRoll);
    }

    public void setRoll(float roll) {
        rotationRoll = roll;
    }

    public void setGroundFriction(float frictionCoefficient) {
        groundFriction = frictionCoefficient;
    }

	public boolean isThrustOn() {
		return entityData.get(THRUSTING);
	}

	public float getThrustEffort() {
		return entityData.get(THRUST_EFFORT);
	}

    public void setThrustEffort(float effortFactor) {
        entityData.set(THRUST_EFFORT, effortFactor);
    }

	@Deprecated
	public Vector3f getFlyTarget() {
		return new Vector3f(getTargetPos());
	}

    @Override
    public boolean onClimbable() {
        return false;
    }

    public boolean hasFlyingDebug() {
        return this.debugFlying;
    }

    public void setPathfindFlying(boolean flag) {
        this.flyPathfind = flag;
    }

    public boolean getPathFindFlying() {
        return flyPathfind;
    }

    public void setFlyState(FlyState flyState) {
        entityData.set(FLY_STATE, flyState.ordinal());
    }

    public float getMaxPoweredFlightSpeed() {
        return this.maxPoweredFlightSpeed;
    }

    public float getLiftFactor() {
        return this.liftFactor;
    }

    protected void setLiftFactor(float liftFactor) {
        this.liftFactor = liftFactor;
    }

    public float getThrust() {
        return this.thrust;
    }

    protected void setThrust(float thrust) {
        this.thrust = thrust;
    }

    public float getThrustComponentRatioMin() {
        return this.thrustComponentRatioMin;
    }

    public float getThrustComponentRatioMax() {
        return this.thrustComponentRatioMax;
    }

    public float getMaxTurnForce() {
        return this.maxTurnForce;
    }

    public float getMaxPitch() {
        return this.optimalPitch;
    }

    public float getLandingSpeedThreshold() {
        return getSpeed() * 1.2F;
    }

    protected float getMaxRunSpeed() {
        return this.maxRunSpeed;
    }

    public void setAcceleration(Vec3 accelleration) {
       this.accelleration = accelleration;
    }

    public void setThrustOn(boolean flag) {
        entityData.set(THRUSTING, flag);
    }

    protected void setMaxPoweredFlightSpeed(float speed) {
        this.maxPoweredFlightSpeed = speed;
        ((FlyingNavigation)getNavigatorNew()).setFlySpeed(speed);
    }

    protected void setThrustComponentRatioMin(float ratio) {
        thrustComponentRatioMin = ratio;
    }

    protected void setThrustComponentRatioMax(float ratio) {
        thrustComponentRatioMax = ratio;
    }

    protected void setMaxTurnForce(float maxTurnForce) {
        this.maxTurnForce = maxTurnForce;
    }

    protected void setOptimalPitch(float pitch) {
        optimalPitch = pitch;
    }

    protected void setMaxRunSpeed(float speed) {
        maxRunSpeed = speed;
    }

    @Override
    public MoveState getMoveState() {
        return MoveState.of(entityData.get(MOVE_STATE));
    }

    @Override
    public void setMoveState(MoveState moveState) {
        entityData.set(MOVE_STATE, moveState.ordinal());
    }

	@Override
	public FlyingMoveControl getMoveControl() {
		return (FlyingMoveControl)super.getMoveControl();
	}

	@Override
	public FlyingEntityLookControl getLookControl() {
		return (FlyingEntityLookControl)super.getLookControl();
	}

    @Override
    public void aiStep() {
        super.aiStep();
        int packedAngles = MathUtil.packAnglesDeg(getVisualRotationYInDegrees(), getYHeadRot(), getXRot(), 0);
        if (packedAngles != entityData.get(ANGLES)) {
            entityData.set(ANGLES, packedAngles);
        }
    }

    // based on FlyingEntity (originals in comments)
	@Override
	public void travel(Vec3 movementInput) {
	    if (isEffectiveAi()) {
    		if (isInWater()) {
    			moveRelative(isNoAi() ? 0.02F : 0.04F /*0.02F*/, movementInput);
    			move(MoverType.SELF, getDeltaMovement());
    			setDeltaMovement(getDeltaMovement().scale(0.8F));
    		} else if (isInLava()) {
                moveRelative(isNoAi() ? 0.02F : 0.04F /*0.02F*/, movementInput);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.5));
    		} else {
    		    float groundFriction = 0.9995F;
                if (onGround()) {
                    groundFriction = level().getBlockState(getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
                }

                float landMoveSpeed = 0.16277137F / (groundFriction * groundFriction * groundFriction);
                groundFriction = this.groundFriction;
                if (onGround()) {
                    groundFriction = level().getBlockState(getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
                }

                /** IM additions **/
                // added air resistance
                groundFriction *= airResistance;

                // added accelleration
                push(accelleration);
                /** end IM additions **/

    			moveRelative(onGround() ? 0.1F * landMoveSpeed : 0.02F, movementInput);
    			move(MoverType.SELF, getDeltaMovement());
    			//setVelocity(getVelocity().multiply(groundFriction));
    			setDeltaMovement(getDeltaMovement().multiply(groundFriction, airResistance, airResistance).add(-getDefaultGravity(), 0, 0));
    		}
	    }

	    calculateEntityAnimation(false);
	}

	@Override
	protected void checkFallDamage(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == ANGLES) {
            int packedAngles = entityData.get(ANGLES);
            setYBodyRot(MathUtil.unpackAnglesDeg_1(packedAngles));
            setYHeadRot(MathUtil.unpackAnglesDeg_2(packedAngles));
            setXRot(MathUtil.unpackAnglesDeg_3(packedAngles));
        }
    }

}
