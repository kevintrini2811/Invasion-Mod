package com.invasion.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.item.InvItems;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

public class IMWolfEntity extends Wolf implements IHasNexus {
    private static final double BASE_ATTACK_DAMAGE = 4.0D;
    private static final double TAMED_BASE_HEALTH = 25.0D;
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private int appliedWave = -1;

    public IMWolfEntity(EntityType<IMWolfEntity> type, Level world) {
        this(type, world, null);
    }

    public IMWolfEntity(EntityType<IMWolfEntity> type, Level world, @Nullable NexusAccess nexus) {
        super(type, world);
        setNexus(nexus);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> goal instanceof FollowOwnerGoal);
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(
                this,
                LivingEntity.class,
                10,
                true,
                false,
                target -> target instanceof Combatant<?>
                        && !(target instanceof IMWolfEntity)));
    }

    @Override
    public Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public boolean shouldTryTeleportToOwner() {
        return false;
    }

    @Override
    public double findDistanceToNexus() {
        return nexus.getPos().map(pos -> {
            return Math.sqrt(com.invasion.util.math.PosUtils.center(pos.pos()).distanceToSqr(getX(), getY(0.5), getZ()));
        }).orElse(Double.MAX_VALUE);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean success = super.doHurtTarget(target);
        if (success) {
            heal(4);
        }
        return success;
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        updateWaveAttributes();
    }

    private void updateWaveAttributes() {
        int wave = hasNexus()
                ? Math.max(0, getNexus().getProgressionLevel()) / 2
                : 0;
        double desiredHealth = (isTame() ? TAMED_BASE_HEALTH : 8.0D) + wave;
        double desiredDamage = BASE_ATTACK_DAMAGE + wave;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        var attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);

        if (appliedWave == wave
                && maxHealth.getBaseValue() == desiredHealth
                && attackDamage.getBaseValue() == desiredDamage) {
            return;
        }

        float missingHealth = getMaxHealth() - getHealth();
        maxHealth.setBaseValue(desiredHealth);
        attackDamage.setBaseValue(desiredDamage);
        if (isAlive()) {
            setHealth(Math.max(
                    1.0F, (float) desiredHealth - missingHealth));
        }
        appliedWave = wave;
    }

    @Override
    protected void applyTamingSideEffects() {
        super.applyTamingSideEffects();
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                isTame() ? TAMED_BASE_HEALTH : 8);
        appliedWave = -1;
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    protected void tickDeath() {
        if (++deathTime >= 120) {
            level().broadcastEntityEvent(this, EntityEvent.POOF);
            remove(Entity.RemovalReason.KILLED);
            for (int j = 0; j < 20; j++) {
                level().addParticle(ParticleTypes.EXPLOSION,
                        getRandomX(2),
                        getRandomY(),
                        getRandomZ(2),
                        getRandom().nextGaussian() * 0.02D,
                        getRandom().nextGaussian() * 0.02D,
                        getRandom().nextGaussian() * 0.02D
                );
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!respawnAtNexus()) {
            super.die(source);
        }
    }

    public boolean respawnAtNexus() {
        if (!(level() instanceof ServerLevel world)
                || !hasNexus()
                || !getNexus().isActive()) {
            return false;
        }

        return nexus.getPos().filter(center -> {
            IMWolfEntity wolf = InvEntities.WOLF.create(world);
            if (wolf == null) {
                return false;
            }

            wolf.restoreFrom(this);
            wolf.setUUID(UUID.randomUUID());
            wolf.setNexus(getNexus());
            wolf.setHealth(wolf.getMaxHealth());

            Optional<Vec3> respawnPoint = findWolfPositionAtNexus(
                    world, wolf, center.pos());

            if (respawnPoint.isPresent()) {
                wolf.setPos(respawnPoint.get());
                wolf.setRot(0, 0);
                if (!world.addFreshEntity(wolf)) {
                    InvasionMod.LOGGER.warn(
                            "Failed to add respawned wolf at Nexus");
                    return false;
                }
                if (!isRemoved()) {
                    discard();
                }
                return true;
            }
            InvasionMod.LOGGER.warn("No respawn spot for wolf");
            return false;
        }).isPresent();
    }

    private Optional<Vec3> findWolfPositionAtNexus(
            ServerLevel world, IMWolfEntity wolf, BlockPos nexusPos) {
        Optional<Vec3> position = BlockPos
                    .betweenClosedStream(
                            new BlockPos(
                                    nexusPos.getX() - 5,
                                    world.getMinBuildHeight(),
                                    nexusPos.getZ() - 5),
                            new BlockPos(
                                    nexusPos.getX() + 5,
                                    world.getMaxBuildHeight() - 2,
                                    nexusPos.getZ() + 5))
                    .sorted(java.util.Comparator.comparingDouble(
                            nexusPos::distSqr))
                    .map(ground -> getWolfRespawnPoint(
                            world, wolf, ground))
                    .flatMap(Optional::stream)
                    .findFirst();

        if (position.isPresent()) {
            return position;
        }
        BlockPos surface = world.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                nexusPos);
        return getWolfRespawnPoint(world, wolf, surface.below());
    }

    private Optional<Vec3> getWolfRespawnPoint(
            ServerLevel world, IMWolfEntity wolf, BlockPos ground) {
        var groundShape = world.getBlockState(ground)
                .getCollisionShape(world, ground);
        if (groundShape.isEmpty()) {
            return Optional.empty();
        }

        Vec3 position = new Vec3(
                ground.getX() + 0.5D,
                ground.getY() + groundShape.max(Direction.Axis.Y),
                ground.getZ() + 0.5D);
        var targetBox = wolf.getBoundingBox().move(
                position.subtract(wolf.position()));

        // Other mobs crowding the Nexus must not prevent a bound wolf from
        // returning. Block collision still guarantees enough physical space;
        // ordinary entity pushing separates overlapping mobs afterwards.
        return world.noBlockCollision(wolf, targetBox)
                ? Optional.of(position)
                : Optional.empty();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(InvItems.STRANGE_BONE) && isOwnedBy(player)) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (level() instanceof ServerLevel world
                    && hasNexus()
                    && getNexus().isActive()) {
                Optional<Vec3> destination = findWolfPositionAtNexus(
                        world, this, getNexus().getOrigin());
                if (destination.isPresent()) {
                    Vec3 pos = destination.get();
                    getNavigation().stop();
                    setTarget(null);
                    teleportTo(pos.x, pos.y, pos.z);
                    stack.consume(1, player);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.FAIL;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        nexus.writeNbt(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        nexus.readNbt(compound);
    }
}
