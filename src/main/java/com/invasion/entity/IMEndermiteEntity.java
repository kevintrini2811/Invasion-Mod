package com.invasion.entity;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** Support attacker that displaces players and their allies instead of damaging them. */
public final class IMEndermiteEntity extends Endermite
        implements Combatant<Endermite>, EntityConstruct.BuildableMob {
    private static final double TELEPORT_RADIUS = 50.0D;
    private static final int TELEPORT_ATTEMPTS = 16;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMEndermiteEntity(EntityType<? extends Endermite> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.removeAllGoals(
                goal -> goal instanceof NearestAttackableTargetGoal<?>);
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                (candidate, level) -> level instanceof ServerLevel serverLevel
                        && IMWitchEntity.isPlayerAlly(candidate, serverLevel)));
    }

    @Override
    public boolean canAttack(LivingEntity candidate) {
        return level() instanceof ServerLevel serverLevel
                && (candidate == getLastHurtByMob()
                        || IMWitchEntity.isPlayerAlly(candidate, serverLevel))
                && candidate.isAlive()
                && candidate.canBeSeenAsEnemy()
                && candidate.attackable()
                && !isAlliedTo(candidate)
                && candidate != this;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity living) || !canAttack(living)) {
            return false;
        }
        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.TAU;
            double distance = Math.sqrt(random.nextDouble()) * TELEPORT_RADIUS;
            double x = living.getX() + Math.cos(angle) * distance;
            double y = living.getY() + random.nextInt(33) - 16;
            double z = living.getZ() + Math.sin(angle) * distance;
            if (living.randomTeleport(x, y, z, true)) {
                kill(level);
                return true;
            }
        }
        return false;
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Endermite asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMEndermite-T1";
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
        return false;
    }
}
