package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Caches the expensive vertical clearance scan used by flying invasion mobs. */
public final class FlyingWallPath {
    private static final int WALL_SCAN_INTERVAL = 20;
    private static final double CROSSING_REACHED_DISTANCE_SQUARED = 1.5D * 1.5D;

    private final Mob mob;
    @Nullable
    private Vec3 crossingTarget;
    @Nullable
    private BlockPos cachedWall;
    @Nullable
    private Integer cachedClearanceY;
    private int nextWallScanTick;

    public FlyingWallPath(Mob mob) {
        this.mob = mob;
    }

    public Vec3 findTarget(Vec3 objective, @Nullable BlockPos ignoredHit) {
        if (crossingTarget != null) {
            double horizontalDistanceSquared = mob.distanceToSqr(
                    crossingTarget.x, mob.getY(), crossingTarget.z);
            if (horizontalDistanceSquared > CROSSING_REACHED_DISTANCE_SQUARED
                    || mob.getY() < crossingTarget.y - 1.5D) {
                return crossingTarget;
            }
            crossingTarget = null;
        }

        Vec3 horizontalTarget = new Vec3(objective.x, mob.getEyeY(), objective.z);
        BlockHitResult hit = mob.level().clip(new ClipContext(
                mob.getEyePosition(), horizontalTarget, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mob));
        if (hit.getType() != HitResult.Type.BLOCK
                || ignoredHit != null && hit.getBlockPos().equals(ignoredHit)) {
            clearCache();
            return objective.add(0.0D, 2.0D, 0.0D);
        }

        BlockPos wall = hit.getBlockPos();
        if (!wall.equals(cachedWall) || mob.tickCount >= nextWallScanTick) {
            cachedWall = wall.immutable();
            cachedClearanceY = findClearanceY(wall);
            nextWallScanTick = mob.tickCount + WALL_SCAN_INTERVAL;
        }
        if (cachedClearanceY == null) {
            return objective.add(0.0D, 2.0D, 0.0D);
        }

        Vec3 acrossWall = objective.subtract(Vec3.atCenterOf(wall))
                .multiply(1.0D, 0.0D, 1.0D);
        if (acrossWall.lengthSqr() > 0.0D) {
            acrossWall = acrossWall.normalize().scale(4.0D);
        }
        crossingTarget = new Vec3(
                wall.getX() + 0.5D + acrossWall.x,
                cachedClearanceY + 1.0D,
                wall.getZ() + 0.5D + acrossWall.z);
        return crossingTarget;
    }

    public void reset() {
        crossingTarget = null;
        clearCache();
    }

    @Nullable
    private Integer findClearanceY(BlockPos wall) {
        int startY = Math.max(wall.getY() + 1, mob.blockPosition().getY());
        for (int y = startY; y < mob.level().getMaxY() - 1; y++) {
            BlockPos lower = new BlockPos(wall.getX(), y, wall.getZ());
            BlockPos upper = lower.above();
            if (mob.level().getBlockState(lower)
                            .getCollisionShape(mob.level(), lower).isEmpty()
                    && mob.level().getBlockState(upper)
                            .getCollisionShape(mob.level(), upper).isEmpty()) {
                return y;
            }
        }
        return null;
    }

    private void clearCache() {
        cachedWall = null;
        cachedClearanceY = null;
        nextWallScanTick = 0;
    }
}
