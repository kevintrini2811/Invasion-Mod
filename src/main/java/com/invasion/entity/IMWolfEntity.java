package com.invasion.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueOutput;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.item.InvItems;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

public class IMWolfEntity extends Wolf implements IHasNexus {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

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
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public double findDistanceToNexus() {
        return nexus.getPos().map(pos -> {
            return Math.sqrt(com.invasion.util.math.PosUtils.center(pos.pos()).distanceToSqr(getX(), getY(0.5), getZ()));
        }).orElse(Double.MAX_VALUE);
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
        boolean success = super.doHurtTarget(serverLevel, target);
        if (success) {
            heal(4);
        }
        return success;
    }

    @Override
    protected void applyTamingSideEffects() {
        super.applyTamingSideEffects();
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(isTame() ? 25 : 8);
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
            IMWolfEntity wolf = InvEntities.WOLF.create(
                    world, EntitySpawnReason.EVENT);
            if (wolf == null) {
                return false;
            }

            wolf.restoreFrom(this);
            wolf.setUUID(UUID.randomUUID());
            wolf.setNexus(getNexus());
            wolf.setHealth(wolf.getMaxHealth());

            Optional<Vec3> respawnPoint = BlockPos
                    .withinManhattanStream(center.pos(), 5, 3, 5)
                    .sorted(java.util.Comparator.comparingDouble(
                            center.pos()::distSqr))
                    .map(Vec3::atBottomCenterOf)
                    .filter(pos -> {
                        wolf.setPos(pos);
                        BlockPos feet = BlockPos.containing(pos);
                        BlockPos ground = feet.below();
                        return world.getBlockState(ground)
                                        .isCollisionShapeFullBlock(
                                                world, ground)
                                && world.noCollision(wolf);
                    })
                    .findFirst();

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

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(InvItems.STRANGE_BONE) && isOwnedBy(player)) {
            if (!level().isClientSide()) {
                NexusAccess newNexus = IHasNexus.findNexus(level(), blockPosition());
                if (newNexus != null && newNexus != getNexus()) {
                    setNexus(newNexus);
                    stack.consume(1, player);
                    setHealth(25);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        nexus.writeNbt(compound);
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        nexus.readNbt(compound);
    }
}
