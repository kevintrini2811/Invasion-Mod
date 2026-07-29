package com.invasion.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
            return Math.sqrt(pos.pos().getCenter().distanceToSqr(getX(), getY(0.5), getZ()));
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
        if (level().isClientSide || !hasNexus() || getNexus().getMode() == Mode.STOPPED) {
            return false;
        }

        return nexus.getPos().filter(center -> {
            IMWolfEntity wolf = InvEntities.WOLF.create(level());
            Optional<Vec3> respawnPoint = BlockPos.withinManhattanStream(center.pos(), 5, 3, 5).map(BlockPos::getBottomCenter)
                    .filter(pos -> {
                        wolf.setPos(pos);
                        return wolf.checkSpawnRules(level(), MobSpawnType.MOB_SUMMONED);
                    }).sorted(Comparator.comparingDouble(pos -> center.pos().distToLowCornerSqr(pos.x, pos.y, pos.z)))
                    .findAny();

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
            if (!level().isClientSide) {
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