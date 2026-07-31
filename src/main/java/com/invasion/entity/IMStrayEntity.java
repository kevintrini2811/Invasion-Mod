package com.invasion.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class IMStrayEntity extends IMSkeletonEntity {
    public IMStrayEntity(
            EntityType<? extends IMSkeletonEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected AbstractArrow createArrowProjectile(
            ItemStack arrowStack, float damageModifier,
            ItemStack shotFrom) {
        AbstractArrow projectile = super.createArrowProjectile(
                arrowStack, damageModifier, shotFrom);
        if (projectile instanceof Arrow arrow) {
            arrow.addEffect(
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
        }
        return projectile;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRAY_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.STRAY_STEP, 0.15F, 1.0F);
    }
}
