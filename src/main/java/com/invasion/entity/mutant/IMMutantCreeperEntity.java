package com.invasion.entity.mutant;

import java.util.Optional;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.IHasNexus;
import com.invasion.entity.ai.goal.MutantAttackNexusGoal;
import com.invasion.entity.ai.goal.MutantGoToNexusGoal;
import fuzs.mutantmonsters.common.world.entity.mutant.MutantCreeper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

public final class IMMutantCreeperEntity extends MutantCreeper implements IMMutantMob {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    public IMMutantCreeperEntity(EntityType<? extends MutantCreeper> type, Level level) { super(type, level); }
    @Override protected void registerGoals() { super.registerGoals(); goalSelector.addGoal(4, new MutantGoToNexusGoal(this, this)); goalSelector.addGoal(3, new MutantAttackNexusGoal(this, this)); }
    @Override public IHasNexus.Handle getNexusHandle() { return nexus; }
    @Override public void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); nexus.writeNbt(out); }
    @Override public void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); nexus.readNbt(in); }
    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof Combatant<?>) && super.canAttack(target); }
    @Override public boolean removeWhenFarAway(double distance) { return !hasNexus() && super.removeWhenFarAway(distance); }
}
