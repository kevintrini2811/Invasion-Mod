package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Nexus-bound Breeze using Blaze-style flight and vanilla wind attacks. */
public final class IMBreezeEntity extends Breeze
        implements Combatant<Breeze>, EntityConstruct.BuildableMob {
    private static final double APPROACH_DISTANCE_SQUARED = 8.0D * 8.0D;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    @Nullable
    private Vec3 wallCrossingTarget;

    public IMBreezeEntity(EntityType<? extends Breeze> type, Level level) {
        super(type, level);
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Breeze asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMBreeze-T1";
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

    @Override
    public boolean canAttack(LivingEntity candidate) {
        return level() instanceof ServerLevel serverLevel
                && IMWitchEntity.isPlayerAlly(candidate, serverLevel)
                && candidate.isAlive()
                && candidate.canBeSeenAsEnemy()
                && candidate.attackable()
                && !isAlliedTo(candidate)
                && candidate != this;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (isOnFire() || isInLava()) {
            convertToBlaze(level);
            return;
        }
        super.customServerAiStep(level);
        updateBlazeFlight();
    }

    private void updateBlazeFlight() {
        Vec3 objective = null;
        LivingEntity attackTarget = getTarget();
        if (attackTarget != null && attackTarget.isAlive()) {
            objective = attackTarget.position().add(0.0D, 1.0D, 0.0D);
        }
        if (objective == null || distanceToSqr(objective) <= APPROACH_DISTANCE_SQUARED) {
            return;
        }

        Vec3 flightTarget = findFlightTarget(objective);
        getMoveControl().setWantedPosition(
                flightTarget.x, flightTarget.y, flightTarget.z, 1.0D);
        Vec3 approach = flightTarget.subtract(position()).normalize();
        Vec3 velocity = getDeltaMovement().add(approach.scale(0.025D));
        if (flightTarget.y > getY() + 1.0D) {
            velocity = new Vec3(
                    velocity.x,
                    velocity.y + (0.3D - velocity.y) * 0.3D,
                    velocity.z);
        }
        double horizontalSpeed = velocity.horizontalDistance();
        if (horizontalSpeed > 0.35D) {
            double scale = 0.35D / horizontalSpeed;
            velocity = new Vec3(
                    velocity.x * scale, velocity.y, velocity.z * scale);
        }
        setDeltaMovement(velocity);
    }

    private Vec3 findFlightTarget(Vec3 objective) {
        if (wallCrossingTarget != null) {
            double horizontalDistanceSquared = distanceToSqr(
                    wallCrossingTarget.x, getY(), wallCrossingTarget.z);
            if (horizontalDistanceSquared > 1.5D * 1.5D
                    || getY() < wallCrossingTarget.y - 1.5D) {
                return wallCrossingTarget;
            }
            wallCrossingTarget = null;
        }

        Vec3 horizontalTarget = new Vec3(objective.x, getEyeY(), objective.z);
        BlockHitResult hit = level().clip(new ClipContext(
                getEyePosition(), horizontalTarget, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return objective.add(0.0D, 2.0D, 0.0D);
        }

        BlockPos wall = hit.getBlockPos();
        int startY = Math.max(wall.getY() + 1, blockPosition().getY());
        for (int y = startY; y < level().getMaxY() - 1; y++) {
            BlockPos lower = new BlockPos(wall.getX(), y, wall.getZ());
            BlockPos upper = lower.above();
            if (!level().getBlockState(lower)
                            .getCollisionShape(level(), lower).isEmpty()
                    || !level().getBlockState(upper)
                            .getCollisionShape(level(), upper).isEmpty()) {
                continue;
            }
            Vec3 acrossWall = objective.subtract(Vec3.atCenterOf(wall));
            acrossWall = new Vec3(acrossWall.x, 0.0D, acrossWall.z);
            if (acrossWall.lengthSqr() > 0.0D) {
                acrossWall = acrossWall.normalize().scale(4.0D);
            }
            wallCrossingTarget = new Vec3(
                    wall.getX() + 0.5D + acrossWall.x,
                    y + 1.0D,
                    wall.getZ() + 0.5D + acrossWall.z);
            return wallCrossingTarget;
        }
        return objective.add(0.0D, 2.0D, 0.0D);
    }

    private void convertToBlaze(ServerLevel level) {
        IMBlazeEntity blaze = InvEntities.BLAZE.create(
                level, EntitySpawnReason.CONVERSION);
        if (blaze == null) {
            return;
        }
        blaze.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        blaze.setDeltaMovement(getDeltaMovement());
        blaze.setCustomName(getCustomName());
        blaze.setCustomNameVisible(isCustomNameVisible());
        blaze.setNoAi(isNoAi());
        blaze.setCanPickUpLoot(canPickUpLoot());
        blaze.setNexus(getNexus());
        if (isPersistenceRequired()) {
            blaze.setPersistenceRequired();
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            blaze.setItemSlot(slot, getItemBySlot(slot).copy());
        }
        blaze.setHealth(Math.min(blaze.getMaxHealth(),
                getHealth() / getMaxHealth() * blaze.getMaxHealth()));
        Entity vehicle = getVehicle();
        if (!level.addFreshEntity(blaze)) {
            return;
        }
        stopRiding();
        discard();
        if (vehicle != null && !vehicle.isRemoved()) {
            blaze.startRiding(vehicle);
        }
    }

}
