package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.util.math.PosUtils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A nexus-bound Wither whose skull barrage can damage the Nexus. */
public final class IMWitherEntity extends WitherBoss
        implements Combatant<WitherBoss>, EntityConstruct.BuildableMob {
    private static final double MAX_NEXUS_ATTACK_DISTANCE_SQUARED = 64.0D * 64.0D;
    private static final double NEXUS_HOVER_DISTANCE_SQUARED = 12.0D * 12.0D;
    private static final int NEXUS_ATTACK_INTERVAL = 60;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private int nexusAttackCooldown;

    public IMWitherEntity(EntityType<? extends WitherBoss> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WitherBoss.createAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // The side heads may fire at nearby enemies, but no living entity may
        // become a navigation target and pull the Wither away from the Nexus.
        targetSelector.removeAllGoals(goal -> true);
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public WitherBoss asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMWither-T1";
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        double maxHealth = 300.0D + Math.max(0, spawnConditions.tier()) * 25.0D;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        setHealth((float) maxHealth);
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
    protected void customServerAiStep(ServerLevel level) {
        NexusAccess currentNexus = getNexus();
        if (currentNexus != null
                && (currentNexus.isDiscarded() || !currentNexus.isActive())) {
            setNexus(null);
            kill(level);
            return;
        }
        if (!hasNexus()) {
            WorldNexusStorage.of(level).getNexus()
                    .filter(NexusAccess::isActive)
                    .ifPresent(this::setNexus);
        }
        super.customServerAiStep(level);
        attackNexus(level);
    }

    private void attackNexus(ServerLevel level) {
        NexusAccess targetNexus = getNexus();
        if (targetNexus == null || !targetNexus.isActive()
                || getInvulnerableTicks() > 0) {
            return;
        }

        Vec3 target = PosUtils.center(targetNexus.getOrigin());
        double distanceSquared = distanceToSqr(target);
        if (getTarget() == null && distanceSquared > NEXUS_HOVER_DISTANCE_SQUARED) {
            getMoveControl().setWantedPosition(
                    target.x, target.y + 5.0D, target.z, 1.0D);
        }
        if (distanceSquared > MAX_NEXUS_ATTACK_DISTANCE_SQUARED) {
            return;
        }
        if (nexusAttackCooldown > 0) {
            nexusAttackCooldown--;
            return;
        }

        Vec3 origin = new Vec3(getX(), getEyeY(), getZ());
        Vec3 direction = target.subtract(origin).normalize();
        IMWitherSkullEntity skull = new IMWitherSkullEntity(
                level, this, direction);
        level.addFreshEntity(skull);
        level.levelEvent(null, 1024, blockPosition(), 0);
        nexusAttackCooldown = NEXUS_ATTACK_INTERVAL;
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return !(target instanceof Combatant<?>) && super.canAttack(target);
    }

    public void setMergedHealth(double health) {
        double clampedHealth = Math.max(1.0D, health);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(clampedHealth);
        setHealth((float) clampedHealth);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !hasNexus();
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }
}
