package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A Wither skull that damages the Nexus only when it reaches the core. */
public final class IMWitherSkullEntity extends WitherSkull {
    private static final int NEXUS_DAMAGE = 8;

    public IMWitherSkullEntity(
            EntityType<? extends IMWitherSkullEntity> type, Level level) {
        super(type, level);
    }

    public IMWitherSkullEntity(
            Level level, IMWitherEntity owner, Vec3 direction) {
        this(InvEntities.WITHER_SKULL, level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY(), owner.getZ());
        setDeltaMovement(direction.normalize().scale(0.1D));
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !(entity instanceof com.invasion.nexus.Combatant<?>)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        IMWitherEntity nexusWither = level() instanceof ServerLevel
                && hitResult instanceof BlockHitResult blockHit
                && getOwner() instanceof IMWitherEntity wither
                && wither.hasNexus()
                && wither.getNexus().isActive()
                && blockHit.getBlockPos().equals(
                        wither.getNexus().getOrigin())
                ? wither
                : null;

        // Resolve and discard the projectile before Nexus damage can end the
        // invasion and synchronously remove its owner. Continuing vanilla hit
        // handling with an already removed owner can stall the server tick.
        super.onHit(hitResult);

        if (nexusWither != null && nexusWither.hasNexus()) {
            nexusWither.getNexus().damage(
                    damageSources().witherSkull(this, nexusWither), NEXUS_DAMAGE);
        }
    }
}
