package com.invasion.entity;

import org.joml.Vector3f;

import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.entity.ai.goal.EntityAIBirdFight;
import com.invasion.entity.ai.goal.BirdOfPreyGoal;
import com.invasion.entity.ai.goal.FlyingCircleTargetGoal;
import com.invasion.entity.ai.goal.FlyingStrikeGoal;
import com.invasion.entity.ai.goal.FlyingTackleGoal;
import com.invasion.entity.ai.goal.PickUpEntityGoal;
import com.invasion.entity.ai.goal.StabiliseFlightGoal;
import com.invasion.entity.ai.goal.SwoopGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.invasion.entity.ai.goal.LookAtTargetGoal;

public class EntityIMGiantBird extends VultureEntity {
    private static final Vector3f PICKUP_OFFSET = new Vector3f(0, 0.2F, -0.92F);
    private static final float MODEL_ROTATION_OFFSET_Y = 1.9F;
    private static final byte TRIGGER_SQUAWK = 10;
    private static final byte TRIGGER_SCREECH = 11;
    private static final byte TRIGGER_DEATHSOUND = 12;

    public EntityIMGiantBird(EntityType<EntityIMGiantBird> type, Level world) {
        super(type, world);
        setThrust(0.028F);
        setMaxPoweredFlightSpeed(0.9F);
        setLiftFactor(0.35F);
        setThrustComponentRatioMin(0.0F);
        setThrustComponentRatioMax(0.5F);
        setMaxTurnForce((float)getDefaultGravity() * 8.0F);
    }

    public static AttributeSupplier.Builder createVultureAttributes() {
        return createBirdAttributes()
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.GRAVITY, 0.03F)
                .add(Attributes.MOVEMENT_SPEED, 0.4F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new SwoopGoal(this));

        goalSelector.addGoal(3, new BirdOfPreyGoal(this));
        goalSelector.addGoal(4, new FlyingStrikeGoal(this));
        goalSelector.addGoal(4, new FlyingTackleGoal(this));
        goalSelector.addGoal(4, new PickUpEntityGoal(this, PICKUP_OFFSET, 1.5F, 1.5F, 20, 45, 45));
        goalSelector.addGoal(4, new StabiliseFlightGoal(this, 35));
        goalSelector.addGoal(4, new FlyingCircleTargetGoal(this, 300, 16.0F, 45.0F));
        goalSelector.addGoal(4, new EntityAIBirdFight<>(this, Zombie.class, 25, 0.4F));
        goalSelector.addGoal(4, new LookAtTargetGoal(this));

        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(this, Zombie.class, 58.0F, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (InvasionMod.getConfig().debugMode && !level().isClientSide()) {
            setCustomName(Component.literal(getAIGoal() + "\n" + getNavigatorNew()));
        }
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction positionUpdater) {
        super.positionRider(passenger, positionUpdater);
        passenger.setYRot(getCarriedEntityYawOffset() + getYRot());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        double x = PICKUP_OFFSET.x;
        double y = -MODEL_ROTATION_OFFSET_Y;
        double z = -PICKUP_OFFSET.z;

        double dAngle = getXRot() * Mth.DEG_TO_RAD;
        double sinF = Math.sin(dAngle);
        double cosF = Math.cos(dAngle);
        double tmp = z * cosF - y * sinF;
        y = y * cosF + z * sinF;
        z = tmp;

        dAngle = getYRot() * Mth.DEG_TO_RAD;
        sinF = Math.sin(dAngle);
        cosF = Math.cos(dAngle);

        return new Vec3(
                x * cosF - z * sinF,
                y + MODEL_ROTATION_OFFSET_Y,
                z * cosF + x * sinF
        );
    }

    @Override
    public void doScreech() {
        if (!level().isClientSide()) {
            playSound(InvSounds.ENTITY_VULTURE_SCREECH, 6, 1 + (getRandom().nextFloat() * 0.2F - 0.1F));
            level().broadcastEntityEvent(this, TRIGGER_SCREECH);
        } else {
            setBeakState(35);
        }
    }

    @Override
    public void doMeleeSound() {
        doSquawk();
    }

    @Override
    protected void doHurtSound() {
        doSquawk();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return InvSounds.ENTITY_VULTURE_SQUAWK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return InvSounds.ENTITY_VULTURE_DEATH;
    }

    @Override
    protected void doDeathSound() {
        if (!level().isClientSide()) {
            level().broadcastEntityEvent(this, TRIGGER_DEATHSOUND);
        } else {
            setBeakState(25);
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        super.handleEntityEvent(status);
        if (status == TRIGGER_SQUAWK) {
            doSquawk();
        } else if (status == TRIGGER_SCREECH) {
            doScreech();
        } else if (status == TRIGGER_DEATHSOUND) {
            doDeathSound();
        }
    }

    private void doSquawk() {
        if (!level().isClientSide()) {
            playSound(InvSounds.ENTITY_VULTURE_SQUAWK, 1.9F, 1.0F + getRandom().nextFloat() * 0.2F - 0.1F);
            level().broadcastEntityEvent(this, TRIGGER_SQUAWK);
        } else {
            setBeakState(10);
        }
    }

    @Override
    public String getLegacyName() {
        return "IMVulture-T" + getTier();
    }
}