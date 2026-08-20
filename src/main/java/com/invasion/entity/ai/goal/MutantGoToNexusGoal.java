package com.invasion.entity.ai.goal;

import java.util.EnumSet;

import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.entity.NexusBoundMobLifecycle;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public final class MutantGoToNexusGoal extends Goal {
    private final PathfinderMob mob;
    private final IHasNexus boundMob;
    private int repathCooldown;

    public MutantGoToNexusGoal(PathfinderMob mob, IHasNexus boundMob) {
        this.mob = mob;
        this.boundMob = boundMob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        NexusAccess nexus = boundMob.getNexus();
        return mob.getTarget() == null && nexus != null && nexus.isActive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (NexusBoundMobLifecycle.isFollowingRecoveryPath(mob)) {
            return;
        }
        NexusAccess nexus = boundMob.getNexus();
        if (nexus != null && (--repathCooldown <= 0
                || mob.getNavigation().isDone())) {
            mob.getNavigation().moveTo(
                    nexus.getOrigin().getX() + 0.5D,
                    nexus.getOrigin().getY(),
                    nexus.getOrigin().getZ() + 0.5D, 1.1D);
            repathCooldown = 20;
        }
    }
}
