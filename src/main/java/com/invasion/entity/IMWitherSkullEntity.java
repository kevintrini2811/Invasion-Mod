package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.WitherSkull;
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
        if (level() instanceof ServerLevel
                && hitResult instanceof BlockHitResult blockHit
                && getOwner() instanceof IMWitherEntity wither
                && wither.hasNexus()
                && wither.getNexus().isActive()
                && blockHit.getBlockPos().equals(
                        wither.getNexus().getOrigin())) {
            wither.getNexus().damage(
                    damageSources().witherSkull(this, wither), NEXUS_DAMAGE);
        }
        super.onHit(hitResult);
    }
}
