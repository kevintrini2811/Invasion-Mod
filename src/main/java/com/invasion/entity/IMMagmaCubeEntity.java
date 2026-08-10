package com.invasion.entity;

import java.util.EnumSet;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** Nexus-bound magma cube that consumes and destroys loose items. */
public final class IMMagmaCubeEntity extends MagmaCube
        implements Combatant<MagmaCube>, EntityConstruct.BuildableMob {
    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);
    private boolean suppressNexusDeathSplit;

    public IMMagmaCubeEntity(
            EntityType<? extends MagmaCube> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public MagmaCube asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMMagmaCube-T1";
    }

    @Override
    public boolean causeFallDamage(
            double fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        setSize(1 << getRandom().nextInt(3), true);
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        goalSelector.addGoal(3, new AttackNexusGoal());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (!ItemSearchScheduler.shouldSearch(this)) {
            return;
        }

        double pickupRadius = Math.max(1.25D, getSize() * 0.65D);
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(pickupRadius),
                candidate -> !candidate.hasPickUpDelay()
                        && !candidate.getItem().isEmpty())) {
            take(item, item.getItem().getCount());
            item.discard();
        }
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        net.minecraft.world.entity.EntityTypes.MAGMA_CUBE.getDefaultLootTable()
                .ifPresent(lootTable -> dropFromLootTable(
                        level, source, causedByPlayer, lootTable));
    }

    @Override
    protected void setUpSplitCube(
            AbstractCubeMob cubeMob, int halfSize, float xd, float zd) {
        super.setUpSplitCube(cubeMob, halfSize, xd, zd);
        if (cubeMob instanceof IMMagmaCubeEntity magmaCube) {
            magmaCube.setNexus(getNexus());
        }
    }

    public void suppressSplitOnNexusDeath() {
        suppressNexusDeathSplit = true;
    }

    @Override
    protected int getSplitCount() {
        return suppressNexusDeathSplit ? 0 : super.getSplitCount();
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
        return hasNexus() && !countsTowardMobCapOnSuperflat()
                || super.requiresCustomPersistence();
    }

    @Override
    public boolean isPersistenceRequired() {
        return !countsTowardMobCapOnSuperflat()
                && super.isPersistenceRequired();
    }

    private boolean countsTowardMobCapOnSuperflat() {
        return hasNexus()
                && level() instanceof ServerLevel serverLevel
                && serverLevel.getChunkSource().getGenerator()
                        instanceof FlatLevelSource;
    }

    private final class AttackNexusGoal extends Goal {
        private int attackCooldown;

        private AttackNexusGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return hasNexus() && getNexus().isActive()
                    && (getTarget() == null || !getTarget().isAlive());
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
            attackCooldown = 0;
        }

        @Override
        public void tick() {
            NexusAccess targetNexus = getNexus();
            if (targetNexus == null) {
                return;
            }
            BlockPos pos = targetNexus.getOrigin();
            double targetX = pos.getX() + 0.5D;
            double targetY = pos.getY() + 0.5D;
            double targetZ = pos.getZ() + 0.5D;
            double dx = targetX - getX();
            double dz = targetZ - getZ();
            float direction = (float)(Mth.atan2(dz, dx)
                    * (180.0D / Math.PI)) - 90.0F;

            getLookControl().setLookAt(targetX, targetY, targetZ);
            if (getMoveControl() instanceof CubeMobMoveControl<?> control) {
                control.setDirection(direction, true);
                control.setWantedMovement(1.0D);
            }

            double attackRange = Math.max(2.0D,
                    getBbWidth() * 0.5D + 1.0D);
            if (distanceToSqr(targetX, targetY, targetZ)
                    <= attackRange * attackRange
                    && --attackCooldown <= 0) {
                targetNexus.damage(damageSources().mobAttack(
                        IMMagmaCubeEntity.this), Math.max(1, getSize()));
                attackCooldown = 20;
            }
        }
    }
}
