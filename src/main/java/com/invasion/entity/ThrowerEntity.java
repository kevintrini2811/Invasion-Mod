package com.invasion.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import com.invasion.Notifiable;
import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.item.InvItems;
import com.invasion.block.InvBlocks;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.ThrowBoulderGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.ThrowerKillEntityGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;

public class ThrowerEntity extends TieredIMMobEntity {
    private static final EntityDataAccessor<Integer> THROW_ANIMATION_TICKS =
            SynchedEntityData.defineId(ThrowerEntity.class, EntityDataSerializers.INT);
    private static final int THROW_ANIMATION_DURATION = 12;

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        spawnAtLocation(level, InvItems.SMALL_REMNANTS);
    }

    private int throwTime;
    private int punchTimer;

    private int blockBreakSoundCooldown;
    private int nexusPunchCooldown;

    private BlockPos pointToClear;

    private Notifiable clearPointNotifee;

    private float launchSpeed = 1;

    public ThrowerEntity(EntityType<ThrowerEntity> type, Level world) {
        super(type, world);
        xpReward = 20;
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(THROW_ANIMATION_TICKS, 0);
    }

    public static AttributeSupplier.Builder createT1V0Attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.13F)
                .add(Attributes.ATTACK_DAMAGE, 10);
    }

    public static AttributeSupplier.Builder createT2V0Attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.23F)
                .add(Attributes.ATTACK_DAMAGE, 15);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PredicatedGoal(new ThrowerKillEntityGoal<>(this, LivingEntity.class, 55, 60.0F, 1.0F), () -> getTier() == 1));
        goalSelector.addGoal(1, new PredicatedGoal(new ThrowerKillEntityGoal<>(this, LivingEntity.class, 60, 90.0F, 1.5F), () -> getTier() == 2));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(3, new ThrowBoulderGoal(this, 3));
        goalSelector.addGoal(4, new GoToNexusGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 16));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        throwTime--;
        int animationTicks = getThrowAnimationTicks();
        if (animationTicks > 0) {
            entityData.set(THROW_ANIMATION_TICKS, animationTicks - 1);
            getNavigation().stop();
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(0.0D, movement.y, 0.0D);
        }
        if (blockBreakSoundCooldown > 0) {
            blockBreakSoundCooldown--;
        }
        if (nexusPunchCooldown > 0) {
            nexusPunchCooldown--;
        }
        if (pointToClear != null && clearPoint()) {
            pointToClear = null;
            if (clearPointNotifee != null) {
                clearPointNotifee.notifyTask(Notifiable.Status.SUCCESS);
                clearPointNotifee = null;
            }
        }
    }

    @Override
    public void knockback(double strength, double x, double z, DamageSource source, float damage, boolean force) {
        if (getTier() != 2) {
            super.knockback(strength, x, z, source, damage, force);
        }
    }

    public boolean canThrow() {
        return throwTime <= 0;
    }

    public int getThrowAnimationTicks() {
        return entityData.get(THROW_ANIMATION_TICKS);
    }

    public boolean isThrowing() {
        return getThrowAnimationTicks() > 0;
    }

    @Override
    public boolean onPathBlocked(Path path, Notifiable notifee) {
        if (!path.isDone()) {
            clearPointNotifee = notifee;
            pointToClear = path.getNextNodePos();
            return true;
        }
        return false;
    }

    @Override
    protected Component getTypeName() {
        if (isBig()) {
            return Component.translatable("entity.invmod.big_thrower");
        }
        return super.getTypeName();
    }

    public boolean isBig() {
        return getTier() == 2;
    }

    @Override
    public void initTieredAttributes() {
        pointToClear = null;
        if (isBig()) {
            setBaseMovementSpeed(0.23F);
            setAttackStrength(15);
            xpReward = 25;
        } else {
            setBaseMovementSpeed(0.13F);
            setAttackStrength(10);
            xpReward = 20;
        }
    }

    @Override
    public float getAgeScale() {
        if (isBig()) {
            return 1.1F;
        }
        return super.getAgeScale();
    }

    public float scaleAmount() {
        return super.getScale() * getAgeScale();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    protected boolean clearPoint() {
        if (--punchTimer <= 0) {
            int xOffsetR = 0;
            int zOffsetR = 0;
            int axisX = 0;
            int axisZ = 0;

            float facing = Mth.wrapDegrees(getYRot());

            if (facing >= 45 && facing < 135) {
                zOffsetR = -1;
                axisX = -1;
            } else if (facing >= 135 && facing < 225) {
                xOffsetR = -1;
                axisZ = -1;
            } else if (facing >= 225 && facing < 315) {
                zOffsetR = -1;
                axisX = 1;
            } else {
                xOffsetR = -1;
                axisZ = 1;
            }
            // this is a cheat, I should fix it where it get's the point to clear
            BlockPos targetPos = pointToClear.below();
            List<BlockPos> wideArea = List.of(
                    targetPos,
                    pointToClear,
                    targetPos.offset(xOffsetR, 0, zOffsetR),
                    pointToClear.offset(xOffsetR, 0, zOffsetR)
            );
            List<BlockPos> narrowArea = List.of(
                    pointToClear.offset(-axisX, 0, -axisZ),
                    pointToClear.offset(-axisX, 0, -axisZ).offset(xOffsetR, 0, zOffsetR)
            );
            List<BlockPos> singleTarget = List.of(
                    pointToClear.offset(-2 * axisX, 0, -2 * axisZ),
                    pointToClear.offset(-2 * axisX, 0, -2 * axisZ).offset(xOffsetR, 0, zOffsetR)
            );

            if (tryDestroyArea(wideArea) || tryDestroyArea(narrowArea) || tryDestroyArea(singleTarget)) {
                punchTimer = 160;
            } else {
                return true;
            }
        }
        return false;
    }

    protected final boolean tryDestroyArea(List<BlockPos> positions) {
        if (positions.stream().anyMatch(pos -> level().getBlockState(pos).isRedstoneConductor(level(), pos))) {
            positions.forEach(this::tryDestroyBlock);
            return true;
        }
        return false;
    }

    protected void tryDestroyBlock(BlockPos pos) {
        BlockState block = level().getBlockState(pos);
        if (block.isAir()) {
            return;
        }
        if (block.is(InvBlocks.NEXUS_CORE)) {
            if (hasNexus() && canAttack() && pos.equals(getNexus().getOrigin())) {
                getNexus().damage(damageSources().mobAttack(this), 5);
                nexusPunchCooldown = 60;
            }
        } else {
            level().destroyBlock(pos, InvasionMod.getConfig().destructedBlocksDrop);
            if (blockBreakSoundCooldown == 0) {
                playSound(InvSounds.ENTITY_THROWER_RAGE, 1, 0.4F);
                blockBreakSoundCooldown = 5;
            }
        }
    }

    public boolean canAttack() {
        return nexusPunchCooldown <= 0;
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity entity) {
        float distance = entity.distanceTo(this);
        if (throwTime <= 0 && distance > 4) {
            throwTime = 120;
            if (distance < 50) {
                if (canAttack()) {
                    throwProjectile(entity.getEyePosition(), createProjectile(0));
                }
                throwTime = 120;
                return true;
            }
        }
        return super.doHurtTarget(serverLevel, entity);
    }

    public float getLaunchSpeed() {
        return launchSpeed;
    }

    public AbstractArrow createProjectile(int tier) {
        return tier == 2 ? InvEntities.TNT.create(level(), EntitySpawnReason.EVENT) : InvEntities.BOULDER.create(level(), EntitySpawnReason.EVENT);
    }

    public void throwProjectile(Vec3 targetPosition) {
        throwProjectile(targetPosition, createProjectile(getTier()));
    }

    public void throwProjectile(Vec3 targetPosition, AbstractArrow projectile) {
        throwProjectile(targetPosition, projectile, getLaunchSpeed());
    }

    public void throwProjectile(Vec3 targetPosition, AbstractArrow projectile, float speed) {
        this.throwTime = 40;
        entityData.set(THROW_ANIMATION_TICKS, THROW_ANIMATION_DURATION);
        getNavigation().stop();
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(0.0D, movement.y, 0.0D);
        Vec3 eyePos = getEyePosition();
        Vec3 delta = targetPosition.subtract(eyePos);
        double dXZ = delta.horizontalDistance();

        projectile.setOwner(this);
        projectile.setPos(eyePos);
        projectile.shoot(delta.x, delta.y + (dXZ * Math.tan(getThrowAngle(dXZ, speed))), delta.z, speed, 0.05F);
        level().addFreshEntity(projectile);
    }

    private double getThrowAngle(double horDifference) {
        return getThrowAngle(horDifference, getLaunchSpeed());
    }

    private double getThrowAngle(double horDifference, float speed) {
        double p = getThrowPower(horDifference, speed);
        return p <= 1 ? 0.5D * Math.asin(p) : 0.7853981633974483D;
    }

    public double getThrowPower(double horDifference) {
        return getThrowPower(horDifference, getLaunchSpeed());
    }

    public double getThrowPower(double horDifference, float speed) {
        return 0.025D * horDifference / Mth.square(speed);
    }
}
