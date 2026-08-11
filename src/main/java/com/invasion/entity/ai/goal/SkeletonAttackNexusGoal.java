package com.invasion.entity.ai.goal;

import java.util.EnumSet;

import com.invasion.entity.HasAiGoals;
import com.invasion.entity.NexusEntity;
import com.invasion.entity.RangedNexusAttacker;
import com.invasion.util.math.PosUtils;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SkeletonAttackNexusGoal<T extends PathfinderMob & NexusEntity & RangedNexusAttacker> extends Goal {
    private static final double MIN_RANGE_SQUARED = 16;
    private static final double MAX_RANGE_SQUARED = 16 * 16;
    private static final int ATTACK_DELAY = 65;

    private final T skeleton;
    private int attackTime;

    public SkeletonAttackNexusGoal(T skeleton) {
        this.skeleton = skeleton;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return canContinueToUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (skeleton.getTarget() != null || !skeleton.hasNexus()
                || !skeleton.hasGoal(HasAiGoals.Goal.BREAK_NEXUS)) {
            return false;
        }
        double distance = skeleton.distanceToSqr(nexusTarget());
        return distance > MIN_RANGE_SQUARED && distance <= MAX_RANGE_SQUARED && hasClearShot();
    }

    @Override
    public void start() {
        attackTime = 0;
        skeleton.getNavigation().stop();
        skeleton.setAggressive(true);
    }

    @Override
    public void stop() {
        skeleton.setAggressive(false);
    }

    @Override
    public void tick() {
        // Group merging clears the Nexus binding before the old running goals
        // are stopped at the end of the current AI tick.
        if (!skeleton.hasNexus()) {
            return;
        }
        Vec3 target = nexusTarget();
        skeleton.getLookControl().setLookAt(target.x, target.y, target.z);
        skeleton.getNavigatorNew().haltForTick();
        if (--attackTime <= 0) {
            skeleton.performRangedNexusAttack(target);
            attackTime = ATTACK_DELAY;
        }
    }

    private boolean hasClearShot() {
        Vec3 target = nexusTarget();
        BlockHitResult hit = skeleton.level().clip(new ClipContext(
                skeleton.getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                skeleton));
        return hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(skeleton.getNexus().getOrigin());
    }

    private Vec3 nexusTarget() {
        return PosUtils.center(skeleton.getNexus().getOrigin());
    }
}
