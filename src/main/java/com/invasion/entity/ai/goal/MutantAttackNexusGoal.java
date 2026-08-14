package com.invasion.entity.ai.goal;

import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import com.invasion.entity.mutant.IMMutantMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public final class MutantAttackNexusGoal extends Goal {
    private final PathfinderMob mob;
    private final IMMutantMob boundMob;
    private int cooldown;

    public MutantAttackNexusGoal(PathfinderMob mob, IMMutantMob boundMob) {
        this.mob = mob;
        this.boundMob = boundMob;
    }

    @Override
    public boolean canUse() {
        NexusAccess nexus = boundMob.getNexus();
        return mob.getTarget() == null && nexus != null && nexus.isActive()
                && mob.distanceToSqr(Vec3.atCenterOf(nexus.getOrigin())) <= 16.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        NexusAccess nexus = boundMob.getNexus();
        if (nexus != null && --cooldown <= 0
                && mob.level() instanceof net.minecraft.server.level.ServerLevel level) {
            cooldown = boundMob.performNexusAttack(level, nexus);
        }
    }
}
