package com.invasion.entity;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import com.invasion.entity.ai.goal.WitherSkeletonGroupGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class IMWitherSkeletonEntity extends IMSkeletonEntity {
    public IMWitherSkeletonEntity(
            EntityType<? extends IMSkeletonEntity> type, Level world) {
        super(type, world);
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    public boolean isHoldingRangedWeapon() {
        return EquipmentUtil.isRangedWeapon(getMainHandItem());
    }

    @Override
    public float getDiggingSpeedMultiplier() {
        return 1.0F;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(
                    new MobEffectInstance(MobEffects.WITHER, 200), this);
        }
        return hit;
    }

    @Override
    protected AbstractArrow createArrowProjectile(
            ItemStack arrowStack, float damageModifier,
            ItemStack shotFrom) {
        AbstractArrow projectile = super.createArrowProjectile(
                arrowStack, damageModifier, shotFrom);
        if (projectile instanceof Arrow arrow) {
            arrow.addEffect(
                    new MobEffectInstance(MobEffects.WITHER, 200));
        }
        return projectile;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new WitherSkeletonGroupGoal(this));
        goalSelector.addGoal(2, new PredicatedGoal(
                new SkeletonAttackNexusGoal<>(this),
                this::isHoldingRangedWeapon));
        goalSelector.addGoal(3, new AttackNexusGoal<>(this));
        goalSelector.addGoal(4, new GoToNexusGoal(this));
        goalSelector.addGoal(5, new PredicatedGoal(
                new EntityAIKillWithArrow<>(
                        this, LivingEntity.class, 65, 16F),
                this::isHoldingRangedWeapon));
        goalSelector.addGoal(5, new PredicatedGoal(
                new MobMeleeAttackGoal(this, 1.2D, false),
                () -> !isHoldingRangedWeapon()));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new CustomRangeActiveTargetGoal<>(
                this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }
}
