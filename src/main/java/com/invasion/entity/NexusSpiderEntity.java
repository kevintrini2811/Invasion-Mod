package com.invasion.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.item.InvItems;
import com.invasion.entity.ai.IMSpiderMoveControl;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.RallyBehindLeaderGoal;
import com.invasion.entity.ai.goal.ProvideSupportGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.target.RetaliateGoal;
import com.invasion.entity.pathfinding.IMMobNavigation;
import com.invasion.nexus.IHasNexus;
import com.invasion.particle.InvParticles;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AgeableMob.AgeableMobGroupData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class NexusSpiderEntity extends Spider
        implements NexusEntity, MountableEntity, Stunnable, Miner {
    private static final AttributeModifier BABY_SPEED_BONUS = AttributeUtil.addToBase(InvasionMod.id("baby_speed"), 0.05F);
    private static final AttributeModifier BABY_ATTACK_BONUS = AttributeUtil.addToBase(InvasionMod.id("baby_attack"), -2F);

    private static final List<Holder<Attribute>> GROWTH_SCALING_ATTRIBUTES = List.of(
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.KNOCKBACK_RESISTANCE
    );

    private static final EntityDataAccessor<Boolean> CHILD = SynchedEntityData.defineId(NexusSpiderEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int FULLY_GROWN_AGE = 0;
    public static final int MAX_TIME_TO_GROW = AgeableMob.BABY_START_AGE;

    protected int ticksToGrow = FULLY_GROWN_AGE;

    private int stunTime;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public NexusSpiderEntity(EntityType<? extends NexusSpiderEntity> type, Level world) {
        super(type, world);
        moveControl = new IMSpiderMoveControl(this);
        setCanPickUpLoot(
                com.invasion.compat.AsyncCompatibility.canUseVanillaItemPickup());
        getNavigatorNew().setCanDestroyBlocks(true);
        resetHealth();
    }

    @Override
    public boolean wantsToPickUp(ServerLevel world, ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot == EquipmentSlot.HEAD
                && isEquippableInSlot(stack, slot)
                && canReplaceCurrentItem(stack, getItemBySlot(slot), slot);
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (!ItemSearchScheduler.shouldSearch(this)) {
            return;
        }
        for (ItemEntity item : world.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(1.25D),
                candidate -> !candidate.hasPickUpDelay()
                        && wantsToPickUp(world, candidate.getItem()))) {
            com.invasion.compat.AsyncCompatibility.pickUpEquipment(
                    this, world, item);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.29F)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.GRAVITY, 0.08);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHILD, false);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new IMMobNavigation(this) {{
            setCanClimbLadders(true);
        }};
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new MobMeleeAttackGoal(this, 1.3F, false));
        goalSelector.addGoal(1, new RallyBehindLeaderGoal<>(this, IMCreeperEntity.class, 4));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(3, new ProvideSupportGoal(this, 5, false));
        initExtraGoals();
        goalSelector.addGoal(5, new GoToNexusGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));

        targetSelector.addGoal(0, new RetaliateGoal(this));
        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(4, new CustomRangeActiveTargetGoal<>(
                this, AbstractVillager.class, this::getAggroRange, true));
        //targetSelector.add(3, new NoNexusPathGoal(this, new CustomRangeActiveTargetGoal<>(this, PigmanEngineerEntity.class, 3.5F)));
        targetSelector.addGoal(4, new HurtByTargetGoal(this));
    }

    protected void initExtraGoals() {

    }

    @Override
    public float getDiggingSpeedMultiplier() {
        return 0.5F;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        if (isBaby()) {
            return;
        }
        if (getRandom().nextInt(4) == 0) {
            spawnAtLocation(level, InvItems.SMALL_REMNANTS);
        }
    }

    @Override
    public Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public PathfinderMob asEntity() {
        return this;
    }

    @Override
    public final float getAgeScale() {
        return 1;
    }

    public float scaleAmount() {
        return super.getScale() * getGlobalScaleMultiplier();
    }

    protected float getGlobalScaleMultiplier() {
        return isBaby() ? 0.33F : 1;
    }

    @Override
    public boolean stun(int maxTicks) {
        stunTime = Math.max(stunTime, maxTicks);
        return true;
    }

    @Override
    public boolean isStunned() {
        return stunTime > 0;
    }

    @Override
    protected float getJumpPower(float strength) {
        return super.getJumpPower(strength + 0.41F);
    }

    @Override
    public int getNexusBoundAggroRange() {
        return 2 + (isBaby() ? 0 : 8);
    }

    @Override
    public boolean isBaby() {
        return entityData.get(CHILD);
    }

    @Override
    public void setBaby(boolean baby) {
        boolean changed = baby != isBaby();
        entityData.set(CHILD, baby);
        ticksToGrow = baby ? MAX_TIME_TO_GROW : FULLY_GROWN_AGE;
        if (level() != null && !level().isClientSide()) {
            AttributeUtil.toggleAttribute(this, Attributes.MOVEMENT_SPEED, BABY_SPEED_BONUS, isBaby());
            AttributeUtil.toggleAttribute(this, GROWTH_SCALING_ATTRIBUTES, BABY_ATTACK_BONUS, isBaby());
            if (changed) {
                resetHealth();
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (CHILD.equals(data)) {
            refreshDimensions();
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    public void aiStep() {
        if (isStunned()) {
            if (!level().isClientSide() && tickCount % 10 == 0) {
                ((ServerLevel)level()).sendParticles(InvParticles.DAZE, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
            }
            stunTime--;
            return;
        }
        super.aiStep();
        if (!level().isClientSide() && isAlive() && isBaby() && ++ticksToGrow >= 0) {
            setBaby(false);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        if (entityData == null) {
            entityData = new NexusSpiderData(new AgeableMob.AgeableMobGroupData(true));

            if ((world.getDifficulty() == Difficulty.HARD && random.nextFloat() < 0.1F * difficulty.getSpecialMultiplier())
                    || (hasNexus() && getNexus().getProgressionLevel() > 5)) {
                ((NexusSpiderData)entityData).setRandomEffect(random);
            }
        }

        AgeableMob.AgeableMobGroupData passiveData = ((NexusSpiderData)entityData).passiveData;
        if (passiveData.isShouldSpawnBaby() && passiveData.getGroupSize() > 0 && world.getRandom().nextFloat() <= passiveData.getBabySpawnChance()) {
            setBaby(true);
        }

        passiveData.increaseGroupSizeByOne();

        super.finalizeSpawn(world, difficulty, spawnReason, entityData);

        if (hasNexus()) {
            AttributeUtil.applyNexusWaveComplications(this, world, getNexus().getProgressionLevel(), difficulty, spawnReason);
        }

        return entityData;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("isChild", isBaby());
        compound.putInt("ticksToGrow", ticksToGrow);
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        setBaby(compound.getBooleanOr("isChild", false));
        ticksToGrow = compound.getIntOr("ticksToGrow", 0);
    }

    public static class NexusSpiderData extends SpiderEffectsGroupData {
        private final AgeableMobGroupData passiveData;

        private NexusSpiderData(AgeableMobGroupData passiveData) {
            this.passiveData = passiveData;
        }
    }
}
