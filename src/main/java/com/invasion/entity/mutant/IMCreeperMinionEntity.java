package com.invasion.entity.mutant;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import fuzs.mutantmonsters.world.entity.CreeperMinion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;


public final class IMCreeperMinionEntity extends CreeperMinion
        implements IMMutantMob {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMCreeperMinionEntity(
            EntityType<? extends CreeperMinion> type, Level level) {
        super(type, level);
    }

    @Override protected void registerGoals() { super.registerGoals(); addNexusGoals(); }
    @Override public IHasNexus.Handle getNexusHandle() { return nexus; }
    @Override public void addAdditionalSaveData(CompoundTag out) { super.addAdditionalSaveData(out); nexus.writeNbt(out); }
    @Override public void readAdditionalSaveData(CompoundTag in) { super.readAdditionalSaveData(in); nexus.readNbt(in); }
    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof Combatant<?>) && super.canAttack(target); }
    @Override public boolean removeWhenFarAway(double distance) { return !hasNexus() && super.removeWhenFarAway(distance); }
    @Override protected ResourceKey<LootTable> getDefaultLootTable() { return mutantLootTable(); }

    @Override
    public int performNexusAttack(ServerLevel level, NexusAccess nexus) {
        ignite();
        nexus.damage(damageSources().mobAttack(this), isCharged() ? 6 : 3);
        return canExplodeContinuously() ? 60 : 100;
    }
}
