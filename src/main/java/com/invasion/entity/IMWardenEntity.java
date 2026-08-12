package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.util.math.PosUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A nexus-bound Warden that can consume experience to restore health. */
public final class IMWardenEntity extends Warden
        implements Combatant<Warden>, EntityConstruct.BuildableMob {
    private static final double ORB_PICKUP_RANGE = 2.5D;
    private static final double SONIC_BOOM_HORIZONTAL_RANGE = 15.0D;
    private static final double SONIC_BOOM_VERTICAL_RANGE = 20.0D;
    private static final int SONIC_BOOM_CHARGE_TICKS = 34;
    private static final int SONIC_BOOM_COOLDOWN_TICKS = 40;
    private static final int SONIC_BOOM_NEXUS_DAMAGE = 10;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private int nexusSonicBoomCharge;
    private int nexusSonicBoomCooldown;

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
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) level();
        NexusAccess currentNexus = getNexus();
        if (currentNexus != null
                && (currentNexus.isDiscarded() || !currentNexus.isActive())) {
            setNexus(null);
            kill();
            return;
        }
        if (!hasNexus()) {
            WorldNexusStorage.of(level).getNexus()
                    .filter(NexusAccess::isActive)
                    .ifPresent(this::setNexus);
        }

        getBrain().setMemoryWithExpiry(
                MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        super.customServerAiStep();
        consumeExperienceOrbs(level);
        approachAndAttackNexus(level);
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

    private void approachAndAttackNexus(ServerLevel level) {
        if (nexusSonicBoomCooldown > 0) {
            nexusSonicBoomCooldown--;
        }
        NexusAccess targetNexus = getNexus();
        if (targetNexus == null || !targetNexus.isActive()
                || isOccupiedByVanillaWardenAi()) {
            cancelNexusSonicBoom();
            return;
        }

        Vec3 target = PosUtils.center(targetNexus.getOrigin());
        if (!isWithinSonicBoomRange(target)) {
            cancelNexusSonicBoom();
            getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
            return;
        }

        getNavigation().stop();
        getLookControl().setLookAt(target.x, target.y, target.z);
        if (nexusSonicBoomCooldown > 0) {
            return;
        }
        if (nexusSonicBoomCharge == 0) {
            nexusSonicBoomCharge = SONIC_BOOM_CHARGE_TICKS;
            level.broadcastEntityEvent(this, (byte) 62);
            playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 1.0F);
            return;
        }
        if (--nexusSonicBoomCharge == 0) {
            fireSonicBoomAtNexus(level, targetNexus, target);
            nexusSonicBoomCooldown = SONIC_BOOM_COOLDOWN_TICKS;
        }
    }

    private boolean isWithinSonicBoomRange(Vec3 target) {
        double horizontalDistanceSquared = target.subtract(position())
                .multiply(1.0D, 0.0D, 1.0D).lengthSqr();
        return horizontalDistanceSquared
                        <= SONIC_BOOM_HORIZONTAL_RANGE * SONIC_BOOM_HORIZONTAL_RANGE
                && Math.abs(target.y - getY()) <= SONIC_BOOM_VERTICAL_RANGE;
    }

    private void fireSonicBoomAtNexus(
            ServerLevel level, NexusAccess targetNexus, Vec3 target) {
        Vec3 source = position().add(0.0D, 1.6D, 0.0D);
        Vec3 delta = target.subtract(source);
        Vec3 direction = delta.normalize();
        int steps = Mth.floor(delta.length()) + 7;
        for (int i = 1; i < steps; i++) {
            Vec3 particle = source.add(direction.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                    particle.x, particle.y, particle.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
        targetNexus.damage(
                damageSources().sonicBoom(this), SONIC_BOOM_NEXUS_DAMAGE);
    }

    private void cancelNexusSonicBoom() {
        nexusSonicBoomCharge = 0;
    }

    private boolean isOccupiedByVanillaWardenAi() {
        return getTarget() != null
                || getBrain().hasMemoryValue(MemoryModuleType.ROAR_TARGET)
                || getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION)
                || getBrain().hasMemoryValue(MemoryModuleType.IS_SNIFFING);
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return !isFromCombatant(target) && super.canAttack(target);
    }

    @Override
    public boolean canTargetEntity(Entity target) {
        return !isFromCombatant(target) && super.canTargetEntity(target);
    }

    private static boolean isFromCombatant(Entity entity) {
        return entity instanceof Combatant<?>
                || entity instanceof Projectile projectile
                        && projectile.getOwner() instanceof Combatant<?>;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return new CombatantFilteringVibrationUser(super.getVibrationUser());
    }

    private record CombatantFilteringVibrationUser(VibrationSystem.User delegate)
            implements VibrationSystem.User {
        @Override
        public int getListenerRadius() {
            return delegate.getListenerRadius();
        }

        @Override
        public PositionSource getPositionSource() {
            return delegate.getPositionSource();
        }

        @Override
        public TagKey<GameEvent> getListenableEvents() {
            return delegate.getListenableEvents();
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return delegate.canTriggerAvoidVibration();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return delegate.requiresAdjacentChunksToBeTicking();
        }

        @Override
        public int calculateTravelTimeInTicks(float distance) {
            return delegate.calculateTravelTimeInTicks(distance);
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos,
                GameEvent event, GameEvent.Context context) {
            return !isFromCombatant(context.sourceEntity())
                    && delegate.canReceiveVibration(level, pos, event, context);
        }

        @Override
        public void onReceiveVibration(ServerLevel level, BlockPos pos,
                GameEvent event, @Nullable Entity sourceEntity,
                @Nullable Entity projectileOwner, float receivingDistance) {
            if (!isFromCombatant(sourceEntity)
                    && !isFromCombatant(projectileOwner)) {
                delegate.onReceiveVibration(level, pos, event, sourceEntity,
                        projectileOwner, receivingDistance);
            }
        }

        @Override
        public void onDataChanged() {
            delegate.onDataChanged();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!isFromCombatant(source.getDirectEntity())
                && !isFromCombatant(source.getEntity())) {
            cancelNexusSonicBoom();
        }
        return super.hurt(source, damage);
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
