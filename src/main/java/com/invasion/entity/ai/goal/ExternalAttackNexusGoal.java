package com.invasion.entity.ai.goal;

import com.invasion.nexus.NexusAccess;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class ExternalAttackNexusGoal extends Goal {
    private final Mob mob;
    private NexusAccess nexus;
    private int attackCooldown = 0;

    public ExternalAttackNexusGoal(Mob mob, NexusAccess nexus) {
        this.mob = mob;
        this.nexus = nexus;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setNexus(NexusAccess nexus) {
        this.nexus = nexus;
    }

    @Override
    public boolean canUse() {
        return nexus != null && nexus.isActive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !mob.isDeadOrDying();
    }

    @Override
    public void tick() {
        if (nexus == null) return;

        BlockPos pos = nexus.getOrigin();
        Vec3 target = Vec3.atCenterOf(pos);

        double distSq = mob.distanceToSqr(target);
        // erst hinlaufen
        if (distSq > 4.0) {
            mob.getNavigation().moveTo(target.x, target.y, target.z, 1.1);
        } else {
            // Am Nexus: stehenbleiben und draufhauen
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(target.x, target.y, target.z);

            if (attackCooldown > 0) {
                attackCooldown--;
                return;
            }

            mob.swing(InteractionHand.MAIN_HAND);
            if (nexus.isActive()) {
                nexus.damage(mob.damageSources().mobAttack(mob), 2);
            }
            attackCooldown = 20; // alle 20 Ticks = 1x pro Sekunde
        }
    }
}
