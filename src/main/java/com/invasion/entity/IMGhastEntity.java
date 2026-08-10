package com.invasion.entity;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/** A long-range Nexus attacker retaining the vanilla Ghast behaviour. */
public final class IMGhastEntity extends Ghast
        implements Combatant<Ghast>, EntityConstruct.BuildableMob {
    private static final double TARGET_RANGE = 64.0D;
    private static final double NEXUS_ATTACK_RANGE = 64.0D;
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMGhastEntity(EntityType<? extends Ghast> type, Level level) {
        super(type, level);
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IMGhastEntity::onProjectileImpact);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(0, new PlayerAllyTargetGoal());
        goalSelector.addGoal(4, new AttackNexusGoal());
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Ghast asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMGhast-T1";
    }

    @Override
    public void onSpawned(@Nullable NexusAccess nexus,
            EntityConstruct spawnConditions) {
        setNexus(nexus);
        resetHealth();
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

    private static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof LargeFireball fireball)
                || !(fireball.getOwner() instanceof IMGhastEntity ghast)
                || !(event.getRayTraceResult() instanceof BlockHitResult hit)) {
            return;
        }
        NexusAccess nexus = ghast.getNexus();
        if (nexus != null && hit.getBlockPos().equals(nexus.getOrigin())) {
            nexus.damage(ghast.damageSources().fireball(fireball, ghast), 6);
            event.setCanceled(true);
            fireball.discard();
        }
    }

    private final class PlayerAllyTargetGoal extends Goal {
        private int searchCooldown;

        private PlayerAllyTargetGoal() {
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return hasNexus() && getNexus().isActive();
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
        public void tick() {
            LivingEntity current = getTarget();
            if (current != null && current.isAlive()
                    && distanceToSqr(current) <= TARGET_RANGE * TARGET_RANGE
                    && IMWitchEntity.isPlayerAlly(current, (ServerLevel) level())) {
                return;
            }
            if (searchCooldown-- > 0) {
                setTarget(null);
                return;
            }
            searchCooldown = 10;
            LivingEntity nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : level().getEntitiesOfClass(
                    LivingEntity.class, getBoundingBox().inflate(TARGET_RANGE),
                    entity -> entity.isAlive()
                            && IMWitchEntity.isPlayerAlly(
                                    entity, (ServerLevel) level()))) {
                double distance = distanceToSqr(candidate);
                if (distance < nearestDistance && hasLineOfSight(candidate)) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
            setTarget(nearest);
        }
    }

    private final class AttackNexusGoal extends Goal {
        private int chargeTime;

        private AttackNexusGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
            chargeTime = 0;
            setCharging(false);
        }

        @Override
        public void tick() {
            Vec3 target = Vec3.atCenterOf(getNexus().getOrigin());
            double distance = distanceToSqr(target);
            getLookControl().setLookAt(target.x, target.y, target.z, 10.0F, 10.0F);
            if (distance > NEXUS_ATTACK_RANGE * NEXUS_ATTACK_RANGE) {
                getMoveControl().setWantedPosition(
                        target.x, target.y + 8.0D, target.z, 1.0D);
                chargeTime = 0;
                setCharging(false);
                return;
            }
            chargeTime++;
            if (chargeTime == 10) {
                level().levelEvent(null, 1015, blockPosition(), 0);
            } else if (chargeTime == 20) {
                Vec3 direction = target.subtract(getX(), getY(0.5D), getZ());
                LargeFireball fireball = new LargeFireball(
                        level(), IMGhastEntity.this, direction.normalize(), 1);
                Vec3 muzzle = getViewVector(1.0F).scale(4.0D);
                fireball.setPos(getX() + muzzle.x, getY(0.5D) + 0.5D,
                        getZ() + muzzle.z);
                level().addFreshEntity(fireball);
                level().levelEvent(null, 1016, blockPosition(), 0);
                chargeTime = -40;
            }
            setCharging(chargeTime > 10);
        }
    }
}
