package com.invasion.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.invasion.compat.WildfireNexusGoal;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@Pseudo
@Mixin(targets = "com.faboslav.friendsandfoes.common.entity.WildfireEntity")
public abstract class WildfireNexusMixin extends Monster
        implements Combatant<Monster>, EntityConstruct.BuildableMob {
    @Unique private IHasNexus.Handle invmod$nexus;

    protected WildfireNexusMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void invmod$registerNexusGoal(CallbackInfo ci) {
        goalSelector.addGoal(0, new WildfireNexusGoal(this, this));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void invmod$writeNexus(CompoundTag output, CallbackInfo ci) {
        getNexusHandle().writeNbt(output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void invmod$readNexus(CompoundTag input, CallbackInfo ci) {
        getNexusHandle().readNbt(input);
    }

    @Override public IHasNexus.Handle getNexusHandle() {
        if (invmod$nexus == null) invmod$nexus = new IHasNexus.Handle(this::level);
        return invmod$nexus;
    }
    @Override public Monster asEntity() { return this; }
    @Override public String getLegacyName() { return "IMWildfire-T1"; }
    @Override public void onSpawned(@Nullable NexusAccess nexus,
            EntityConstruct spawnConditions) { setNexus(nexus); }
    @Override public boolean removeWhenFarAway(double distanceSquared) {
        return !hasNexus() && super.removeWhenFarAway(distanceSquared);
    }
    @Override public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }
}
