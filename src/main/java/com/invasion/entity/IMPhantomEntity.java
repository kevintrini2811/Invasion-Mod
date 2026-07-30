package com.invasion.entity;

import java.util.EnumSet;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Nexus-bound phantom which retains vanilla phantom flight, size scaling,
 * swooping attacks, daylight burning and drops.
 */
public final class IMPhantomEntity extends Phantom
        implements Combatant<Phantom>, EntityConstruct.BuildableMob, HasAiGoals {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private HasAiGoals.Goal aiGoal = HasAiGoals.Goal.NONE;
    private HasAiGoals.Goal previousAiGoal = HasAiGoals.Goal.NONE;

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
        goalSelector.addGoal(1, new FlyToNexusGoal());
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
            getMoveControl().setWantedPosition(
                    nexusPos.getX() + 0.5D,
                    nexusPos.getY() + 3.5D,
                    nexusPos.getZ() + 0.5D,
                    1.0D);
        }

        @Override
        public void stop() {
            transitionAIGoal(HasAiGoals.Goal.NONE);
        }
    }
}
