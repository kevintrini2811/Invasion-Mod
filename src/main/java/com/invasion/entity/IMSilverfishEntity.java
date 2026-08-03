package com.invasion.entity;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import com.invasion.block.BlockMetadata;
import com.invasion.block.InvBlocks;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Silverfish support unit. It is registered but intentionally absent from waves. */
public final class IMSilverfishEntity extends Silverfish
        implements Combatant<Silverfish>, EntityConstruct.BuildableMob {
    public static final String INFECTED_TAG = "invmod.infected";
    private static final double SEARCH_RANGE = 16.0D;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMSilverfishEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TransformUnbreakableBlockGoal());
        goalSelector.addGoal(2, new InfectEntityGoal(true));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false));
        goalSelector.addGoal(4, new InfectEntityGoal(false));
        goalSelector.addGoal(5, new AttackNexusGoal());
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                (player, world) -> distanceToSqr(player) <= 8.0D * 8.0D));
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Silverfish asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMSilverfish-T1";
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        nexus.writeNbt(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nexus.readNbt(input);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !hasNexus() && super.removeWhenFarAway(distanceSquared);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }

    private void infect(LivingEntity target) {
        target.addTag(INFECTED_TAG);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.INFESTED,
                    target.getX(), target.getY(0.5D), target.getZ(),
                    12, 0.35D, 0.35D, 0.35D, 0.02D);
        }
        discard();
    }

    private final class TransformUnbreakableBlockGoal extends Goal {
        @Nullable private BlockPos target;
        private int searchCooldown;

        private TransformUnbreakableBlockGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(level() instanceof ServerLevel serverLevel)
                    || !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)
                    || searchCooldown-- > 0) {
                return false;
            }
            searchCooldown = 20;
            target = findClosestUnbreakableBlock(serverLevel);
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && isUnbreakableTarget(target);
        }

        @Override
        public void start() {
            getNavigation().moveTo(target.getX() + 0.5D,
                    target.getY() + 0.5D, target.getZ() + 0.5D, 1.2D);
        }

        @Override
        public void tick() {
            if (target == null) return;
            if (distanceToSqr(target.getX() + 0.5D,
                    target.getY() + 0.5D, target.getZ() + 0.5D) <= 4.0D) {
                level().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
                discard();
            } else if (getNavigation().isDone()) {
                start();
            }
        }

        @Nullable
        private BlockPos findClosestUnbreakableBlock(ServerLevel level) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.withinManhattan(
                    blockPosition(), 12, 6, 12)) {
                if (!isUnbreakableTarget(pos)) continue;
                double distance = pos.distSqr(blockPosition());
                if (distance < bestDistance) {
                    best = pos.immutable();
                    bestDistance = distance;
                }
            }
            return best;
        }

        private boolean isUnbreakableTarget(BlockPos pos) {
            BlockState state = level().getBlockState(pos);
            return !state.isAir()
                    && !state.is(InvBlocks.NEXUS_CORE)
                    && (state.getDestroySpeed(level(), pos) < 0.0F
                            || BlockMetadata.isIndestructible(state));
        }
    }

    private final class InfectEntityGoal extends Goal {
        private final boolean invasionMob;
        @Nullable private LivingEntity target;

        private InfectEntityGoal(boolean invasionMob) {
            this.invasionMob = invasionMob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            target = level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(SEARCH_RANGE), this::isCandidate)
                    .stream().min(java.util.Comparator.comparingDouble(
                            IMSilverfishEntity.this::distanceToSqr)).orElse(null);
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && isCandidate(target);
        }

        @Override
        public void start() {
            getNavigation().moveTo(target, 1.25D);
        }

        @Override
        public void tick() {
            if (target == null) return;
            if (distanceToSqr(target) <= 1.5D * 1.5D) {
                infect(target);
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(target, 1.25D);
            }
        }

        private boolean isCandidate(LivingEntity candidate) {
            if (candidate == IMSilverfishEntity.this
                    || candidate.entityTags().contains(INFECTED_TAG)
                    || candidate instanceof Player) {
                return false;
            }
            if (invasionMob) {
                return candidate instanceof Combatant<?>
                        && !(candidate instanceof IMSilverfishEntity)
                        && !(candidate instanceof IMWolfEntity);
            }
            return candidate instanceof IMWolfEntity
                    || candidate instanceof AbstractGolem
                    || candidate instanceof OwnableEntity ownable
                            && ownable.getRootOwner() instanceof Player;
        }
    }

    private final class AttackNexusGoal extends Goal {
        private int attackCooldown;

        private AttackNexusGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return hasNexus() && getNexus().isActive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            BlockPos pos = getNexus().getOrigin();
            if (distanceToSqr(pos.getX() + 0.5D,
                    pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 4.0D) {
                if (--attackCooldown <= 0) {
                    getNexus().damage(damageSources().mobAttack(
                            IMSilverfishEntity.this), 2);
                    attackCooldown = 20;
                }
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(pos.getX() + 0.5D,
                        pos.getY(), pos.getZ() + 0.5D, 1.1D);
            }
        }
    }
}
