package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class IMSkeletonEntity extends IMMobEntity implements RangedAttackMob, RangedNexusAttacker {
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        int arrows = getRandom().nextInt(3);
        for (int i = 0; i < arrows; i++) {
            spawnAtLocation(level, Items.ARROW);
        }
        if (getRandom().nextInt(3) == 2) {
            spawnAtLocation(level, Items.BONE);
        }
    }

    public IMSkeletonEntity(EntityType<IMSkeletonEntity> type, Level world) {
        super(type, world);
        setItemInHand(InteractionHand.MAIN_HAND, Items.BOW.getDefaultInstance());
        setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createIMSkeletonAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.21);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new EntityAIKillWithArrow<>(this, Player.class, 65, 16F));
        goalSelector.addGoal(2, new SkeletonAttackNexusGoal<>(this));
        // goalSelector.add(1, new EntityAIRallyBehindEntity(this, EntityIMCreeper.class, 4.0F));
        goalSelector.addGoal(3, new AttackNexusGoal<>(this));
        goalSelector.addGoal(4, new GoToNexusGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));

        targetSelector.addGoal(0, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(world, difficulty, spawnReason, data);
        setItemInHand(InteractionHand.MAIN_HAND, Items.BOW.getDefaultInstance());
        return data;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack bow = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
        ItemStack arrow = getProjectile(bow);
        AbstractArrow projectile = createArrowProjectile(arrow, pullProgress, bow);
        double dX = target.getX() - getX();
        double dY = target.getY(0.3333333333333333) - projectile.getY();
        double dZ = target.getZ() - getZ();
        double horLength = Math.sqrt(dX * dX + dZ * dZ);
        projectile.shoot(dX, dY + horLength * 0.2F, dZ, 1.1F, 12);
        playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }

    protected AbstractArrow createArrowProjectile(ItemStack arrow, float damageModifier, @Nullable ItemStack shotFrom) {
        return ProjectileUtil.getMobArrow(this, arrow, damageModifier, shotFrom);
    }

    @Override
    public boolean wantsToPickUp(ServerLevel world, ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot.isArmor()
                && isEquippableInSlot(stack, slot)
                && canReplaceCurrentItem(stack, getItemBySlot(slot), slot);
    }

    @Override
    public void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (tickCount % 5 != 0) {
            return;
        }
        for (ItemEntity item : world.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(1.25D),
                candidate -> !candidate.hasPickUpDelay()
                        && wantsToPickUp(world, candidate.getItem()))) {
            pickUpItem(world, item);
        }
    }

    public void performRangedNexusAttack(net.minecraft.world.phys.Vec3 target) {
        SkeletonArrowEntity projectile = new SkeletonArrowEntity(level(), this, getMainHandItem());
        double dX = target.x - getX();
        double dY = target.y - projectile.getY();
        double dZ = target.z - getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        projectile.shoot(dX, dY + horizontalDistance * 0.2F, dZ, 1.1F, 12);
        playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }
}
