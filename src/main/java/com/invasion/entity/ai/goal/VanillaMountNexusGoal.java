package com.invasion.entity.ai.goal;

import java.util.EnumSet;

import com.invasion.entity.NexusEntity;
import com.invasion.nexus.NexusAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Lets a vanilla jockey mount carry its converted IM rider into battle. */
public final class VanillaMountNexusGoal extends Goal {
    private static final int REPATH_INTERVAL = 20;
    private final PathfinderMob mount;
    private final Mob rider;
    private final NexusEntity nexusRider;
    private int repathTimer;

    public VanillaMountNexusGoal(PathfinderMob mount, Mob rider, NexusEntity nexusRider) {
        this.mount = mount;
        this.rider = rider;
        this.nexusRider = nexusRider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return rider.getVehicle() == mount && nexusRider.hasNexus()
                && nexusRider.getNexus().isActive();
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void start() {
        repathTimer = 0;
        moveToObjective();
    }

    @Override
    public void tick() {
        if (--repathTimer <= 0 || mount.getNavigation().isDone()) moveToObjective();
    }

    @Override
    public void stop() { mount.getNavigation().stop(); }

    private void moveToObjective() {
        repathTimer = REPATH_INTERVAL;
        LivingEntity target = rider.getTarget();
        if (target != null && target.isAlive()) {
            mount.getNavigation().moveTo(target, 1.2D);
            return;
        }
        NexusAccess nexus = nexusRider.getNexus();
        BlockPos targetPos = nexus.getOrigin();
        mount.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(),
                targetPos.getZ() + 0.5D, 1.1D);
    }
}
