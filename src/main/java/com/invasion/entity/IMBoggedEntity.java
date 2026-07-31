package com.invasion.entity;

import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public final class IMBoggedEntity extends IMSkeletonEntity implements Shearable {
    private static final EntityDataAccessor<Boolean> SHEARED =
            SynchedEntityData.defineId(
                    IMBoggedEntity.class, EntityDataSerializers.BOOLEAN);

    public IMBoggedEntity(
            EntityType<? extends IMSkeletonEntity> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IMSkeletonEntity.createIMSkeletonAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHEARED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("sheared", isSheared());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        setSheared(input.getBoolean("sheared"));
    }

    public boolean isSheared() {
        return entityData.get(SHEARED);
    }

    public void setSheared(boolean sheared) {
        entityData.set(SHEARED, sheared);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new PredicatedGoal(
                new BoggedKillWithArrowGoal(this),
                () -> getMainHandItem().is(Items.BOW)));
        goalSelector.addGoal(2, new PredicatedGoal(
                new SkeletonAttackNexusGoal<>(this),
                () -> getMainHandItem().is(Items.BOW)));
    }

    @Override
    protected AbstractArrow createArrowProjectile(
            ItemStack arrowStack, float damageModifier,
            ItemStack shotFrom) {
        AbstractArrow projectile = super.createArrowProjectile(
                arrowStack, damageModifier, shotFrom);
        if (projectile instanceof Arrow arrow) {
            arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        }
        return projectile;
    }

    @Override
    protected InteractionResult mobInteract(
            Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS) && readyForShearing()) {
            if (level() instanceof ServerLevel world) {
                shear(SoundSource.PLAYERS);
                gameEvent(GameEvent.SHEAR, player);
                stack.hurtAndBreak(1, player,
                        hand == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void shear(SoundSource source) {
        level().playSound(
                null, this, SoundEvents.BOGGED_SHEAR, source, 1.0F, 1.0F);
        setSheared(true);
    }

    @Override
    public boolean readyForShearing() {
        return !isSheared();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BOGGED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BOGGED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BOGGED_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.BOGGED_STEP, 0.15F, 1.0F);
    }

    private static final class BoggedKillWithArrowGoal
            extends EntityAIKillWithArrow<LivingEntity> {
        private final IMBoggedEntity bogged;

        private BoggedKillWithArrowGoal(IMBoggedEntity bogged) {
            super(bogged, LivingEntity.class, 70, 16.0F);
            this.bogged = bogged;
        }

        @Override
        protected int getAttackDelay() {
            return bogged.level().getDifficulty().getId() >= 3 ? 50 : 70;
        }
    }
}
