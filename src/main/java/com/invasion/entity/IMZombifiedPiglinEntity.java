package com.invasion.entity;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.pathfinding.IMMobNavigation;
import com.invasion.item.InvItems;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.nbt.CompoundTag;

/**
 * Nexus-aware zombified piglin retaining vanilla anger propagation, water
 * immunity, equipment, baby dimensions, sounds and spawn behaviour.
 */
public final class IMZombifiedPiglinEntity extends ZombifiedPiglin
        implements NexusEntity, Miner {
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(
                    IMZombifiedPiglinEntity.class,
                    EntityDataSerializers.INT);
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMZombifiedPiglinEntity(
            EntityType<? extends ZombifiedPiglin> type, Level level) {
        super(type, level);
        setCanPickUpLoot(true);
        getNavigatorNew().setCanDestroyBlocks(true);
        applyTierAttributes();
        resetHealth();
    }

    public static AttributeSupplier.Builder createIMAttributes() {
        return ZombifiedPiglin.createAttributes();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new IMMobNavigation(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TIER, 1);
    }

    public int getTier() {
        return entityData.get(TIER);
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        entityData.set(TIER, net.minecraft.util.Mth.clamp(spawnConditions.tier(), 1, 2));
        applyTierAttributes();
        if (getTier() == 2) {
            equipTierTwoArmor();
        }
        resetHealth();
    }

    private void applyTierAttributes() {
        boolean tierTwo = getTier() == 2;
        getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                .setBaseValue(tierTwo ? 0.35D : 0.25D);
        getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                .setBaseValue(tierTwo ? 12D : 8D);
    }

    private void equipTierTwoArmor() {
        equipGoldArmor(EquipmentSlot.CHEST, Items.GOLDEN_CHESTPLATE);
        equipGoldArmor(EquipmentSlot.LEGS, Items.GOLDEN_LEGGINGS);
        equipGoldArmor(EquipmentSlot.FEET, Items.GOLDEN_BOOTS);
    }

    private void equipGoldArmor(
            EquipmentSlot slot, net.minecraft.world.item.Item item) {
        if (getItemBySlot(slot).isEmpty() && random.nextInt(5) == 1) {
            setItemSlot(slot, item.getDefaultInstance());
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new AttackNexusGoal<>(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        goalSelector.addGoal(5, new GoToNexusGoal(this));

        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(
                this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(
                this, AbstractVillager.class, this::getAggroRange, true));
        targetSelector.addGoal(3, new CustomRangeActiveTargetGoal<>(
                this, AbstractGolem.class, this::getAggroRange, true));
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
    public boolean wantsToPickUp(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        if (EquipmentUtil.isMeleeWeapon(stack)) {
            return canReplaceCurrentItem(
                    stack, getItemBySlot(EquipmentSlot.MAINHAND));
        }
        return slot.isArmor()
                && slot != EquipmentSlot.HEAD
                && stack.canEquip(slot, this)
                && canReplaceCurrentItem(
                        stack, getItemBySlot(slot));
    }

    @Override
    protected void customServerAiStep() {
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

    @Override
    public boolean checkSpawnObstruction(LevelReader world) {
        // SpawnPoint performs the complete block/entity collision check after
        // positioning. Vanilla's additional entity-obstruction test rejects
        // crowded invasion batches before that authoritative check can run.
        return !world.containsAnyLiquid(getBoundingBox());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putInt("tier", getTier());
        nexus.writeNbt(output);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        entityData.set(TIER, net.minecraft.util.Mth.clamp(
                input.contains("tier") ? input.getInt("tier") : 1, 1, 2));
        applyTierAttributes();
        setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        nexus.readNbt(input);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !hasNexus();
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean causedByPlayer) {
        super.dropCustomDeathLoot(source, looting, causedByPlayer);
        VanillaLoot.drop(this, EntityType.ZOMBIFIED_PIGLIN, source,
                causedByPlayer ? lastHurtByPlayer : null);
        if (random.nextInt(4) == 0) {
            spawnAtLocation(InvItems.SMALL_REMNANTS);
        }
    }
}
