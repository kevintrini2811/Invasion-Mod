package com.invasion.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.mixin.SlimeMoveControlAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;
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
            float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        setSize(1 << getRandom().nextInt(3), true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new AttackNexusGoal());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel)level();
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

    public void suppressSplitOnNexusDeath() {
        suppressNexusDeathSplit = true;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        int size = getSize();
        if (!level().isClientSide && size > 1 && isDeadOrDying()
                && !suppressNexusDeathSplit) {
            Component name = getCustomName();
            boolean noAi = isNoAi();
            float offset = getDimensions(getPose()).width() / 2.0F;
            int halfSize = size / 2;
            int count = 2 + random.nextInt(3);
            List<Mob> children = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                float xOffset = (index % 2 - 0.5F) * offset;
                float zOffset = (index / 2 - 0.5F) * offset;
                IMMagmaCubeEntity child = InvEntities.MAGMA_CUBE.create(level());
                if (child != null) {
                    child.setCustomName(name);
                    child.setNoAi(noAi);
                    child.setInvulnerable(isInvulnerable());
                    child.setSize(halfSize, true);
                    child.moveTo(getX() + xOffset, getY() + 0.5D,
                            getZ() + zOffset, random.nextFloat() * 360.0F, 0.0F);
                    child.setNexus(getNexus());
                    children.add(child);
                }
            }
            if (!net.neoforged.neoforge.event.EventHooks
                    .onMobSplit(this, children).isCanceled()) {
                children.forEach(level()::addFreshEntity);
            }
        }
        if (size > 1 && isDeadOrDying()) {
            setSize(1, false);
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        nexus.writeNbt(output);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
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
            SlimeMoveControlAccessor control =
                    (SlimeMoveControlAccessor)getMoveControl();
            control.invasion$setDirection(direction, true);
            control.invasion$setWantedMovement(1.0D);

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
