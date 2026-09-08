package com.invasion.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class IMHuskEntity extends EntityIMZombie {
    private static final EntityDataAccessor<Boolean> CONVERTING =
            SynchedEntityData.defineId(
                    IMHuskEntity.class, EntityDataSerializers.BOOLEAN);
    private int inWaterTime;
    private int conversionTime = -1;

    public IMHuskEntity(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CONVERTING, false);
    }

    public boolean isUnderWaterConverting() {
        return entityData.get(CONVERTING);
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && isAlive() && !isNoAi()) {
            if (isUnderWaterConverting()) {
                if (--conversionTime < 0
                        && level() instanceof ServerLevel world) {
                    convertToIMZombie(world);
                }
            } else if (isEyeInFluid(FluidTags.WATER)) {
                if (++inWaterTime >= 600) {
                    conversionTime = 300;
                    entityData.set(CONVERTING, true);
                }
            } else {
                inWaterTime = -1;
            }
        }
        super.tick();
    }

    private void convertToIMZombie(ServerLevel world) {
        EntityIMZombie zombie = ZombieVariants.resolve(InvEntities.ZOMBIE, getTier(), getFlavour()).create(
                world, EntitySpawnReason.CONVERSION);
        if (zombie == null) {
            return;
        }

        zombie.setFlavour(getFlavour());
        zombie.setTier(getTier());
        Entity vehicle = getVehicle();
        stopRiding();
        zombie.snapTo(
                getX(), getY(), getZ(), getYRot(), getXRot());
        zombie.setDeltaMovement(getDeltaMovement());
        zombie.setBaby(isBaby());
        zombie.setCustomName(getCustomName());
        zombie.setCustomNameVisible(isCustomNameVisible());
        zombie.setNoAi(isNoAi());
        zombie.setCanPickUpLoot(canPickUpLoot());
        zombie.setNexus(getNexus());
        zombie.setCountsTowardMobCap(true);
        if (isPersistenceRequired()) {
            zombie.setPersistenceRequired();
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            zombie.setItemSlot(slot, getItemBySlot(slot).copy());
        }
        float healthRatio = getHealth() / getMaxHealth();
        zombie.setHealth(zombie.getMaxHealth() * healthRatio);

        discard();
        if (world.addFreshEntity(zombie)
                && vehicle != null && !vehicle.isRemoved()) {
            zombie.startRiding(vehicle);
        }
        if (!isSilent()) {
            world.levelEvent(null, 1041, blockPosition(), 0);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        boolean hit = super.doHurtTarget(world, target);
        if (hit && getMainHandItem().isEmpty()
                && target instanceof LivingEntity living) {
            int duration = 140 * (int) world
                    .getCurrentDifficultyAt(blockPosition())
                    .getEffectiveDifficulty();
            living.addEffect(
                    new MobEffectInstance(MobEffects.HUNGER, duration),
                    this);
        }
        return hit;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(
            net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.HUSK_STEP, 0.15F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("InWaterTime", inWaterTime);
        if (isUnderWaterConverting()) {
            output.putInt("ZombieConversionTime", conversionTime);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        inWaterTime = input.getIntOr("InWaterTime", 0);
        conversionTime = input.getIntOr(
                "ZombieConversionTime", -1);
        entityData.set(CONVERTING, conversionTime >= 0);
    }
}
