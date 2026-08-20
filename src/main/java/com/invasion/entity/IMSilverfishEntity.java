package com.invasion.entity;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import com.invasion.block.BlockMetadata;
import com.invasion.block.InvBlocks;
import com.invasion.Notifiable;
import com.invasion.entity.ai.builder.ModifyBlockEntry;
import com.invasion.entity.ai.builder.TerrainModifier;
import com.invasion.entity.pathfinding.BuilderIMMobNavigation;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Silverfish support unit. It is registered but intentionally absent from waves. */
public final class IMSilverfishEntity extends Silverfish
        implements NexusEntity {
    public static final String INFECTED_TAG = "invmod.infected";
    private static final double SEARCH_RANGE = 16.0D;
    private static final int BRIDGE_BUILD_TIME = 45;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private final TerrainModifier terrainModifier = new TerrainModifier(this, 4.5F);

    public IMSilverfishEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BuilderIMMobNavigation(this);
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
    public void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        terrainModifier.onUpdate();
    }

    @Override
    public boolean handlePathAction(
            BlockPos feetPos, PathAction action, Notifiable asker) {
        if (action.getType() != PathAction.Type.BRIDGE) {
            return false;
        }

        BlockState feetState = level().getBlockState(feetPos);
        BlockPos placementPos = feetState.getFluidState().isEmpty()
                ? feetPos.below()
                : feetPos;
        BlockState replacedState = level().getBlockState(placementPos);
        if (!replacedState.isAir()
                && replacedState.getFluidState().isEmpty()) {
            return false;
        }

        var movement = getDeltaMovement();
        setXxa(0);
        setZza(0);
        setSpeed(0);
        setDeltaMovement(0, movement.y, 0);
        return terrainModifier.requestTask(
                java.util.List.of(new ModifyBlockEntry(
                        placementPos,
                        Blocks.STONE.defaultBlockState(),
                        BRIDGE_BUILD_TIME)),
                status -> {
                    asker.notifyTask(status);
                    if (status == Notifiable.Status.SUCCESS
                            && level() instanceof ServerLevel level) {
                        kill(level);
                    }
                },
                null);
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

    private final class TransformUnbreakableBlockGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
        private static final int HORIZONTAL_SEARCH_RANGE = 12;
        private static final int VERTICAL_SEARCH_RANGE = 6;
        private static final int SAMPLES_PER_TICK = 64;
        private static final int MIN_SEARCH_TICKS = 20;
        private static final int SEARCH_WIDTH = HORIZONTAL_SEARCH_RANGE * 2 + 1;
        private static final int SEARCH_HEIGHT = VERTICAL_SEARCH_RANGE * 2 + 1;
        private static final int SEARCH_VOLUME = SEARCH_WIDTH * SEARCH_WIDTH
                * SEARCH_HEIGHT;
        private static final int MAX_SEARCH_TICKS =
                (SEARCH_VOLUME + SAMPLES_PER_TICK - 1) / SAMPLES_PER_TICK;
        private static final int SEARCH_STRIDE = 7919;
        private static final int SEARCH_RESULT_CACHE_TICKS = 60;

        @Nullable private BlockPos target;
        @Nullable private BlockPos approach;
        @Nullable private BlockPos bestCandidate;
        @Nullable private BlockPos bestCandidateApproach;
        @Nullable private BlockPos searchOrigin;
        @Nullable private BlockPos rejectedTarget;
        @Nullable private Path cachedPath;
        private double bestCandidateDistance;
        private int searchTicksRemaining;
        private int searchIndex;
        private int searchCooldown;
        private int rejectedTargetCooldown;

        private TransformUnbreakableBlockGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(level() instanceof ServerLevel serverLevel)
                    || !serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
                resetSearch();
                return false;
            }
            if (searchCooldown > 0) {
                searchCooldown--;
                tickRejectedTargetCooldown();
                return false;
            }
            tickRejectedTargetCooldown();
            if (searchTicksRemaining == 0) {
                beginSearch();
            }
            sampleCandidates();
            searchTicksRemaining--;
            if (searchTicksRemaining > 0
                    && (bestCandidate == null
                            || searchTicksRemaining
                                    > MAX_SEARCH_TICKS - MIN_SEARCH_TICKS)) {
                return false;
            }
            searchCooldown = SEARCH_RESULT_CACHE_TICKS;
            return finishSearch();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && approach != null
                    && isUnbreakableTarget(target)
                    && (isWithinTransformRange() || !getNavigation().isDone());
        }

        @Override
        public void start() {
            if (!isWithinTransformRange()) {
                getNavigation().moveTo(cachedPath, 1.2D);
            }
        }

        @Override
        public void tick() {
            if (target == null) return;
            if (isWithinTransformRange()) {
                level().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
                discard();
            }
        }

        private boolean isWithinTransformRange() {
            return target != null && distanceToSqr(target.getX() + 0.5D,
                    target.getY() + 0.5D, target.getZ() + 0.5D) <= 4.0D;
        }

        private void beginSearch() {
            searchOrigin = blockPosition();
            bestCandidate = null;
            bestCandidateApproach = null;
            bestCandidateDistance = Double.MAX_VALUE;
            searchTicksRemaining = MAX_SEARCH_TICKS;
            searchIndex = getRandom().nextInt(SEARCH_VOLUME);
        }

        private void sampleCandidates() {
            for (int i = 0; i < SAMPLES_PER_TICK; i++) {
                int index = searchIndex;
                searchIndex = (searchIndex + SEARCH_STRIDE) % SEARCH_VOLUME;
                int x = index % SEARCH_WIDTH - HORIZONTAL_SEARCH_RANGE;
                index /= SEARCH_WIDTH;
                int z = index % SEARCH_WIDTH - HORIZONTAL_SEARCH_RANGE;
                int y = index / SEARCH_WIDTH - VERTICAL_SEARCH_RANGE;
                BlockPos pos = searchOrigin.offset(x, y, z);
                if (pos.equals(rejectedTarget)) {
                    continue;
                }
                if (!isUnbreakableTarget(pos)) {
                    continue;
                }
                BlockPos candidateApproach = findCheapApproach(pos);
                if (candidateApproach == null) {
                    continue;
                }
                double distance = pos.distSqr(searchOrigin);
                if (distance < bestCandidateDistance) {
                    bestCandidate = pos.immutable();
                    bestCandidateApproach = candidateApproach;
                    bestCandidateDistance = distance;
                }
            }
        }

        private boolean finishSearch() {
            target = bestCandidate;
            approach = bestCandidateApproach;
            cachedPath = null;
            searchOrigin = null;
            searchTicksRemaining = 0;
            bestCandidate = null;
            bestCandidateApproach = null;
            if (target == null || approach == null) {
                return false;
            }
            if (distanceToSqr(target.getX() + 0.5D,
                    target.getY() + 0.5D, target.getZ() + 0.5D) <= 4.0D) {
                return true;
            }
            cachedPath = getNavigation().createPath(approach, 0);
            if (cachedPath != null && cachedPath.canReach()) {
                rejectedTarget = null;
                rejectedTargetCooldown = 0;
                return true;
            }
            rejectedTarget = target;
            rejectedTargetCooldown = SEARCH_RESULT_CACHE_TICKS * 2;
            target = null;
            approach = null;
            cachedPath = null;
            searchCooldown = 20;
            return false;
        }

        @Nullable
        private BlockPos findCheapApproach(BlockPos block) {
            if (distanceToSqr(block.getX() + 0.5D,
                    block.getY() + 0.5D, block.getZ() + 0.5D) <= 4.0D) {
                return blockPosition();
            }
            BlockPos nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Direction direction : Direction.values()) {
                BlockPos candidate = block.relative(direction);
                if (!level().getBlockState(candidate)
                                .getCollisionShape(level(), candidate).isEmpty()) {
                    continue;
                }
                BlockPos support = candidate.below();
                if (level().getBlockState(support)
                        .getCollisionShape(level(), support).isEmpty()) {
                    continue;
                }
                double distance = candidate.distSqr(blockPosition());
                if (distance < nearestDistance) {
                    nearest = candidate.immutable();
                    nearestDistance = distance;
                }
            }
            return nearest;
        }

        private void resetSearch() {
            target = null;
            approach = null;
            bestCandidate = null;
            bestCandidateApproach = null;
            searchOrigin = null;
            cachedPath = null;
            searchTicksRemaining = 0;
        }

        private void tickRejectedTargetCooldown() {
            if (rejectedTargetCooldown > 0
                    && --rejectedTargetCooldown == 0) {
                rejectedTarget = null;
            }
        }

        private boolean isUnbreakableTarget(BlockPos pos) {
            BlockState state = level().getBlockState(pos);
            return !state.isAir()
                    && !state.is(InvBlocks.NEXUS_CORE)
                    && !state.is(BlockTags.CLIMBABLE)
                    && (state.getDestroySpeed(level(), pos) < 0.0F
                            || BlockMetadata.isIndestructible(state));
        }
    }

    private final class InfectEntityGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
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
            if (candidate instanceof Silverfish
                    || candidate instanceof Endermite
                    || isFlyingTarget(candidate)
                    || candidate.entityTags().contains(INFECTED_TAG)
                    || candidate instanceof Player) {
                return false;
            }
            if (invasionMob) {
                return candidate instanceof Combatant<?>
                        && !(candidate instanceof IMWolfEntity);
            }
            return candidate instanceof IMWolfEntity
                    || candidate instanceof AbstractGolem
                    || candidate instanceof OwnableEntity ownable
                            && ownable.getRootOwner() instanceof Player;
        }

        private boolean isFlyingTarget(LivingEntity candidate) {
            EntityType<?> type = candidate.getType();
            return type == EntityTypes.ALLAY || type == EntityTypes.BAT
                    || type == EntityTypes.BEE || type == EntityTypes.BLAZE
                    || type == EntityTypes.ENDER_DRAGON || type == EntityTypes.GHAST
                    || type == EntityTypes.HAPPY_GHAST || type == EntityTypes.PARROT
                    || type == EntityTypes.PHANTOM || type == EntityTypes.VEX
                    || type == EntityTypes.WITHER
                    || candidate instanceof IMBlazeEntity
                    || candidate instanceof IMGhastEntity
                    || candidate instanceof IMPhantomEntity
                    || candidate instanceof IMWitherEntity;
        }
    }

    private final class AttackNexusGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
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
