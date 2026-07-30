package com.invasion.entity;

import java.util.EnumSet;
import java.util.UUID;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.mixin.PhantomAccessor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A Nexus-bound phantom which retains vanilla phantom flight, size scaling,
 * swooping attacks, daylight burning and drops.
 */
public final class IMPhantomEntity extends Phantom
        implements Combatant<Phantom>, EntityConstruct.BuildableMob, HasAiGoals {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private HasAiGoals.Goal aiGoal = HasAiGoals.Goal.NONE;
    private HasAiGoals.Goal previousAiGoal = HasAiGoals.Goal.NONE;
    @Nullable
    private UUID temporarilyUnreachableTarget;
    private int unreachableTargetCooldown;

    public IMPhantomEntity(
            EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        resetHealth();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);

        goalSelector.addGoal(0, new SwoopAtTargetGoal());
        goalSelector.addGoal(1, new FlyToNexusGoal());
        goalSelector.addGoal(2, new IdleCircleGoal());

        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                (candidate, world) -> isPlayerAlly(candidate, world)));
    }

    @Override
    public Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Phantom asEntity() {
        return this;
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        resetHealth();
    }

    @Override
    public String getLegacyName() {
        return "IMPhantom-T1";
    }

    @Override
    public HasAiGoals.Goal getAIGoal() {
        return aiGoal;
    }

    @Override
    public HasAiGoals.Goal getPrevAIGoal() {
        return previousAiGoal;
    }

    @Override
    public HasAiGoals.Goal transitionAIGoal(HasAiGoals.Goal newGoal) {
        previousAiGoal = aiGoal;
        aiGoal = newGoal;
        return previousAiGoal;
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
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        net.minecraft.world.entity.EntityTypes.PHANTOM.getDefaultLootTable()
                .ifPresent(lootTable -> dropFromLootTable(
                        level, source, causedByPlayer, lootTable));
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return !hasNexus();
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (!hasNexus()) {
            WorldNexusStorage.of(level).getNexus()
                    .filter(NexusAccess::isActive)
                    .ifPresent(this::setNexus);
        }
        if (unreachableTargetCooldown > 0
                && --unreachableTargetCooldown == 0) {
            temporarilyUnreachableTarget = null;
        }
        super.customServerAiStep(level);
    }

    private boolean isPlayerAlly(
            LivingEntity candidate, ServerLevel world) {
        if (candidate == this
                || temporarilyUnreachableTarget != null
                        && temporarilyUnreachableTarget.equals(
                                candidate.getUUID())) {
            return false;
        }
        if (candidate instanceof Player) {
            return true;
        }
        if (candidate instanceof AbstractVillager
                || candidate instanceof AbstractGolem) {
            return true;
        }
        if (candidate instanceof OwnableEntity ownable
                && ownable.getRootOwner() instanceof Player) {
            return true;
        }
        return world.players().stream()
                .anyMatch(player -> candidate.isAlliedTo(player));
    }

    private final class FlyToNexusGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
        private int attackCooldown;

        private FlyToNexusGoal() {
            setFlags(EnumSet.of(
                    net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return hasNexus() && getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            transitionAIGoal(HasAiGoals.Goal.GOTO_ENTITY);
            attackCooldown = 20;
        }

        @Override
        public void tick() {
            BlockPos nexusPos = getNexus().getOrigin();
            double distanceSquared = distanceToSqr(
                    nexusPos.getX() + 0.5D,
                    nexusPos.getY() + 0.5D,
                    nexusPos.getZ() + 0.5D);
            if (distanceSquared <= 16D) {
                transitionAIGoal(HasAiGoals.Goal.BREAK_NEXUS);
                if (--attackCooldown <= 0) {
                    getNexus().damage(
                            damageSources().mobAttack(IMPhantomEntity.this), 2);
                    attackCooldown = 20;
                }
                return;
            }

            transitionAIGoal(HasAiGoals.Goal.GOTO_ENTITY);
            setFlightTarget(new Vec3(
                    nexusPos.getX() + 0.5D,
                    nexusPos.getY() + 3.5D,
                    nexusPos.getZ() + 0.5D));
        }

        @Override
        public void stop() {
            transitionAIGoal(HasAiGoals.Goal.NONE);
        }
    }

    private final class SwoopAtTargetGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
        private int retreatTicks;
        private int stalledTicks;
        private double closestDistanceSquared;

        private SwoopAtTargetGoal() {
            setFlags(EnumSet.of(
                    net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            retreatTicks = 0;
            stalledTicks = 0;
            closestDistanceSquared = Double.MAX_VALUE;
            transitionAIGoal(HasAiGoals.Goal.SWOOP);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }

            double distanceSquared = distanceToSqr(target);
            if (distanceSquared + 1D < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                stalledTicks = 0;
            } else if (++stalledTicks >= 100) {
                temporarilyUnreachableTarget = target.getUUID();
                unreachableTargetCooldown = 20 * 15;
                setTarget(null);
                return;
            }

            if (retreatTicks > 0) {
                retreatTicks--;
                stalledTicks = 0;
                closestDistanceSquared = Double.MAX_VALUE;
                setFlightTarget(new Vec3(
                        target.getX(),
                        target.getY() + 8D,
                        target.getZ()));
                return;
            }

            setFlightTarget(new Vec3(
                    target.getX(), target.getY(0.5D), target.getZ()));
            if (getBoundingBox().inflate(0.2D)
                    .intersects(target.getBoundingBox())) {
                doHurtTarget((ServerLevel)level(), target);
                level().levelEvent(1039, blockPosition(), 0);
                retreatTicks = 40;
            }
        }

        @Override
        public void stop() {
            transitionAIGoal(HasAiGoals.Goal.NONE);
        }
    }

    private final class IdleCircleGoal
            extends net.minecraft.world.entity.ai.goal.Goal {
        private float angle;

        private IdleCircleGoal() {
            setFlags(EnumSet.of(
                    net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && !hasNexus();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            angle += 0.05F;
            setFlightTarget(new Vec3(
                    getX() + Math.cos(angle) * 8D,
                    getY() + Math.sin(angle * 0.5F) * 2D,
                    getZ() + Math.sin(angle) * 8D));
        }
    }

    private void setFlightTarget(Vec3 target) {
        ((PhantomAccessor)(Object)this).invasion$setMoveTargetPoint(target);
    }
}
