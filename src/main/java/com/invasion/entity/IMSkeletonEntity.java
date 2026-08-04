package com.invasion.entity;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
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
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class IMSkeletonEntity extends IMMobEntity
        implements RangedAttackMob, RangedNexusAttacker, Miner {
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean causedByPlayer) {
        super.dropCustomDeathLoot(source, looting, causedByPlayer);
        int arrows = random.nextInt(3);
        for (int i = 0; i < arrows; i++) {
            spawnAtLocation(Items.ARROW);
        }
        if (random.nextInt(3) == 2) {
            spawnAtLocation(Items.BONE);
        }
    }

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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new EntityAIKillWithArrow<>(
                this, LivingEntity.class, 65, 16F));
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
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty,
            MobSpawnType spawnReason, @Nullable SpawnGroupData data,
            @Nullable CompoundTag entityTag) {
        data = super.finalizeSpawn(world, difficulty, spawnReason, data, entityTag);
        setItemInHand(InteractionHand.MAIN_HAND, Items.BOW.getDefaultInstance());
        return data;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack bow = getMainHandItem();
        ItemStack arrow = getProjectile(bow);
        AbstractArrow projectile = createArrowProjectile(arrow, pullProgress, bow);
        double dX = target.getX() - getX();
        double dY = target.getY(0.3333333333333333) - projectile.getY();
        double dZ = target.getZ() - getZ();
        double horLength = Math.sqrt(dX * dX + dZ * dZ);
        projectile.shoot(dX, dY + horLength * 0.2F, dZ, 1.1F, 12);
        playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (random.nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }

    protected AbstractArrow createArrowProjectile(ItemStack arrow, float damageModifier, @Nullable ItemStack shotFrom) {
        return ProjectileUtil.getMobArrow(this, arrow, damageModifier);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot.isArmor()
                && stack.canEquip(slot, this)
                && canReplaceCurrentItem(stack, getItemBySlot(slot));
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel world = (ServerLevel) level();
        if (!ItemSearchScheduler.shouldSearch(this)) {
            return;
        }
        for (ItemEntity item : world.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(1.25D),
                candidate -> !candidate.hasPickUpDelay()
                        && wantsToPickUp(candidate.getItem()))) {
            pickUpItem(item);
        }
    }

    public void performRangedNexusAttack(net.minecraft.world.phys.Vec3 target) {
        SkeletonArrowEntity projectile = new SkeletonArrowEntity(level(), this, getMainHandItem());
        double dX = target.x - getX();
        double dY = target.y - projectile.getY();
        double dZ = target.z - getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        projectile.shoot(dX, dY + horizontalDistance * 0.2F, dZ, 1.1F, 12);
        playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (random.nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }
}
