package com.invasion.entity.ai.goal;

import com.invasion.nexus.NexusAccess;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class ExternalAttackNexusGoal extends Goal {
    private final MobEntity mob;
    private NexusAccess nexus;
    private int attackCooldown = 0;

    public ExternalAttackNexusGoal(MobEntity mob, NexusAccess nexus) {
        this.mob = mob;
        this.nexus = nexus;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    public void setNexus(NexusAccess nexus) {
        this.nexus = nexus;
    }

    @Override
    public boolean canStart() {
        return nexus != null && nexus.isActive();
    }

    @Override
    public boolean shouldContinue() {
        return canStart() && !mob.isDead();
    }

    @Override
    public void tick() {
        if (nexus == null) return;

        BlockPos pos = nexus.getOrigin();
        Vec3d target = Vec3d.ofCenter(pos);

        double distSq = mob.squaredDistanceTo(target);
        // erst hinlaufen
        if (distSq > 4.0) {
            mob.getNavigation().startMovingTo(target.x, target.y, target.z, 1.1);
        } else {
            // Am Nexus: stehenbleiben und draufhauen
            mob.getNavigation().stop();
            mob.getLookControl().lookAt(target.x, target.y, target.z);

            if (attackCooldown > 0) {
                attackCooldown--;
                return;
            }

            mob.swingHand(Hand.MAIN_HAND);
            if (nexus.isActive()) {
                nexus.damage(mob.getDamageSources().mobAttack(mob), 2);
            }
            attackCooldown = 20; // alle 20 Ticks = 1x pro Sekunde
        }
    }
}
