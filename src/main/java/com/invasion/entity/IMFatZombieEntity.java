package com.invasion.entity;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

/** A brute-sized zombie which consumes nearby IM mobs to grow stronger. */
public final class IMFatZombieEntity extends EntityIMZombie {
    private static final int EAT_DURATION = 32;
    private static final int EAT_COOLDOWN = 200;
    private static final float BASE_EAT_RADIUS = 2.0F;
    private static final float GROWTH_PER_MEAL = 0.05F;
    private static final EntityDataAccessor<Boolean> EATING =
            SynchedEntityData.defineId(
                    IMFatZombieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MEALS =
            SynchedEntityData.defineId(
                    IMFatZombieEntity.class, EntityDataSerializers.INT);

    private int eatingTicks;
    private int eatCooldown;
    private float pendingHealth;
    private float consumedHealth;

    public IMFatZombieEntity(
            EntityType<? extends EntityIMZombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createTierT3V0Attributes();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EATING, false);
        builder.define(MEALS, 0);
    }

    @Override
    protected void initTieredAttributes() {
        setBaseMovementSpeed(0.17F);
        setAttackStrength(18.0D + getMeals());
        selfDamage = 4;
        maxSelfDamage = 20;
        flammability = 4;
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new PredicatedGoal(
                new MineBlockGoal(this), () -> !isEating()));
        goalSelector.addGoal(1, new PredicatedGoal(
                new AttackNexusGoal<>(this), () -> !isEating()));
        goalSelector.addGoal(2, new PredicatedGoal(
                new SkeletonAttackNexusGoal<>(this),
                () -> !isEating() && isHoldingRangedWeapon()));
        goalSelector.addGoal(6, new PredicatedGoal(
                new EntityAIKillWithArrow<>(
                        this, LivingEntity.class, 65, 16.0F),
                () -> !isEating() && isHoldingRangedWeapon()));
        goalSelector.addGoal(6, new PredicatedGoal(
                new MobMeleeAttackGoal(this, 1.3D, false),
                () -> !isEating() && !isHoldingRangedWeapon()));
        goalSelector.addGoal(5, new PredicatedGoal(
                new GoToNexusGoal(this), () -> !isEating()));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(
                this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(
                this, IronGolem.class, this::getAggroRange, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, Wolf.class, 10, true, false,
                wolf -> wolf instanceof Wolf tame && tame.isTame()));
    }

    @Override
    public boolean wantsToPickUp(net.minecraft.world.item.ItemStack stack) {
        return EquipmentUtil.isWeapon(stack)
                && !EquipmentUtil.isWeapon(getMainHandItem());
    }

    @Override
    public boolean canUseSlot(net.minecraft.world.entity.EquipmentSlot slot) {
        return !slot.isArmor() && super.canUseSlot(slot);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (eatCooldown > 0) {
            eatCooldown--;
        }
        if (eatingTicks > 0) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (eatingTicks % 4 == 0) {
                playSound(SoundEvents.GENERIC_EAT, 1.0F,
                        0.8F + getRandom().nextFloat() * 0.4F);
            }
            if (--eatingTicks == 0) {
                finishEating();
            }
        } else if (eatCooldown == 0) {
            findMeal();
        }
    }

    private void findMeal() {
        float eatRadius = getEatRadius();
        LivingEntity meal = level().getEntitiesOfClass(
                        EntityIMLiving.class,
                        getBoundingBox().inflate(eatRadius),
                        mob -> mob != this && mob.isAlive() && !mob.isRemoved())
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (meal == null || distanceTo(meal) > eatRadius) {
            return;
        }
        pendingHealth = meal.getHealth();
        meal.discard();
        eatingTicks = EAT_DURATION;
        entityData.set(EATING, true);
        eatCooldown = EAT_COOLDOWN;
        setTarget(null);
        getNavigation().stop();
    }

    private void finishEating() {
        entityData.set(MEALS, getMeals() + 1);
        entityData.set(EATING, false);
        setAttackStrength(18.0D + getMeals());
        consumedHealth += pendingHealth;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                getAttribute(Attributes.MAX_HEALTH).getBaseValue() + pendingHealth);
        setHealth(getHealth() + pendingHealth);
        pendingHealth = 0.0F;
        refreshDimensions();
        playSound(SoundEvents.PLAYER_BURP, 1.0F, 1.0F);
    }

    public boolean isEating() {
        return entityData.get(EATING);
    }

    public float getGrowthScale() {
        return 1.0F + getMeals() * GROWTH_PER_MEAL;
    }

    public float getEatRadius() {
        return BASE_EAT_RADIUS * getGrowthScale();
    }

    public float getEatAnimation(float partialTick) {
        return isEating() ? ((tickCount + partialTick) % 16.0F) / 16.0F : 0.0F;
    }

    private int getMeals() {
        return entityData.get(MEALS);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatableWithFallback(
                "entity.invmod.fat_zombie", "IM Fat Zombie");
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(getGrowthScale());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isEating() && super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putInt("EatingTicks", eatingTicks);
        output.putInt("EatCooldown", eatCooldown);
        output.putInt("Meals", getMeals());
        output.putFloat("PendingHealth", pendingHealth);
        output.putFloat("ConsumedHealth", consumedHealth);
        output.putFloat("FatZombieMaxHealth",
                (float)getAttribute(Attributes.MAX_HEALTH).getBaseValue());
        output.putFloat("FatZombieHealth", getHealth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        eatingTicks = input.getInt("EatingTicks");
        eatCooldown = input.getInt("EatCooldown");
        entityData.set(MEALS, input.getInt("Meals"));
        entityData.set(EATING, eatingTicks > 0);
        pendingHealth = input.getFloat("PendingHealth");
        consumedHealth = input.getFloat("ConsumedHealth");
        float savedMaxHealth = input.contains("FatZombieMaxHealth")
                ? input.getFloat("FatZombieMaxHealth") : getMaxHealth() + consumedHealth;
        float savedHealth = input.contains("FatZombieHealth")
                ? input.getFloat("FatZombieHealth") : getHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(savedMaxHealth);
        setAttackStrength(18.0D + getMeals());
        setHealth(Math.min(savedHealth, getMaxHealth()));
        clearArmor();
        refreshDimensions();
    }

    private void clearArmor() {
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.item.ItemStack.EMPTY);
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.item.ItemStack.EMPTY);
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.item.ItemStack.EMPTY);
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.item.ItemStack.EMPTY);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == MEALS) {
            refreshDimensions();
        }
    }
}
