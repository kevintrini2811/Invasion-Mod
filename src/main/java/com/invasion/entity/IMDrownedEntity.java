package com.invasion.entity;

import com.invasion.entity.ai.goal.PredicatedGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class IMDrownedEntity extends EntityIMZombie
        implements RangedAttackMob {
    public IMDrownedEntity(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
        moveControl = new SmoothSwimmingMoveControl<>(
                this, 85, 10, 0.02F, 0.1F, true);
        setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityIMZombie.createTierT1V0Attributes()
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new PredicatedGoal(
                new RangedAttackGoal(this, 1.0D, 40, 10.0F),
                () -> getMainHandItem().is(Items.TRIDENT)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor world, DifficultyInstance difficulty,
            EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(world, difficulty, reason, data);
        RandomSource random = world.getRandom();
        if (getMainHandItem().isEmpty() && random.nextFloat() > 0.9F) {
            setItemSlot(
                    EquipmentSlot.MAINHAND,
                    new ItemStack(random.nextInt(16) < 10
                            ? Items.TRIDENT : Items.FISHING_ROD));
        }
        if (getOffhandItem().isEmpty() && random.nextFloat() < 0.03F) {
            setItemSlot(
                    EquipmentSlot.OFFHAND,
                    new ItemStack(Items.NAUTILUS_SHELL));
            setGuaranteedDrop(EquipmentSlot.OFFHAND);
        }
        return data;
    }

    @Override
    public void performRangedAttack(
            LivingEntity target, float pullProgress) {
        ItemStack held = getMainHandItem();
        ItemStack tridentStack = held.is(Items.TRIDENT)
                ? held : new ItemStack(Items.TRIDENT);
        ThrownTrident trident = new ThrownTrident(
                level(), this, tridentStack);
        double x = target.getX() - getX();
        double y = target.getY(0.3333333333333333D) - trident.getY();
        double z = target.getZ() - getZ();
        double horizontal = Math.sqrt(x * x + z * z);
        if (level() instanceof ServerLevel world) {
            Projectile.spawnProjectileUsingShoot(
                    trident, world, tridentStack,
                    x, y + horizontal * 0.2D, z,
                    1.6F, 14 - level().getDifficulty().getId() * 4);
        }
        playSound(
                SoundEvents.DROWNED_SHOOT, 1.0F,
                1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
    }

    public boolean wantsToSwim() {
        LivingEntity target = getTarget();
        return hasNexus()
                || target != null && target.isInWater();
    }

    @Override
    protected void travelInWater(
            Vec3 movementInput, double gravity,
            boolean falling, double y) {
        if (isUnderWater() && wantsToSwim()) {
            moveRelative(0.01F, movementInput);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9D));
        } else {
            super.travelInWater(movementInput, gravity, falling, y);
        }
    }

    @Override
    public void updateSwimming() {
        if (!level().isClientSide()) {
            setSwimming(isEffectiveAi()
                    && isUnderWater() && wantsToSwim());
        }
    }

    @Override
    public boolean isVisuallySwimming() {
        return isSwimming() && !isPassenger();
    }

    @Override
    public boolean isPushedByFluid() {
        return !isSwimming();
    }

    @Override
    public SoundEvent getAmbientSound() {
        return isInWater()
                ? SoundEvents.DROWNED_AMBIENT_WATER
                : SoundEvents.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(
            net.minecraft.world.damagesource.DamageSource source) {
        return isInWater()
                ? SoundEvents.DROWNED_HURT_WATER
                : SoundEvents.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isInWater()
                ? SoundEvents.DROWNED_DEATH_WATER
                : SoundEvents.DROWNED_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.DROWNED_STEP, 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DROWNED_SWIM;
    }

    @Override
    public TagKey<Item> getPreferredWeaponType() {
        return ItemTags.DROWNED_PREFERRED_WEAPONS;
    }
}
