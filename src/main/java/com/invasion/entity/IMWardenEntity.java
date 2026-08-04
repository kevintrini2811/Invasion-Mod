package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.util.math.PosUtils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A nexus-bound Warden that can consume experience to restore health. */
public final class IMWardenEntity extends Warden
        implements Combatant<Warden>, EntityConstruct.BuildableMob {
    private static final double ORB_PICKUP_RANGE = 2.5D;
    private static final double NEXUS_ATTACK_RANGE_SQUARED = 4.0D * 4.0D;
    private static final int NEXUS_ATTACK_INTERVAL = 20;
    private static final int NEXUS_DAMAGE = 2;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private int nexusAttackCooldown;

    public IMWardenEntity(EntityType<? extends Warden> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Warden.createAttributes();
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Warden asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMWarden-T1";
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        setHealth(getMaxHealth());
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
        consumeExperienceOrbs(level);
        approachAndAttackNexus();
    }

    private void consumeExperienceOrbs(ServerLevel level) {
        if (getHealth() >= getMaxHealth()) {
            return;
        }
        AABB pickupArea = getBoundingBox().inflate(ORB_PICKUP_RANGE);
        for (ExperienceOrb orb : level.getEntitiesOfClass(
                ExperienceOrb.class, pickupArea, ExperienceOrb::isAlive)) {
            heal(orb.getValue());
            orb.discard();
            if (getHealth() >= getMaxHealth()) {
                break;
            }
        }
    }

    private void approachAndAttackNexus() {
        NexusAccess targetNexus = getNexus();
        if (targetNexus == null || !targetNexus.isActive()
                || getTarget() != null) {
            return;
        }

        Vec3 target = PosUtils.center(targetNexus.getOrigin());
        double distanceSquared = distanceToSqr(target);
        if (distanceSquared > NEXUS_ATTACK_RANGE_SQUARED) {
            getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
            return;
        }

        getNavigation().stop();
        getLookControl().setLookAt(target.x, target.y, target.z);
        if (nexusAttackCooldown > 0) {
            nexusAttackCooldown--;
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        targetNexus.damage(damageSources().mobAttack(this), NEXUS_DAMAGE);
        nexusAttackCooldown = NEXUS_ATTACK_INTERVAL;
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return !(target instanceof Combatant<?>) && super.canAttack(target);
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
