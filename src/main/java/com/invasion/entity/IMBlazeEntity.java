package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/** A Nexus-bound Blaze retaining the complete vanilla Blaze behaviour. */
public final class IMBlazeEntity extends Blaze
        implements Combatant<Blaze>, EntityConstruct.BuildableMob {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMBlazeEntity(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
        setCanPickUpLoot(true);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot == EquipmentSlot.HEAD
                && canReplaceCurrentItem(stack, getItemBySlot(slot));
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IMBlazeEntity::onProjectileImpact);
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
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        nexus.writeNbt(output);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
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

    private static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof SmallFireball fireball)
                || !(fireball.getOwner() instanceof IMBlazeEntity blaze)
                || !(event.getRayTraceResult() instanceof BlockHitResult hit)) {
            return;
        }
        NexusAccess nexus = blaze.getNexus();
        if (nexus != null && hit.getBlockPos().equals(nexus.getOrigin())) {
            nexus.damage(blaze.damageSources().fireball(fireball, blaze), 2);
            event.setCanceled(true);
            fireball.discard();
        }
    }

    private final class AttackNexusGoal extends Goal {
        private static final double ATTACK_RANGE_SQUARED = 32.0D * 32.0D;
        private int attackCooldown;
        private final FlyingWallPath wallPath = new FlyingWallPath(IMBlazeEntity.this);

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
            wallPath.reset();
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
            return wallPath.findTarget(nexusTarget, nexusPos);
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
