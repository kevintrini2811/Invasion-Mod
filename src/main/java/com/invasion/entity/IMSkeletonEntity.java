package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.InvasionMod;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class IMSkeletonEntity extends IMMobEntity
        implements RangedAttackMob, RangedNexusAttacker, Miner {
    private static final EntityDataAccessor<Boolean> BABY =
            SynchedEntityData.defineId(
                    IMSkeletonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.world.entity.ai.attributes.AttributeModifier
            BABY_SPEED_BONUS = AttributeUtil.addPercentage(
                    InvasionMod.id("baby_skeleton_speed"), 50);
    private int switchWeaponCooldown;

    public IMSkeletonEntity(
            EntityType<? extends IMSkeletonEntity> type, Level world) {
        super(type, world);
        setItemInHand(InteractionHand.MAIN_HAND, Items.BOW.getDefaultInstance());
        setCanPickUpLoot(true);
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    public static AttributeSupplier.Builder createIMSkeletonAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.21);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BABY, false);
    }

    @Override
    public boolean isBaby() {
        return entityData.get(BABY);
    }

    @Override
    public void setBaby(boolean baby) {
        baby = baby
                && com.invasion.compat.TinySkeletonsCompatibility.isLoaded();
        entityData.set(BABY, baby);
        if (!level().isClientSide()) {
            AttributeUtil.toggleAttribute(
                    this, Attributes.MOVEMENT_SPEED,
                    BABY_SPEED_BONUS, baby);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == BABY) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        return isBaby()
                ? dimensions.scale(0.5F).withEyeHeight(
                        dimensions.eyeHeight() * 0.534F)
                : dimensions;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsBaby", isBaby());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setBaby(input.getBooleanOr("IsBaby", false)
                || input.getBooleanOr("isBaby", false));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new PredicatedGoal(
                new EntityAIKillWithArrow<>(
                        this, LivingEntity.class, 65, 16F),
                () -> !isBaby() || getMainHandItem().is(Items.BOW)));
        goalSelector.addGoal(1, new PredicatedGoal(
                new EntityAIKillWithArrow<>(
                        this, LivingEntity.class, 30, 15F),
                () -> isBaby() && usesThrownItems()));
        goalSelector.addGoal(1, new PredicatedGoal(
                new MobMeleeAttackGoal(this, 1.2D, false),
                () -> isBaby() && getType() == InvEntities.SKELETON
                        && getMainHandItem().is(Items.WOODEN_SWORD)));
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
    public float getDiggingSpeedMultiplier() {
        return 0.75F;
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
        if (isBaby() && usesThrownItems()) {
            throwItemAt(
                    target.position().add(0.0, target.getEyeHeight(), 0.0),
                    true);
            return;
        }
        ItemStack bow = getMainHandItem();
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
        updateBabySkeletonWeapon();
        if (!ItemSearchScheduler.shouldSearch(this)) {
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
        if (isBaby() && usesThrownItems()) {
            throwItemAt(target, false);
            return;
        }
        SkeletonArrowEntity projectile = new SkeletonArrowEntity(level(), this, getMainHandItem());
        double dX = target.x - getX();
        double dY = target.y - projectile.getY();
        double dZ = target.z - getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        projectile.shoot(dX, dY + horizontalDistance * 0.2F, dZ, 1.1F, 12);
        playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }

    public void initializeTinySkeletonAbilities() {
        if (getType() == InvEntities.SKELETON) {
            setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }
    }

    protected boolean usesThrownItems() {
        return getMainHandItem().is(Items.SNOWBALL)
                || getMainHandItem().is(Items.BROWN_MUSHROOM)
                || getMainHandItem().is(Items.RED_MUSHROOM)
                || getMainHandItem().is(Items.SAND);
    }

    private void updateBabySkeletonWeapon() {
        if (!isBaby() || getType() != InvEntities.SKELETON) {
            return;
        }
        if (switchWeaponCooldown > 0) {
            switchWeaponCooldown--;
        }
        LivingEntity target = getTarget();
        if (switchWeaponCooldown == 0 && target != null
                && distanceToSqr(target) < 16.0D
                && getMainHandItem().is(Items.BOW)) {
            swapHandItems();
        } else if (switchWeaponCooldown == 0
                && (target == null || distanceToSqr(target) > 36.0D)
                && getMainHandItem().is(Items.WOODEN_SWORD)) {
            swapHandItems();
        }
    }

    private void swapHandItems() {
        ItemStack mainHand = getMainHandItem();
        setItemInHand(InteractionHand.MAIN_HAND, getOffhandItem());
        setItemInHand(InteractionHand.OFF_HAND, mainHand);
        switchWeaponCooldown = 60;
    }

    private void throwItemAt(
            net.minecraft.world.phys.Vec3 target, boolean aimBelowEyes) {
        ItemStack item = getMainHandItem();
        if (item.isEmpty() || !(level() instanceof ServerLevel world)) {
            return;
        }
        double x = target.x - getX();
        double y = target.y - (aimBelowEyes ? 1.1F : 0.0F);
        double z = target.z - getZ();
        double arc = Math.sqrt(x * x + z * z) * 0.2F;
        Projectile.spawnProjectile(
                new IMThrownItemEntity(world, this, item), world, item,
                projectile -> projectile.shoot(
                        x, y + arc - projectile.getY(), z, 1.6F,
                        14.0F - world.getDifficulty().getId() * 2.0F));
        playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F,
                0.4F / (getRandom().nextFloat() * 0.4F + 0.8F));
        swing(InteractionHand.MAIN_HAND);
    }
}
