package com.invasion.entity;

import java.util.EnumSet;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A vanilla guardian that joins the active Nexus invasion. */
public final class IMGuardianEntity extends Guardian
        implements Combatant<Guardian>, EntityConstruct.BuildableMob {
    private static final EntityDataAccessor<Optional<BlockPos>> NEXUS_BEAM_TARGET =
            SynchedEntityData.defineId(
                    IMGuardianEntity.class,
                    EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> NEXUS_BEAM_START_TICK =
            SynchedEntityData.defineId(
                    IMGuardianEntity.class, EntityDataSerializers.INT);
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMGuardianEntity(EntityType<? extends Guardian> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Guardian.createAttributes();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NEXUS_BEAM_TARGET, Optional.empty());
        builder.define(NEXUS_BEAM_START_TICK, 0);
    }

    public Optional<BlockPos> getNexusBeamTarget() {
        return entityData.get(NEXUS_BEAM_TARGET);
    }

    public int getNexusBeamStartTick() {
        return entityData.get(NEXUS_BEAM_START_TICK);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(5, new AttackNexusGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                (candidate, level) -> level instanceof ServerLevel serverLevel
                        && IMWitchEntity.isPlayerAlly(candidate, serverLevel)));
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Guardian asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMGuardian-T1";
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
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
        return !hasNexus() && super.removeWhenFarAway(distanceSquared);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }

    private final class AttackNexusGoal extends Goal {
        private AttackNexusGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && hasNexus() && getNexus().isActive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            BlockPos pos = getNexus().getOrigin();
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D;
            if (distanceToSqr(x, y, z) <= 4.0D) {
                getNavigation().stop();
                getLookControl().setLookAt(x, y, z);
                if (getNexusBeamTarget().isEmpty()) {
                    entityData.set(NEXUS_BEAM_TARGET, Optional.of(pos));
                    entityData.set(NEXUS_BEAM_START_TICK, tickCount);
                    playSound(SoundEvents.GUARDIAN_ATTACK, 1.0F, 1.0F);
                } else if (tickCount - getNexusBeamStartTick()
                        >= getAttackDuration()) {
                    getNexus().damage(damageSources().mobAttack(
                            IMGuardianEntity.this), 2);
                    clearNexusBeam();
                }
            } else {
                clearNexusBeam();
                if (getNavigation().isDone()) {
                    getNavigation().moveTo(x, y, z, 1.0D);
                }
            }
        }

        @Override
        public void stop() {
            clearNexusBeam();
        }

        private void clearNexusBeam() {
            entityData.set(NEXUS_BEAM_TARGET, Optional.empty());
        }
    }
}
