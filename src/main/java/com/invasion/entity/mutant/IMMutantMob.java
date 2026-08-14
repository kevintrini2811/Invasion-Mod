package com.invasion.entity.mutant;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.ai.goal.MutantAttackNexusGoal;
import com.invasion.entity.ai.goal.MutantGoToNexusGoal;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.world.entity.PathfinderMob;

public interface IMMutantMob
        extends Combatant<PathfinderMob>, EntityConstruct.BuildableMob {
    @Override
    default PathfinderMob asEntity() {
        return (PathfinderMob) this;
    }

    @Override
    default String getLegacyName() {
        return getClass().getSimpleName();
    }

    @Override
    default void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
    }

    default void addNexusGoals() {
        PathfinderMob mob = asEntity();
        mob.goalSelector.addGoal(4, new MutantGoToNexusGoal(mob, this));
        mob.goalSelector.addGoal(3, new MutantAttackNexusGoal(mob, this));
    }

    IHasNexus.Handle getNexusHandle();
}
