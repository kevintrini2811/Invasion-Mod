package com.invasion.entity.mutant;

import com.invasion.nexus.Combatant;
import com.invasion.entity.StationaryPathRecoveryExcluded;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import fuzs.mutantmonsters.common.world.entity.CreeperMinion;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class IMCreeperMinionEntity extends CreeperMinion
        implements IMMutantMob, StationaryPathRecoveryExcluded {
    private static final int MAX_STATIONARY_TICKS = 20 * 10;
    private static final int SPAWN_IGNITION_GRACE_TICKS = 40;
    private static final double STATIONARY_TOLERANCE_SQR = 0.2D * 0.2D;
    private static final int NEXUS_EXPLOSION_DAMAGE = 5;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private Vec3 stationaryAnchor;
    private int stationaryTicks;
    private int ignitionGraceTicks = SPAWN_IGNITION_GRACE_TICKS;

    public IMCreeperMinionEntity(
            EntityType<? extends CreeperMinion> type, Level level) {
        super(type, level);
    }

    @Override protected void registerGoals() { super.registerGoals(); addNexusGoals(); }
    @Override public IHasNexus.Handle getNexusHandle() { return nexus; }
    @Override public void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); nexus.writeNbt(out); }
    @Override public void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); nexus.readNbt(in); }
    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof Combatant<?>) && super.canAttack(target); }
    @Override public boolean removeWhenFarAway(double distance) { return !hasNexus() && super.removeWhenFarAway(distance); }

    @Override
    public void setExplodeState(int state) {
        if (state > 0 && ignitionGraceTicks > 0 && !hasIgnited()) {
            return;
        }
        super.setExplodeState(state);
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && isAlive()) {
            if (ignitionGraceTicks > 0) {
                ignitionGraceTicks--;
            }
            tickStationaryFuse();
        }

        int previousExplodeState = getExplodeState();
        super.tick();

        if (!level().isClientSide()
                && previousExplodeState > 0
                && getExplodeState() < 0
                && hasNexus()) {
            NexusAccess activeNexus = getNexus();
            double damageRange = (getExplosionRadius()
                    + (isCharged() ? 2.0D : 0.0D)) * 2.0D;
            if (findDistanceToNexus() <= damageRange) {
                activeNexus.damage(damageSources().explosion(this, this),
                        NEXUS_EXPLOSION_DAMAGE * (isCharged() ? 2 : 1));
            }
            if (!isAlive()) {
                activeNexus.notifyCombatantRemoved(
                        this, Entity.RemovalReason.KILLED);
                setNexus(null);
            }
        }
    }

    private void tickStationaryFuse() {
        if (hasIgnited() || getExplodeState() > 0) {
            return;
        }
        if (stationaryAnchor == null) {
            stationaryAnchor = position();
            return;
        }
        if (position().distanceToSqr(stationaryAnchor)
                > STATIONARY_TOLERANCE_SQR) {
            stationaryAnchor = position();
            stationaryTicks = 0;
            return;
        }
        if (++stationaryTicks >= MAX_STATIONARY_TICKS) {
            ignite();
        }
    }

    @Override
    public int performNexusAttack(ServerLevel level, NexusAccess nexus) {
        if (ignitionGraceTicks > 0) {
            return ignitionGraceTicks;
        }
        ignite();
        return canExplodeContinuously() ? 60 : 100;
    }
}
