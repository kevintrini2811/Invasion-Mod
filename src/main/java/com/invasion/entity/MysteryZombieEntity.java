package com.invasion.entity;

import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.wave.BudgetWavePlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/** The exceptionally rare ???? zombie variant. */
public final class MysteryZombieEntity extends EntityIMZombie {
    private boolean releasedMob;

    public MysteryZombieEntity(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
    }

    @Override
    public void die(DamageSource source) {
        if (!releasedMob && level() instanceof ServerLevel world) {
            releasedMob = true;
            EntityConstruct construct = BudgetWavePlan.randomMobConstruct(
                    getRandom());
            Mob replacement = construct.createMob(world, getNexus());
            if (replacement != null) {
                replacement.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
                replacement.setDeltaMovement(getDeltaMovement());
                if (replacement instanceof EntityIMLiving imMob) {
                    imMob.setCountsTowardMobCap(countsTowardMobCap());
                }
                world.addFreshEntity(replacement);
            }
        }
        super.die(source);
    }
}
