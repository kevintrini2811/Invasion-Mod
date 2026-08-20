package com.invasion.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.invasion.compat.WildfireNexusGoal;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void invmod$preventFriendlyShieldRetaliation(
            ServerLevel level, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() instanceof Combatant<?>) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void invmod$writeNexus(ValueOutput output, CallbackInfo ci) {
        getNexusHandle().writeNbt(output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void invmod$readNexus(ValueInput input, CallbackInfo ci) {
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
