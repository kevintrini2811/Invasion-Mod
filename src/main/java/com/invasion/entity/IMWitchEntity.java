package com.invasion.entity;

import java.util.EnumSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvMobEffects;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Nexus support caster that prioritizes nearby player-aligned targets. */
public final class IMWitchEntity extends Witch
        implements Combatant<Witch>, EntityConstruct.BuildableMob {
    private static final double THROW_RANGE = 16.0D;
    private static final double FOLLOW_RANGE = 32.0D;
    private static final int THROW_COOLDOWN = 80;
    private static final int MIN_TARGET_SEARCH_INTERVAL = 5;
    private static final int TARGET_SEARCH_INTERVAL_VARIANCE = 6;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMWitchEntity(EntityType<? extends Witch> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // Witch.aiStep() always updates two private goals initialized by the
        // vanilla implementation. Initialize them before replacing the
        // registered behavior with the Nexus support AI.
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WitchSupportGoal());
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
    }

    @Override
    public IHasNexus.Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public Witch asEntity() {
        return this;
    }

    @Override
    public String getLegacyName() {
        return "IMWitch-T1";
    }

    @Override
    public void onSpawned(@Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        resetHealth();
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
        return !hasNexus();
    }

    @Override
    public boolean requiresCustomPersistence() {
        return hasNexus() || super.requiresCustomPersistence();
    }

    static boolean isPlayerAlly(LivingEntity candidate, ServerLevel level) {
        if (candidate instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        if (candidate instanceof AbstractVillager
                || candidate instanceof AbstractGolem
                || candidate instanceof AbstractPiglin
                || candidate instanceof Hoglin
                || candidate instanceof Pig
                || candidate instanceof IMWolfEntity) {
            return true;
        }
        if (candidate instanceof OwnableEntity ownable
                && ownable.getRootOwner() instanceof Player) {
            return true;
        }
        return level.players().stream().anyMatch(candidate::isAlliedTo);
    }

    private final class WitchSupportGoal extends Goal {
        private int cooldown;
        private int targetSearchCooldown;
        @Nullable
        private LivingEntity cachedEnemy;
        @Nullable
        private LivingEntity cachedAlly;

        private WitchSupportGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            ServerLevel level = (ServerLevel) level();
            if (!isValidEnemy(cachedEnemy, level)) {
                cachedEnemy = null;
            }
            if (!isValidAlly(cachedAlly)) {
                cachedAlly = null;
            }
            if (targetSearchCooldown > 0) {
                targetSearchCooldown--;
            } else {
                if (cachedEnemy == null) {
                    cachedEnemy = nearest(level, THROW_RANGE,
                            candidate -> isPlayerAlly(candidate, level));
                }
                if (cachedEnemy == null && cachedAlly == null) {
                    cachedAlly = nearest(level, FOLLOW_RANGE,
                            this::isNexusAlly);
                }
                targetSearchCooldown = MIN_TARGET_SEARCH_INTERVAL
                        + getRandom().nextInt(TARGET_SEARCH_INTERVAL_VARIANCE);
            }
            if (cachedEnemy != null) {
                getNavigation().stop();
                getLookControl().setLookAt(cachedEnemy, 30.0F, 30.0F);
                if (cooldown == 0 && hasLineOfSight(cachedEnemy)) {
                    throwPotion(cachedEnemy, harmfulType());
                }
                return;
            }

            if (cachedAlly == null) {
                return;
            }
            getLookControl().setLookAt(cachedAlly, 20.0F, 20.0F);
            double distance = distanceToSqr(cachedAlly);
            if (distance > 6.0D * 6.0D) {
                getNavigation().moveTo(cachedAlly, 1.0D);
            } else {
                getNavigation().stop();
            }
            if (cooldown == 0 && distance <= THROW_RANGE * THROW_RANGE
                    && hasLineOfSight(cachedAlly)) {
                throwPotion(cachedAlly, supportType(cachedAlly));
            }
        }

        private boolean isValidEnemy(@Nullable LivingEntity candidate,
                ServerLevel level) {
            return candidate != null && candidate.isAlive()
                    && distanceToSqr(candidate) <= THROW_RANGE * THROW_RANGE
                    && isPlayerAlly(candidate, level);
        }

        private boolean isValidAlly(@Nullable LivingEntity candidate) {
            return candidate != null && candidate.isAlive()
                    && distanceToSqr(candidate) <= FOLLOW_RANGE * FOLLOW_RANGE
                    && isNexusAlly(candidate);
        }

        private boolean isNexusAlly(LivingEntity candidate) {
            return candidate instanceof Combatant<?> combatant
                    && !(candidate instanceof IMWolfEntity)
                    && !(candidate instanceof IMWitchEntity)
                    && combatant.getNexus() == getNexus();
        }

        @Nullable
        private LivingEntity nearest(ServerLevel level, double range,
                java.util.function.Predicate<LivingEntity> predicate) {
            LivingEntity nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : level.getEntitiesOfClass(
                    LivingEntity.class, getBoundingBox().inflate(range),
                    entity -> entity.isAlive() && predicate.test(entity))) {
                double distance = distanceToSqr(candidate);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
            return nearest;
        }

        private IMWitchPotionEntity.Type supportType(LivingEntity ally) {
            if (ally.getHealth() <= ally.getMaxHealth() - 4.0F) {
                return IMWitchPotionEntity.Type.HEALING;
            }
            List<IMWitchPotionEntity.Type> missing = new java.util.ArrayList<>();
            if (!ally.hasEffect(InvMobEffects.INVASION_STRENGTH)) {
                missing.add(IMWitchPotionEntity.Type.STRENGTH);
            }
            if (!ally.hasEffect(MobEffects.SPEED)) {
                missing.add(IMWitchPotionEntity.Type.SPEED);
            }
            if (!ally.hasEffect(MobEffects.HASTE)) {
                missing.add(IMWitchPotionEntity.Type.HASTE);
            }
            return missing.isEmpty()
                    ? IMWitchPotionEntity.Type.HEALING
                    : missing.get(getRandom().nextInt(missing.size()));
        }

        private IMWitchPotionEntity.Type harmfulType() {
            IMWitchPotionEntity.Type[] values = {
                    IMWitchPotionEntity.Type.NAUSEA,
                    IMWitchPotionEntity.Type.POISON,
                    IMWitchPotionEntity.Type.WITHER,
                    IMWitchPotionEntity.Type.BLINDNESS,
                    IMWitchPotionEntity.Type.HUNGER
            };
            return values[getRandom().nextInt(values.length)];
        }

        private void throwPotion(LivingEntity target,
                IMWitchPotionEntity.Type type) {
            IMWitchPotionEntity potion = new IMWitchPotionEntity(
                    level(), IMWitchEntity.this, type);
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double dy = target.getY(0.5D) - potion.getY() + horizontal * 0.2D;
            potion.shoot(dx, dy, dz, 0.75F, 8.0F);
            level().addFreshEntity(potion);
            cooldown = THROW_COOLDOWN;
        }
    }
}
