package com.invasion.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.target.RetaliateGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.util.math.PosUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class IMZoglinEntity extends EntityIMLiving implements HoglinBase {
    private static final int DASH_COOLDOWN_TICKS = 600;
    private static final int STUCK_TICKS_REQUIRED = 60;
    private static final int DASH_DURATION_TICKS = 15;
    private static final double MIN_PROGRESS_SQUARED = 0.04D * 0.04D;
    private static final double DASH_SPEED = 0.75D;

    private int attackAnimationRemainingTicks;
    private boolean charging;
    private Vec3 lastProgressPosition;
    private Vec3 dashDirection = Vec3.ZERO;
    private LivingEntity dashTarget;
    private boolean hitDashTarget;
    private int stuckTicks;
    private int dashCooldown;
    private int dashTicks;

    public IMZoglinEntity(EntityType<? extends IMZoglinEntity> type, Level level) {
        super(type, level);
        lastProgressPosition = position();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(3, new MobMeleeAttackGoal(this, 1.4F, true));
        goalSelector.addGoal(6, new GoToNexusGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new RetaliateGoal(this));
        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(
                this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(
                this, IronGolem.class, this::getAggroRange, true));
    }

    @Override
    public void aiStep() {
        if (attackAnimationRemainingTicks > 0) {
            attackAnimationRemainingTicks--;
        }
        super.aiStep();
        if (level() instanceof ServerLevel serverLevel) {
            tickObstacleDash(serverLevel);
        }
    }

    private void tickObstacleDash(ServerLevel level) {
        if (dashTicks > 0) {
            getNavigation().stop();
            getLookControl().setLookAt(
                    getX() + dashDirection.x * 8.0D,
                    getEyeY(),
                    getZ() + dashDirection.z * 8.0D,
                    30.0F, 30.0F);
            setDeltaMovement(
                    dashDirection.x * DASH_SPEED,
                    getDeltaMovement().y,
                    dashDirection.z * DASH_SPEED);
            hurtMarked = true;
            destroyDashObstacles(level);
            hitDashTarget(level);
            dashTicks--;
            if (dashTicks == 0) {
                setCharging(false);
                dashCooldown = DASH_COOLDOWN_TICKS;
                dashTarget = null;
                lastProgressPosition = position();
            }
            return;
        }

        if (dashCooldown > 0) {
            dashCooldown--;
            lastProgressPosition = position();
            return;
        }
        if (isStunned() || !onGround()) {
            stuckTicks = 0;
            lastProgressPosition = position();
            return;
        }

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            double distance = distanceToSqr(target);
            if (distance >= 25.0D && distance <= 400.0D) {
                Vec3 direction = target.position().subtract(position())
                        .multiply(1.0D, 0.0D, 1.0D);
                if (direction.lengthSqr() >= 1.0E-4D) {
                    startDash(direction, target);
                    return;
                }
            }
        }
        if (!hasNexus()) {
            stuckTicks = 0;
            lastProgressPosition = position();
            return;
        }

        Vec3 current = position();
        double xProgress = current.x - lastProgressPosition.x;
        double zProgress = current.z - lastProgressPosition.z;
        lastProgressPosition = current;
        if (xProgress * xProgress + zProgress * zProgress < MIN_PROGRESS_SQUARED) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        if (stuckTicks < STUCK_TICKS_REQUIRED) {
            return;
        }

        Vec3 nexus = PosUtils.center(getNexus().getOrigin());
        Vec3 direction = nexus.subtract(current).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-4D) {
            stuckTicks = 0;
            return;
        }
        startDash(direction, null);
        stuckTicks = 0;
    }

    private void startDash(Vec3 direction, LivingEntity target) {
        dashDirection = direction.normalize();
        dashTarget = target;
        hitDashTarget = false;
        dashTicks = DASH_DURATION_TICKS;
        getNavigation().stop();
        setCharging(true);
    }

    private void hitDashTarget(ServerLevel level) {
        if (hitDashTarget || dashTarget == null || !dashTarget.isAlive()) {
            return;
        }
        if (getBoundingBox().inflate(0.35D).intersects(dashTarget.getBoundingBox())) {
            hitDashTarget = doHurtTarget(dashTarget);
        }
    }

    private void destroyDashObstacles(ServerLevel level) {
        Vec3 direction = getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        }
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }

        AABB dashBox = getBoundingBox().move(direction.normalize().scale(getBbWidth()));
        boolean brokeBlock = false;
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(dashBox.minX, dashBox.minY, dashBox.minZ),
                BlockPos.containing(dashBox.maxX, dashBox.maxY, dashBox.maxZ))) {
            if (!IMLandPathNodeMaker.canImpactDestroyBlock(this, pos)) {
                continue;
            }
            brokeBlock = true;
            level.destroyBlock(pos, InvasionMod.getConfig().destructedBlocksDrop);
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    4, 0.35D, 0.35D, 0.35D, 0.05D);
        }
        if (brokeBlock) {
            playSound(SoundEvents.GENERIC_EXPLODE, 0.2F, 0.5F);
        }
    }

    private void setCharging(boolean charging) {
        this.charging = charging;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof net.minecraft.world.entity.LivingEntity living)) {
            return false;
        }
        attackAnimationRemainingTicks = ATTACK_ANIMATION_DURATION;
        level().broadcastEntityEvent(this, (byte) 4);
        playSound(SoundEvents.ZOGLIN_ATTACK, 1.0F, getVoicePitch());
        return HoglinBase.hurtAndThrowTarget(this, living);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackAnimationRemainingTicks = ATTACK_ANIMATION_DURATION;
            playSound(SoundEvents.ZOGLIN_ATTACK, 1.0F, getVoicePitch());
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public int getAttackAnimationRemainingTicks() {
        return attackAnimationRemainingTicks;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOGLIN_DEATH;
    }
}
