package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.Notifiable;
import com.invasion.InvasionMod;
import com.invasion.entity.pathfinding.Navigation;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.EntityConstruct.BuildableMob;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.pathfinder.Path;

public interface NexusEntity extends IHasNexus, BuildableMob, HasAiGoals, EntityAccess, Combatant<PathfinderMob> {
    @Deprecated
    static CustomData createVariant(int flavour, int tier) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("flavour", flavour);
        nbt.putInt("tier", tier);
        return CustomData.of(nbt);
    }
    float DEFAULT_AIR_RESISTANCE = 0.9995F;
    float DEFAULT_GROUND_FRICTION = 0.546F;
    float DEFAULT_BASE_MOVEMENT_SPEED = 0.26F;

    @SuppressWarnings("deprecation")
    default Navigation getNavigatorNew() {
        return asEntity().getNavigation() instanceof Navigation a ? a
                : asEntity().getNavigation() instanceof com.invasion.entity.pathfinding.PathNavigateAdapter b ? b.getNewNavigator()
                : null;
    }

    default int getAggroRange() {
        return hasNexus() ? getNexusBoundAggroRange() : InvasionMod.getConfig().nightMobSightRange;
    }

    default int getSenseRange() {
        return hasNexus() ? getNexusBoundSenseRange() : InvasionMod.getConfig().nightMobSenseRange;
    }

    default int getNexusBoundSenseRange() {
        return 6;
    }

    default int getNexusBoundAggroRange() {
        return 12;
    }

    default boolean getBurnsInDay() {
        return false;
    }

    default void setIsHoldingIntoLadder(boolean flag) {
        asEntity().setShiftKeyDown(flag);
    }

    @Override
    default void onSpawned(@Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        resetHealth();
    }

    default void onFollowingEntity(Entity entity) {
    }

    default void onPathSet() {
    }

    default boolean onPathBlocked(Path path, Notifiable asker) {
        return false;
    }

    default boolean handlePathAction(BlockPos pos, PathAction action, Notifiable asker) {
        return false;
    }

    @Deprecated
    default void setGravity(float acceleration) {
        asEntity().getAttribute(Attributes.GRAVITY).setBaseValue(acceleration);
    }

    default void setAttackStrength(double attackStrength) {
        asEntity().getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackStrength);
    }

    default void setBaseMovementSpeed(double speed) {
        asEntity().getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
    }

    default double getAttackStrength() {
        return asEntity().getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    default boolean getLightLevelBelow8() {
        BlockPos pos = asEntity().blockPosition();
        return asEntity().level().getBrightness(LightLayer.SKY, pos) <= asEntity().getRandom().nextInt(32)
            && asEntity().level().getBrightness(LightLayer.BLOCK, pos) <= asEntity().getRandom().nextInt(8);
    }

    @Override
    @Deprecated
    default String getLegacyName() {
        return String.format("%s-T1", getClass().getName().replace("Entity", ""));
    }

    @Override
    default Goal getAIGoal() {
        return getNavigatorNew().getAIGoal();
    }

    @Override
    default Goal getPrevAIGoal() {
        return getNavigatorNew().getPrevAIGoal();
    }

    @Override
    default Goal transitionAIGoal(Goal newGoal) {
        return getNavigatorNew().transitionAIGoal(newGoal);
    }

    @Deprecated
    default void setName(String name) {
    }
}
