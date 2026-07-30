package com.invasion.entity;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.pathfinding.IMMobNavigation;
import com.invasion.item.InvItems;
import com.invasion.nexus.IHasNexus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Nexus-aware zombified piglin retaining vanilla anger propagation, water
 * immunity, equipment, baby dimensions, sounds and spawn behaviour.
 */
public final class IMZombifiedPiglinEntity extends ZombifiedPiglin
        implements NexusEntity {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMZombifiedPiglinEntity(
            EntityType<? extends ZombifiedPiglin> type, Level level) {
        super(type, level);
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
    protected void registerGoals() {
        super.registerGoals();
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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        nexus.writeNbt(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
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
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        net.minecraft.world.entity.EntityTypes.ZOMBIFIED_PIGLIN
                .getDefaultLootTable()
                .ifPresent(lootTable -> dropFromLootTable(
                        level, source, causedByPlayer, lootTable));
        if (getRandom().nextInt(4) == 0) {
            spawnAtLocation(level, InvItems.SMALL_REMNANTS);
        }
    }
}
