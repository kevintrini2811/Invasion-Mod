package com.invasion.entity;

import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class IMParchedEntity extends IMSkeletonEntity {
    public IMParchedEntity(
            EntityType<? extends IMSkeletonEntity> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IMSkeletonEntity.createIMSkeletonAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new PredicatedGoal(
                new ParchedKillWithArrowGoal(this),
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
            arrow.addEffect(
                    new MobEffectInstance(MobEffects.WEAKNESS, 600));
        }
        return projectile;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WEAKNESS)
                && super.canBeAffected(effect);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARCHED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARCHED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARCHED_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.PARCHED_STEP, 0.15F, 1.0F);
    }

    private static final class ParchedKillWithArrowGoal
            extends EntityAIKillWithArrow<LivingEntity> {
        private final IMParchedEntity parched;

        private ParchedKillWithArrowGoal(IMParchedEntity parched) {
            super(parched, LivingEntity.class, 70, 16.0F);
            this.parched = parched;
        }

        @Override
        protected int getAttackDelay() {
            return parched.level().getDifficulty().getId() >= 3 ? 50 : 70;
        }
    }
}
