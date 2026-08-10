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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class IMWitherSkeletonEntity extends IMSkeletonEntity {
    private static final EntityDataAccessor<Boolean> GROUP_LEADER_WAITING =
            SynchedEntityData.defineId(
                    IMWitherSkeletonEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DANCING =
            SynchedEntityData.defineId(
                    IMWitherSkeletonEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private int dancingTicks;

    public IMWitherSkeletonEntity(
            EntityType<? extends IMSkeletonEntity> type, Level world) {
        super(type, world);
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GROUP_LEADER_WAITING, false);
        entityData.define(DANCING, false);
    }

    public boolean isGroupLeaderWaiting() {
        return entityData.get(GROUP_LEADER_WAITING);
    }

    public void setGroupLeaderWaiting(boolean waiting) {
        entityData.set(GROUP_LEADER_WAITING, waiting);
    }

    public boolean isHoldingRangedWeapon() {
        return EquipmentUtil.isRangedWeapon(getMainHandItem());
    }

    public boolean isDancing() {
        return entityData.get(DANCING);
    }

    @Override
    public float getDiggingSpeedMultiplier() {
        return 1.0F;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
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
        goalSelector.addGoal(3, new PredicatedGoal(
                new AvoidEntityGoal<>(
                        this, Player.class, 6.0F, 1.0D, 1.2D),
                () -> isBaby() && isCarryingSkull()));
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

    @Override
    public void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (dancingTicks > 0 && --dancingTicks == 0) {
            entityData.set(DANCING, false);
        }
    }

    @Override
    public void initializeTinySkeletonAbilities() {
        if (getMainHandItem().is(Items.WITHER_SKELETON_SKULL)) {
            setGuaranteedDrop(EquipmentSlot.MAINHAND);
        } else if (getOffhandItem().is(Items.WITHER_SKELETON_SKULL)) {
            setGuaranteedDrop(EquipmentSlot.OFFHAND);
        }
    }

    @Override
    protected InteractionResult mobInteract(
            Player player, InteractionHand hand) {
        ItemStack offered = player.getItemInHand(hand);
        if (isBaby() && offered.is(Items.WITHER_ROSE)
                && isCarryingSkull()
                && level() instanceof ServerLevel world) {
            if (!player.getAbilities().instabuild) {
                offered.shrink(1);
            }
            ItemStack skull = getMainHandItem().is(
                    Items.WITHER_SKELETON_SKULL)
                    ? getMainHandItem() : getOffhandItem();
            spawnAtLocation(world, skull.copy());
            if (getMainHandItem().is(Items.WITHER_SKELETON_SKULL)) {
                setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            } else {
                setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            dancingTicks = 300;
            entityData.set(DANCING, true);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private boolean isCarryingSkull() {
        return (getMainHandItem().is(Items.WITHER_SKELETON_SKULL)
                        && getOffhandItem().isEmpty())
                || (getOffhandItem().is(Items.WITHER_SKELETON_SKULL)
                        && getMainHandItem().isEmpty());
    }
}
