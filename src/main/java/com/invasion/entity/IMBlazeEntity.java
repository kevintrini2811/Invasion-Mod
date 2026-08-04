package com.invasion.entity;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

/** A Nexus-bound Blaze retaining the complete vanilla Blaze behaviour. */
public final class IMBlazeEntity extends Blaze
        implements Combatant<Blaze>, EntityConstruct.BuildableMob {
    private static final Map<ServerLevel, Set<SmallFireball>> FIREBALLS =
            new WeakHashMap<>();
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMBlazeEntity(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
    }

    public static void bootstrap() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof SmallFireball fireball) {
                FIREBALLS.computeIfAbsent(level, ignored ->
                        Collections.newSetFromMap(new IdentityHashMap<>()))
                        .add(fireball);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            Set<SmallFireball> fireballs = FIREBALLS.get(level);
            if (fireballs != null && entity instanceof SmallFireball fireball) {
                fireballs.remove(fireball);
            }
        });
        ServerTickEvents.START_LEVEL_TICK.register(IMBlazeEntity::checkProjectileImpacts);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new AttackNexusGoal());
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Blaze asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMBlaze-T1";
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

    private static void checkProjectileImpacts(ServerLevel level) {
        Set<SmallFireball> fireballs = FIREBALLS.get(level);
        if (fireballs == null || fireballs.isEmpty()) {
            return;
        }
        for (SmallFireball fireball : Set.copyOf(fireballs)) {
            if (!(fireball.getOwner() instanceof IMBlazeEntity blaze)) {
                continue;
            }
            NexusAccess nexus = blaze.getNexus();
            if (nexus == null) {
                continue;
            }
            BlockHitResult hit = level.clip(new ClipContext(
                    fireball.position(),
                    fireball.position().add(fireball.getDeltaMovement()),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fireball));
            if (hit.getType() == HitResult.Type.BLOCK
                    && hit.getBlockPos().equals(nexus.getOrigin())) {
                nexus.damage(blaze.damageSources().fireball(fireball, blaze), 2);
                fireball.discard();
            }
        }
    }

    private final class AttackNexusGoal extends Goal {
        private static final double ATTACK_RANGE_SQUARED = 32.0D * 32.0D;
        private int attackCooldown;
        @Nullable
        private Vec3 wallCrossingTarget;

        private AttackNexusGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return hasNexus() && getNexus().isActive() && getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            wallCrossingTarget = null;
        }

        @Override
        public void tick() {
            NexusAccess nexus = getNexus();
            if (nexus == null) {
                return;
            }
            Vec3 nexusTarget = Vec3.atCenterOf(nexus.getOrigin());
            getLookControl().setLookAt(nexusTarget.x, nexusTarget.y, nexusTarget.z);
            if (distanceToSqr(nexusTarget) > 8.0D * 8.0D) {
                Vec3 flightTarget = findFlightTarget(nexusTarget, nexus.getOrigin());
                getMoveControl().setWantedPosition(
                        flightTarget.x, flightTarget.y,
                        flightTarget.z, 1.0D);
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

            if (attackCooldown > 0) {
                attackCooldown--;
                return;
            }
            Vec3 shotTarget = findShotTarget(nexusTarget, nexus.getOrigin());
            if (shotTarget == null || distanceToSqr(shotTarget) > ATTACK_RANGE_SQUARED) {
                return;
            }
            Vec3 direction = shotTarget.subtract(getX(), getY(0.5D), getZ());
            SmallFireball fireball = new SmallFireball(level(), IMBlazeEntity.this,
                    direction.normalize());
            fireball.setPos(getX(), getY(0.5D) + 0.5D, getZ());
            level().addFreshEntity(fireball);
            level().levelEvent(null, 1018, blockPosition(), 0);
            attackCooldown = 60;
        }

        private Vec3 findFlightTarget(
                Vec3 nexusTarget, net.minecraft.core.BlockPos nexusPos) {
            if (wallCrossingTarget != null) {
                double horizontalDistanceSquared =
                        distanceToSqr(wallCrossingTarget.x, getY(), wallCrossingTarget.z);
                if (horizontalDistanceSquared > 1.5D * 1.5D
                        || getY() < wallCrossingTarget.y - 1.5D) {
                    return wallCrossingTarget;
                }
                wallCrossingTarget = null;
            }

            BlockHitResult hit = findWallHit(nexusTarget);
            if (hit.getType() != HitResult.Type.BLOCK
                    || hit.getBlockPos().equals(nexusPos)) {
                return nexusTarget.add(0.0D, 2.0D, 0.0D);
            }

            net.minecraft.core.BlockPos wall = hit.getBlockPos();
            int startY = Math.max(wall.getY() + 1, blockPosition().getY());
            for (int y = startY; y < level().getMaxY() - 1; y++) {
                net.minecraft.core.BlockPos lower = new net.minecraft.core.BlockPos(
                        wall.getX(), y, wall.getZ());
                net.minecraft.core.BlockPos upper = lower.above();
                if (level().getBlockState(lower)
                                .getCollisionShape(level(), lower).isEmpty()
                        && level().getBlockState(upper)
                                .getCollisionShape(level(), upper).isEmpty()) {
                    Vec3 acrossWall = nexusTarget.subtract(Vec3.atCenterOf(wall));
                    acrossWall = new Vec3(acrossWall.x, 0.0D, acrossWall.z);
                    if (acrossWall.lengthSqr() > 0.0D) {
                        // Keep flying beyond the edge instead of immediately
                        // descending as soon as the top becomes visible.
                        acrossWall = acrossWall.normalize().scale(4.0D);
                    }
                    wallCrossingTarget = new Vec3(
                            wall.getX() + 0.5D + acrossWall.x,
                            y + 1.0D,
                            wall.getZ() + 0.5D + acrossWall.z);
                    return wallCrossingTarget;
                }
            }
            return nexusTarget.add(0.0D, 2.0D, 0.0D);
        }

        private BlockHitResult findWallHit(Vec3 nexusTarget) {
            Vec3 horizontalTarget = new Vec3(
                    nexusTarget.x, getEyeY(), nexusTarget.z);
            return level().clip(new ClipContext(
                    getEyePosition(), horizontalTarget, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, IMBlazeEntity.this));
        }

        @Nullable
        private Vec3 findShotTarget(Vec3 nexusTarget, net.minecraft.core.BlockPos nexusPos) {
            BlockHitResult hit = level().clip(new ClipContext(
                    getEyePosition(), nexusTarget, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, IMBlazeEntity.this));
            if (hit.getType() != HitResult.Type.BLOCK
                    || hit.getBlockPos().equals(nexusPos)) {
                return nexusTarget;
            }
            return level().getBlockState(hit.getBlockPos()).ignitedByLava()
                    ? Vec3.atCenterOf(hit.getBlockPos())
                    : null;
        }
    }
}
