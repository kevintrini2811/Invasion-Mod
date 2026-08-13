package com.invasion.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Shared three-dimensional lava movement for the two IM piglin families. */
final class LavaSwimmingBehavior<T extends PathfinderMob & NexusEntity> {
    private final T mob;
    private boolean diving;

    LavaSwimmingBehavior(T mob) {
        this.mob = mob;
    }

    MoveControl<T> createMoveControl() {
        return new LavaMoveControl();
    }

    Goal createDiveGoal() {
        return new DiveGoal();
    }

    boolean wantsToSwim() {
        LivingEntity target = mob.getTarget();
        return diving || mob.hasNexus()
                || target != null && target.isInLava();
    }

    private final class DiveGoal extends Goal {
        private static final int TARGET_REFRESH_INTERVAL = 20 * 2;
        private Vec3 destination;
        private int remainingTicks;

        private DiveGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || !mob.isInLava()) {
                return false;
            }
            destination = findDestination();
            return destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            return diving && mob.isInLava() && mob.getTarget() == null;
        }

        @Override
        public void start() {
            diving = true;
            remainingTicks = TARGET_REFRESH_INTERVAL;
            moveToDestination();
        }

        @Override
        public void tick() {
            if (--remainingTicks <= 0 || mob.distanceToSqr(destination) < 2.0D) {
                Vec3 nextDestination = findDestination();
                if (nextDestination != null) {
                    destination = nextDestination;
                }
                remainingTicks = TARGET_REFRESH_INTERVAL;
            }
            moveToDestination();
        }

        @Override
        public void stop() {
            diving = false;
            destination = null;
        }

        private void moveToDestination() {
            mob.getMoveControl().setWantedPosition(
                    destination.x, destination.y, destination.z, 1.0D);
        }

        private Vec3 findDestination() {
            if (!mob.hasNexus()) {
                return findDiveDestination();
            }
            BlockPos nexus = mob.getNexus().getOrigin();
            double nexusX = nexus.getX() + 0.5D;
            double nexusZ = nexus.getZ() + 0.5D;
            double horizontalDistanceSqr =
                    Mth.square(nexusX - mob.getX())
                            + Mth.square(nexusZ - mob.getZ());
            return new Vec3(
                    nexusX,
                    horizontalDistanceSqr <= 16.0D
                            ? nexus.getY() + 0.5D
                            : Math.min(nexus.getY() + 0.5D, mob.getY() - 1.0D),
                    nexusZ);
        }

        private Vec3 findDiveDestination() {
            BlockPos origin = mob.blockPosition();
            RandomSource random = mob.getRandom();
            for (int depthPass = 2; depthPass >= 1; depthPass--) {
                for (int attempt = 0; attempt < 16; attempt++) {
                    int yOffset = mob.isEyeInFluid(FluidTags.LAVA)
                            ? random.nextInt(5) - 2
                            : -(2 + random.nextInt(6));
                    BlockPos candidate = origin.offset(
                            random.nextInt(13) - 6,
                            yOffset,
                            random.nextInt(13) - 6);
                    if (mob.level().getFluidState(candidate).is(FluidTags.LAVA)
                            && mob.level().getFluidState(candidate.above(depthPass))
                                    .is(FluidTags.LAVA)) {
                        return Vec3.atCenterOf(candidate);
                    }
                }
            }
            return null;
        }
    }

    private final class LavaMoveControl extends MoveControl<T> {
        private static final float LAVA_SPEED_MULTIPLIER = 2.0F;
        private int shoreClimbTicks;

        private LavaMoveControl() {
            super(LavaSwimmingBehavior.this.mob);
        }

        @Override
        public void tick() {
            if (!mob.isInLava()) {
                super.tick();
                return;
            }

            double targetX = wantedX;
            double targetY = wantedY;
            double targetZ = wantedZ;
            double targetSpeedModifier = speedModifier;
            LivingEntity attackTarget = mob.getTarget();
            if (attackTarget != null && attackTarget.isInLava()) {
                targetX = attackTarget.getX();
                targetY = attackTarget.getY(0.5D);
                targetZ = attackTarget.getZ();
                targetSpeedModifier = 1.0D;
            } else if (operation != Operation.MOVE_TO) {
                if (!mob.hasNexus()) {
                    super.tick();
                    return;
                }
                BlockPos nexus = mob.getNexus().getOrigin();
                targetX = nexus.getX() + 0.5D;
                targetY = nexus.getY() + 0.5D;
                targetZ = nexus.getZ() + 0.5D;
                targetSpeedModifier = 1.0D;
            }

            double x = targetX - mob.getX();
            double y = targetY - mob.getY();
            double z = targetZ - mob.getZ();
            BlockPos nexusPos = mob.hasNexus()
                    ? mob.getNexus().getOrigin() : null;
            if (nexusPos != null && mob.horizontalCollision
                    && nexusPos.getY() + 0.5D > mob.getY()) {
                shoreClimbTicks = 20;
            }
            boolean climbingShore = nexusPos != null
                    && nexusPos.getY() + 0.5D > mob.getY()
                    && shoreClimbTicks-- > 0;
            if (climbingShore && mob.isEyeInFluid(FluidTags.LAVA)) {
                x = 0.0D;
                z = 0.0D;
                y = Math.max(nexusPos.getY() + 0.5D - mob.getY(), 1.0D);
            } else if (climbingShore) {
                x = nexusPos.getX() + 0.5D - mob.getX();
                y = nexusPos.getY() + 0.5D - mob.getY();
                z = nexusPos.getZ() + 0.5D - mob.getZ();
            }

            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < 1.0E-5D) {
                mob.setSpeed(0.0F);
                mob.setXxa(0.0F);
                mob.setYya(0.0F);
                mob.setZza(0.0F);
                return;
            }

            float targetYaw = (float) (Mth.atan2(z, x) * Mth.RAD_TO_DEG) - 90.0F;
            mob.setYRot(rotlerp(mob.getYRot(), targetYaw, 10.0F));
            mob.yBodyRot = mob.getYRot();
            double horizontalDistance = Math.sqrt(x * x + z * z);
            float targetPitch = -((float) (Mth.atan2(y, horizontalDistance)
                    * Mth.RAD_TO_DEG));
            mob.setXRot(rotateTowards(
                    mob.getXRot(), Mth.clamp(targetPitch, -85.0F, 85.0F), 5.0F));

            float speed = (float) (targetSpeedModifier
                    * mob.getAttributeValue(Attributes.MOVEMENT_SPEED))
                    * LAVA_SPEED_MULTIPLIER;
            mob.setSpeed(speed);
            float pitchRadians = mob.getXRot() * Mth.DEG_TO_RAD;
            mob.setZza(Mth.cos(pitchRadians) * speed);
            mob.setYya(-Mth.sin(pitchRadians) * speed);
            double verticalSpeed = Mth.clamp(y * 0.1D, -speed, speed);
            Vec3 movement = mob.getDeltaMovement();
            mob.setDeltaMovement(
                    movement.x,
                    climbingShore && !mob.isEyeInFluid(FluidTags.LAVA)
                            ? Math.max(movement.y, 0.3D)
                            : Mth.lerp(0.2D, movement.y, verticalSpeed),
                    movement.z);
        }
    }
}
