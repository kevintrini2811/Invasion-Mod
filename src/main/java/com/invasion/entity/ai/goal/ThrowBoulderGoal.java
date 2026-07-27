package com.invasion.entity.ai.goal;

import com.invasion.entity.ThrowerEntity;
import com.invasion.nexus.NexusAccess;
import net.minecraft.world.entity.ai.goal.Goal;

public class ThrowBoulderGoal extends Goal {
    private final ThrowerEntity theEntity;
    private int randomAmmo;
    private int timer = 180;

    public ThrowBoulderGoal(ThrowerEntity entity, int ammo) {
        theEntity = entity;
        randomAmmo = ammo;
    }

    @Override
    public boolean canUse() {
        return theEntity.getNexus() != null && randomAmmo > 0 && theEntity.canThrow() && --timer <= 0;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        randomAmmo--;
        timer = 240;
        NexusAccess nexus = theEntity.getNexus();
        int d = Math.max(1, (int) (theEntity.findDistanceToNexus() * 0.37D));
        theEntity.throwProjectile(com.invasion.util.math.PosUtils.bottomCenter(nexus.getOrigin()).add(
                theEntity.getRandom().triangle(0, d),
                theEntity.getRandom().triangle(0, 10),
                theEntity.getRandom().triangle(0, d)
        ), theEntity.createProjectile(0));
    }
}
