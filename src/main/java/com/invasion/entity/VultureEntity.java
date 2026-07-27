package com.invasion.entity;

import com.invasion.client.render.animation.AnimationAction;
import com.invasion.client.render.animation.AnimationRegistry;
import com.invasion.client.render.animation.AnimationState;
import com.invasion.entity.animation.LegController;
import com.invasion.entity.animation.MouthController;
import com.invasion.entity.animation.WingController;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class VultureEntity extends EntityIMFlying {
    @Deprecated
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CLAWS_FORWARD = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BEAK_DOWN = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACKING_WITH_WINGS = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.BOOLEAN);

    private final WingController wingController = AnimationRegistry.instance().get("wing_flap_2_piece").createState(this, AnimationAction.WINGTUCK, WingController::new);
    private final LegController legController = AnimationRegistry.instance().get("bird_run").createState(this, AnimationAction.STAND, LegController::new);
    private final MouthController beakController = AnimationRegistry.instance().get("bird_beak").createState(this, AnimationAction.MOUTH_CLOSE, MouthController::new);

    private float carriedEntityYawOffset;

    public VultureEntity(EntityType<? extends VultureEntity> type, Level world) {
        super(type, world);
        setThrust(0.1F);
        setMaxPoweredFlightSpeed(0.5F);
        setLiftFactor(0.35F);
        setThrustComponentRatioMin(0);
        setThrustComponentRatioMax(0.5F);
        setMaxTurnForce((float)getDefaultGravity() * 8);
    }

    public static AttributeSupplier.Builder createBirdAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 1)
                .add(Attributes.ATTACK_DAMAGE, 1)
                .add(Attributes.GRAVITY, 0.025);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TIER, 1);
        builder.define(CLAWS_FORWARD, false);
        builder.define(ATTACKING_WITH_WINGS, false);
    }

    @Deprecated
    public int getTier() {
        return entityData.get(TIER);
    }

    @Deprecated
    protected void setTier(int tier) {
        tier = Math.max(1, tier);
        entityData.set(TIER, tier);
    }

    public boolean getClawsForward() {
        return entityData.get(CLAWS_FORWARD);
    }

    public void setClawsForward(boolean flag) {
        entityData.set(CLAWS_FORWARD, flag);
    }

    public boolean isAttackingWithWings() {
        return entityData.get(ATTACKING_WITH_WINGS);
    }

    public void setAttackingWithWings(boolean flag) {
        entityData.set(ATTACKING_WITH_WINGS, flag);
    }

    public boolean isBeakOpen() {
        return entityData.get(BEAK_DOWN);
    }

    protected void setBeakOpen(boolean flag) {
        entityData.set(BEAK_DOWN, flag);
    }

    public float getCarriedEntityYawOffset() {
        return carriedEntityYawOffset;
    }

    public AnimationState<?> getWingAnimationState() {
        return wingController.getState();
    }

    public float getLegSweepProgress() {
        return 1.0F;
    }

    public AnimationState<?> getLegAnimationState() {
        return legController.getState();
    }

    public AnimationState<?> getBeakAnimationState() {
        return beakController.getState();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (level().isClientSide()) {
            updateFlapAnimation();
            updateLegAnimation();
            updateBeakAnimation();
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {

    }

    public void doScreech() {
    }

    public void doMeleeSound() {
    }

    protected void doHurtSound() {
    }

    protected void doDeathSound() {
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        doDeathSound();
    }

    @Override
    protected void playHurtSound(DamageSource damageSource) {
        super.playHurtSound(damageSource);
        doHurtSound();
    }

    protected void setBeakState(int timeOpen) {
        beakController.setMouthState(timeOpen);
    }

    protected void onPickedUpEntity(Entity entity) {
        carriedEntityYawOffset = (entity.getYRot() - entity.getYRot());
    }

    protected void updateFlapAnimation() {
        wingController.update();
    }

    protected void updateLegAnimation() {
        legController.update();
    }

    protected void updateBeakAnimation() {
        beakController.update();
    }
}