package com.invasion.entity;

import net.minecraft.nbt.CompoundTag;
import com.invasion.entity.ai.goal.PredicatedGoal;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class IMDrownedEntity extends EntityIMZombie
        implements RangedAttackMob {
    private static final float UNDERWATER_SPEED_MULTIPLIER = 0.5F;
    private boolean diving;

    public IMDrownedEntity(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
        moveControl = new DrownedMoveControl(this);
        setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        setFireImmune(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityIMZombie.createTierT1V0Attributes()
                .add(Attributes.MOVEMENT_SPEED, 0.17D)
;
    }

    @Override
    protected void initTieredAttributes() {
        super.initTieredAttributes();
        setBaseMovementSpeed(0.17D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> goal instanceof FloatGoal);
        goalSelector.addGoal(1, new PredicatedGoal(
                new RangedAttackGoal(this, 1.0D, 40, 10.0F),
                () -> getMainHandItem().is(Items.TRIDENT)));
        goalSelector.addGoal(4, new DiveGoal());
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            solidifyLavaUnderfoot();
        }
        super.tick();
    }

    private void solidifyLavaUnderfoot() {
        BlockPos lavaPos = BlockPos.containing(
                getX(), getBoundingBox().minY - 0.01D, getZ());
        if (level().getFluidState(lavaPos).is(FluidTags.LAVA)) {
            level().setBlockAndUpdate(
                    lavaPos, Blocks.COBBLESTONE.defaultBlockState());
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor world, DifficultyInstance difficulty,
            MobSpawnType reason, @Nullable SpawnGroupData data,
            @Nullable CompoundTag entityTag) {
        data = super.finalizeSpawn(world, difficulty, reason, data, entityTag);
        RandomSource random = world.getRandom();
        if (getMainHandItem().isEmpty() && random.nextFloat() > 0.9F) {
            setItemSlot(
                    EquipmentSlot.MAINHAND,
                    new ItemStack(random.nextInt(16) < 10
                            ? Items.TRIDENT : Items.FISHING_ROD));
        }
        if (getOffhandItem().isEmpty() && random.nextFloat() < 0.03F) {
            setItemSlot(
                    EquipmentSlot.OFFHAND,
                    new ItemStack(Items.NAUTILUS_SHELL));
            setGuaranteedDrop(EquipmentSlot.OFFHAND);
        }
        return data;
    }

    @Override
    public void performRangedAttack(
            LivingEntity target, float pullProgress) {
        ItemStack held = getMainHandItem();
        ItemStack tridentStack = held.is(Items.TRIDENT)
                ? held : new ItemStack(Items.TRIDENT);
        ThrownTrident trident = new IMThrownTridentEntity(
                level(), this, tridentStack);
        double x = target.getX() - getX();
        double y = target.getY(0.3333333333333333D) - trident.getY();
        double z = target.getZ() - getZ();
        double horizontal = Math.sqrt(x * x + z * z);
        if (level() instanceof ServerLevel world) {
            trident.shoot(
                    x, y + horizontal * 0.2D, z,
                    1.6F, 14 - level().getDifficulty().getId() * 4);
            world.addFreshEntity(trident);
        }
        playSound(
                SoundEvents.DROWNED_SHOOT, 1.0F,
                1.0F / (random.nextFloat() * 0.4F + 0.8F));
    }

    public boolean wantsToSwim() {
        LivingEntity target = getTarget();
        return diving || hasNexus()
                || target != null && target.isInWater();
    }

    private final class DiveGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
        private static final int TARGET_REFRESH_INTERVAL = 20 * 2;
        private Vec3 destination;
        private int remainingTicks;

        private DiveGoal() {
            setFlags(EnumSet.of(
                    net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || !isInWater()) {
                return false;
            }
            destination = findDestination();
            return destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            return diving && isInWater() && getTarget() == null;
        }

        @Override
        public void start() {
            diving = true;
            remainingTicks = TARGET_REFRESH_INTERVAL;
            moveControl.setWantedPosition(
                    destination.x, destination.y, destination.z, 1.0D);
        }

        @Override
        public void tick() {
            if (--remainingTicks <= 0 || distanceToSqr(destination) < 2.0D) {
                Vec3 nextDestination = findDestination();
                if (nextDestination != null) {
                    destination = nextDestination;
                }
                remainingTicks = TARGET_REFRESH_INTERVAL;
            }
            moveControl.setWantedPosition(
                    destination.x, destination.y, destination.z, 1.0D);
        }

        @Override
        public void stop() {
            diving = false;
            destination = null;
        }

        private Vec3 findDestination() {
            if (!hasNexus()) {
                return findDiveDestination();
            }
            BlockPos nexus = getNexus().getOrigin();
            double nexusX = nexus.getX() + 0.5D;
            double nexusZ = nexus.getZ() + 0.5D;
            double horizontalDistanceSqr =
                    (nexusX - getX()) * (nexusX - getX())
                            + (nexusZ - getZ()) * (nexusZ - getZ());
            return new Vec3(
                    nexusX,
                    horizontalDistanceSqr <= 16.0D
                            ? nexus.getY() + 0.5D
                            : Math.min(nexus.getY() + 0.5D, getY() - 1.0D),
                    nexusZ);
        }

        private Vec3 findDiveDestination() {
            BlockPos origin = blockPosition();
            RandomSource random = getRandom();
            for (int depthPass = 2; depthPass >= 1; depthPass--) {
                for (int attempt = 0; attempt < 16; attempt++) {
                    int yOffset = isUnderWater()
                            ? random.nextInt(5) - 2
                            : -(2 + random.nextInt(6));
                    BlockPos candidate = origin.offset(
                            random.nextInt(13) - 6,
                            yOffset,
                            random.nextInt(13) - 6);
                    if (level().getFluidState(candidate).is(FluidTags.WATER)
                            && level().getFluidState(candidate.above(depthPass))
                                    .is(FluidTags.WATER)) {
                        return Vec3.atCenterOf(candidate);
                    }
                }
            }
            return null;
        }
    }

    @Override
    protected boolean canDigDown() {
        return !isInWater() && super.canDigDown();
    }

    private static final class DrownedMoveControl
            extends MoveControl {
        private final IMDrownedEntity drowned;
        private int shoreClimbTicks;
        private DrownedMoveControl(IMDrownedEntity drowned) {
            super(drowned);
            this.drowned = drowned;
        }

        @Override
        public void tick() {
            if (!mob.isInWater()) {
                super.tick();
                return;
            }

            double targetX = wantedX;
            double targetY = wantedY;
            double targetZ = wantedZ;
            double targetSpeedModifier = speedModifier;
            LivingEntity attackTarget = mob.getTarget();
            if (attackTarget != null && attackTarget.isInWater()) {
                targetX = attackTarget.getX();
                targetY = attackTarget.getY(0.5D);
                targetZ = attackTarget.getZ();
                targetSpeedModifier = 1.0D;
            } else if (operation != Operation.MOVE_TO) {
                if (!drowned.hasNexus()) {
                    super.tick();
                    return;
                }
                BlockPos nexus = drowned.getNexus().getOrigin();
                targetX = nexus.getX() + 0.5D;
                targetY = nexus.getY() + 0.5D;
                targetZ = nexus.getZ() + 0.5D;
                targetSpeedModifier = 1.0D;
            }

            double x = targetX - mob.getX();
            double y = targetY - mob.getY();
            double z = targetZ - mob.getZ();
            BlockPos nexusPos = drowned.hasNexus()
                    ? drowned.getNexus().getOrigin() : null;
            if (nexusPos != null && mob.horizontalCollision
                    && nexusPos.getY() + 0.5D > mob.getY()) {
                shoreClimbTicks = 20;
            }
            boolean climbingShore = nexusPos != null
                    && nexusPos.getY() + 0.5D > mob.getY()
                    && shoreClimbTicks-- > 0;
            if (climbingShore && mob.isUnderWater()) {
                // Swim up beside the shore instead of continuously pushing
                // into its submerged wall.
                x = 0.0D;
                z = 0.0D;
                y = Math.max(
                        nexusPos.getY() + 0.5D - mob.getY(), 1.0D);
            } else if (climbingShore) {
                // Once its head reaches the surface, keep pushing toward the
                // real Nexus instead of an underwater land-path waypoint.
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

            float targetYaw = (float) (Mth.atan2(z, x)
                    * Mth.RAD_TO_DEG) - 90.0F;
            mob.setYRot(rotlerp(mob.getYRot(), targetYaw, 10.0F));
            mob.yBodyRot = mob.getYRot();

            double horizontalDistance = Math.sqrt(x * x + z * z);
            float targetPitch = -((float) (Mth.atan2(y, horizontalDistance)
                    * Mth.RAD_TO_DEG));
            mob.setXRot(Mth.rotLerp(
                    0.2F, mob.getXRot(),
                    Mth.clamp(targetPitch, -85.0F, 85.0F)));

            float speed = (float) (targetSpeedModifier
                    * mob.getAttributeValue(Attributes.MOVEMENT_SPEED))
                    * UNDERWATER_SPEED_MULTIPLIER;
            mob.setSpeed(speed);
            float pitchRadians = mob.getXRot() * Mth.DEG_TO_RAD;
            mob.setZza(Mth.cos(pitchRadians) * speed);
            mob.setYya(-Mth.sin(pitchRadians) * speed);
            double verticalSpeed = Mth.clamp(y * 0.1D, -speed, speed);
            double horizontalScale = horizontalDistance > 1.0E-5D
                    ? speed / horizontalDistance : 0.0D;
            Vec3 movement = mob.getDeltaMovement();
            mob.setDeltaMovement(
                    Mth.lerp(0.2D, movement.x, x * horizontalScale),
                    climbingShore && !mob.isUnderWater()
                            ? Math.max(movement.y, 0.3D)
                            : Mth.lerp(0.2D, movement.y, verticalSpeed),
                    Mth.lerp(0.2D, movement.z, z * horizontalScale));
        }
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (isInWater() && wantsToSwim()) {
            moveRelative(0.01F, movementInput);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9D));
        } else {
            super.travel(movementInput);
        }
    }

    @Override
    public void updateSwimming() {
        if (!level().isClientSide()) {
            setSwimming(isEffectiveAi()
                    && isInWater() && wantsToSwim());
        }
    }

    @Override
    public boolean isVisuallySwimming() {
        return isSwimming() && !isPassenger();
    }

    @Override
    public boolean isPushedByFluid() {
        return !isSwimming();
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return isInWater()
                ? SoundEvents.DROWNED_AMBIENT_WATER
                : SoundEvents.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(
            net.minecraft.world.damagesource.DamageSource source) {
        return isInWater()
                ? SoundEvents.DROWNED_HURT_WATER
                : SoundEvents.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isInWater()
                ? SoundEvents.DROWNED_DEATH_WATER
                : SoundEvents.DROWNED_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.DROWNED_STEP, 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DROWNED_SWIM;
    }

}
