package com.invasion.entity;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
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
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A brute-sized zombie which consumes nearby IM mobs to grow stronger. */
public final class IMFatZombieEntity extends EntityIMZombie {
    private static final int EAT_DURATION = 32;
    private static final int EAT_COOLDOWN = 200;
    private static final float GROWTH_PER_MEAL = 0.01F;
    private static final EntityDataAccessor<Boolean> EATING =
            SynchedEntityData.defineId(
                    IMFatZombieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MEALS =
            SynchedEntityData.defineId(
                    IMFatZombieEntity.class, EntityDataSerializers.INT);

    private int eatingTicks;
    private int eatCooldown;
    private float pendingHealth;

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
                new MobMeleeAttackGoal(this, 1.3D, false), () -> !isEating()));
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
                (wolf, level) -> wolf instanceof Wolf tame && tame.isTame()));
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
                playSound(SoundEvents.GENERIC_EAT.value(), 1.0F,
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
        LivingEntity meal = level().getEntitiesOfClass(
                        EntityIMLiving.class, getBoundingBox().inflate(2.0D),
                        mob -> mob != this && mob.isAlive() && !mob.isRemoved())
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (meal == null || distanceTo(meal) > 2.0F) {
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
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                getAttribute(Attributes.MAX_HEALTH).getBaseValue()
                        + pendingHealth);
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
    public boolean hurtServer(
            ServerLevel level, DamageSource source, float amount) {
        return !isEating() && super.hurtServer(level, source, amount);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("EatingTicks", eatingTicks);
        output.putInt("EatCooldown", eatCooldown);
        output.putInt("Meals", getMeals());
        output.putFloat("PendingHealth", pendingHealth);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        eatingTicks = input.getIntOr("EatingTicks", 0);
        eatCooldown = input.getIntOr("EatCooldown", 0);
        entityData.set(MEALS, input.getIntOr("Meals", 0));
        entityData.set(EATING, eatingTicks > 0);
        pendingHealth = input.getFloatOr("PendingHealth", 0.0F);
        setAttackStrength(18.0D + getMeals());
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == MEALS) {
            refreshDimensions();
        }
    }
}
