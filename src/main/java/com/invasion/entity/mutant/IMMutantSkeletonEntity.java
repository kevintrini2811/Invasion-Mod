package com.invasion.entity.mutant;

import java.util.Optional;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.IHasNexus;
import fuzs.mutantmonsters.common.world.entity.mutant.MutantSkeleton;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

public final class IMMutantSkeletonEntity extends MutantSkeleton implements IMMutantMob {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    public IMMutantSkeletonEntity(EntityType<? extends MutantSkeleton> type, Level level) { super(type, level); }
    @Override protected void registerGoals() { super.registerGoals(); addNexusGoals(); }
    @Override public IHasNexus.Handle getNexusHandle() { return nexus; }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); nexus.writeNbt(out); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); nexus.readNbt(in); }
    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof Combatant<?>) && super.canAttack(target); }
    @Override public boolean removeWhenFarAway(double distance) { return !hasNexus() && super.removeWhenFarAway(distance); }
}
