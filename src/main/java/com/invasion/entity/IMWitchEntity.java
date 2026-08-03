package com.invasion.entity;

import java.util.Comparator;
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

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public IMWitchEntity(EntityType<? extends Witch> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
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
        if (candidate instanceof Player) {
            return true;
        }
        if (candidate instanceof AbstractVillager
                || candidate instanceof AbstractGolem
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
            LivingEntity enemy = nearest(level, THROW_RANGE,
                    candidate -> isPlayerAlly(candidate, level));
            if (enemy != null) {
                getNavigation().stop();
                getLookControl().setLookAt(enemy, 30.0F, 30.0F);
                if (cooldown == 0 && hasLineOfSight(enemy)) {
                    throwPotion(enemy, harmfulType());
                }
                return;
            }

            LivingEntity ally = nearest(level, FOLLOW_RANGE,
                    candidate -> candidate instanceof Combatant<?> combatant
                            && !(candidate instanceof IMWolfEntity)
                            && candidate != IMWitchEntity.this
                            && combatant.getNexus() == getNexus());
            if (ally == null) {
                return;
            }
            getLookControl().setLookAt(ally, 20.0F, 20.0F);
            double distance = distanceToSqr(ally);
            if (distance > 6.0D * 6.0D) {
                getNavigation().moveTo(ally, 1.0D);
            } else {
                getNavigation().stop();
            }
            if (cooldown == 0 && distance <= THROW_RANGE * THROW_RANGE
                    && hasLineOfSight(ally)) {
                throwPotion(ally, supportType(ally));
            }
        }

        @Nullable
        private LivingEntity nearest(ServerLevel level, double range,
                java.util.function.Predicate<LivingEntity> predicate) {
            return level.getEntitiesOfClass(LivingEntity.class,
                            getBoundingBox().inflate(range),
                            candidate -> candidate.isAlive() && predicate.test(candidate))
                    .stream().min(Comparator.comparingDouble(
                            IMWitchEntity.this::distanceToSqr)).orElse(null);
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
