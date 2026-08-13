package com.invasion.entity;

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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class IMDrownedEntity extends EntityIMZombie
        implements RangedAttackMob {
    private boolean diving;

    public IMDrownedEntity(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
        moveControl = new DrownedMoveControl(this);
        setPathfindingMalus(PathType.WATER, 0.0F);
        setPathfindingMalus(PathType.LAVA, 0.0F);
        setFireImmune(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityIMZombie.createTierT1V0Attributes()
                .add(Attributes.MOVEMENT_SPEED, 0.17D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void initTieredAttributes() {
        super.initTieredAttributes();
        setBaseMovementSpeed(0.17D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
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
            EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        data = super.finalizeSpawn(world, difficulty, reason, data);
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
            Projectile.spawnProjectileUsingShoot(
                    trident, world, tridentStack,
                    x, y + horizontal * 0.2D, z,
                    1.6F, 14 - level().getDifficulty().getId() * 4);
        }
        playSound(
                SoundEvents.DROWNED_SHOOT, 1.0F,
                1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
    }

    public boolean wantsToSwim() {
        LivingEntity target = getTarget();
        return diving || hasNexus()
                || target != null && target.isInWater();
    }

    private final class DiveGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
        private static final int MAX_DIVE_TIME = 20 * 8;
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
            if (hasNexus()) {
                BlockPos nexus = getNexus().getOrigin();
                if (nexus.getY() >= getY() - 0.5D) {
                    return false;
                }
                destination = Vec3.atCenterOf(nexus);
                return true;
            }
            if (getRandom().nextInt(isUnderWater() ? 80 : 20) != 0) {
                return false;
            }
            destination = findDiveDestination();
            return destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            return diving && remainingTicks-- > 0 && isInWater()
                    && getTarget() == null
                    && distanceToSqr(destination) > 1.0D;
        }

        @Override
        public void start() {
            diving = true;
            remainingTicks = MAX_DIVE_TIME;
            moveControl.setWantedPosition(
                    destination.x, destination.y, destination.z, 1.0D);
        }

        @Override
        public void tick() {
            moveControl.setWantedPosition(
                    destination.x, destination.y, destination.z, 1.0D);
        }

        @Override
        public void stop() {
            diving = false;
            destination = null;
        }

        private Vec3 findDiveDestination() {
            BlockPos origin = blockPosition();
            RandomSource random = getRandom();
            for (int attempt = 0; attempt < 16; attempt++) {
                BlockPos candidate = origin.offset(
                        random.nextInt(11) - 5,
                        -(2 + random.nextInt(6)),
                        random.nextInt(11) - 5);
                if (level().getFluidState(candidate).is(FluidTags.WATER)
                        && level().getFluidState(candidate.above())
                                .is(FluidTags.WATER)) {
                    return Vec3.atCenterOf(candidate);
                }
            }
            return null;
        }
    }

    private static final class DrownedMoveControl
            extends MoveControl<IMDrownedEntity> {
        private DrownedMoveControl(IMDrownedEntity drowned) {
            super(drowned);
        }

        @Override
        public void tick() {
            if (!mob.isInWater()
                    || operation != Operation.MOVE_TO) {
                super.tick();
                return;
            }

            double x = wantedX - mob.getX();
            double y = wantedY - mob.getY();
            double z = wantedZ - mob.getZ();
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
            mob.setXRot(rotateTowards(
                    mob.getXRot(), Mth.clamp(targetPitch, -85.0F, 85.0F),
                    5.0F));

            float speed = (float) (speedModifier
                    * mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            mob.setSpeed(speed);
            float pitchRadians = mob.getXRot() * Mth.DEG_TO_RAD;
            mob.setZza(Mth.cos(pitchRadians) * speed);
            mob.setYya(-Mth.sin(pitchRadians) * speed);
        }
    }

    @Override
    protected void travelInWater(
            Vec3 movementInput, double gravity,
            boolean falling, double y) {
        if (isUnderWater() && wantsToSwim()) {
            moveRelative(0.01F, movementInput);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9D));
        } else {
            super.travelInWater(movementInput, gravity, falling, y);
        }
    }

    @Override
    public void updateSwimming() {
        if (!level().isClientSide()) {
            setSwimming(isEffectiveAi()
                    && isUnderWater() && wantsToSwim());
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

    @Override
    public TagKey<Item> getPreferredWeaponType() {
        return ItemTags.DROWNED_PREFERRED_WEAPONS;
    }
}
