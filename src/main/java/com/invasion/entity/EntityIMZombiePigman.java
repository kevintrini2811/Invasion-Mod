package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.ChargeMobGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MineBlockGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.NoNexusPathGoal;
import com.invasion.entity.ai.goal.ProvideSupportGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.target.RetaliateGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

public class EntityIMZombiePigman extends AbstractIMZombieEntity {
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(EntityIMZombiePigman.class, EntityDataSerializers.BOOLEAN);
    private final LavaSwimmingBehavior<EntityIMZombiePigman> lavaSwimming =
            new LavaSwimmingBehavior<>(this);

    public EntityIMZombiePigman(EntityType<EntityIMZombiePigman> type, Level world) {
        super(type, world, 0.75F);
        moveControl = lavaSwimming.createMoveControl();
        goalSelector.removeAllGoals(goal -> goal instanceof FloatGoal);
        goalSelector.addGoal(4, lavaSwimming.createDiveGoal());
        setFireImmune(true);
        getNavigatorNew().setCanDestroyBlocks(true);
    }

    public static AttributeSupplier.Builder createT1Attributes() {
        return Zombie.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25F)
                .add(Attributes.ATTACK_DAMAGE, 8);
    }

    public static AttributeSupplier.Builder createT2Attributes() {
        return Zombie.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35F)
                .add(Attributes.ATTACK_DAMAGE, 12);
    }

    public static AttributeSupplier.Builder createT3Attributes() {
        return Zombie.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.2F)
                .add(Attributes.ATTACK_DAMAGE, 18);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CHARGING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new MineBlockGoal(this));
        goalSelector.addGoal(1, new PredicatedGoal(
                new ChargeMobGoal<>(this, Player.class, 0.75F),
                () -> getTier() == 3 && !isHoldingRangedWeapon()));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(4, new ProvideSupportGoal(this, 4, true));
        goalSelector.addGoal(6, new GoToNexusGoal(this));
        addWeaponCombatGoals(1.4F);
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new RetaliateGoal(this));
        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, false), () -> getTier() != 3));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(3, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, PigmanEngineerEntity.class, 3.5F), () -> getTier() != 3 && NoNexusPathGoal.isLostPathToNexus(this)));
        targetSelector.addGoal(4, new CustomRangeActiveTargetGoal<>(this, IronGolem.class, this::getAggroRange, true));
        targetSelector.addGoal(5, new HurtByTargetGoal(this));
    }

    @Override
    public void travel(Vec3 input) {
        if (isInLava() && lavaSwimming.wantsToSwim()) {
            moveRelative(0.01F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9D));
        } else {
            super.travel(input);
        }
    }

    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    public void setCharging(boolean charging) {
        entityData.set(CHARGING, charging);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (isCharging()) {
            boolean mobgriefing = level() instanceof ServerLevel serverLevel
                    && serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            boolean sound = false;

            BlockPos center = BlockPos.containing(getEyePosition(1).add(getViewVector(1).normalize()));

            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                if (IMLandPathNodeMaker.canMineBlock(this, pos)) {
                    sound = true;
                    if (mobgriefing) {
                        level().destroyBlock(pos, InvasionMod.getConfig().destructedBlocksDrop);
                    }

                    for (int i = 0; i < 10; i++) {
                        double x = getRandom().triangle(pos.getX() + 0.5, 0.5);
                        double y = getRandom().triangle(pos.getY() + 0.5, 0.5);
                        double z = getRandom().triangle(pos.getZ() + 0.5, 0.5);
                        if (level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CLOUD,
                                    x, y, z, 1,
                                    pos.getX() + 0.5 - x,
                                    pos.getY() + 0.5 - y,
                                    pos.getZ() + 0.5 - z, 0.0D);
                        }
                    }
                }
            }
            if (sound) {
                playSound(SoundEvents.GENERIC_EXPLODE, 0.2F, 0.5F);
            }
        }
    }

    @Override
    public void updateAnimation(boolean override) {

    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        if (getTier() == 3) {
            return random.nextInt(3) == 0 ? InvSounds.ENTITY_BIG_ZOMBIE_AMBIENT : null;
        }

        return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
    }

    @Override
    public int getTextureId() {
        return Mth.clamp(getTier() - 1, 0, 2);
    }

    @Override
    protected void initTieredAttributes() {
        if (getTier() == 1) {
            setBaseMovementSpeed(0.25F);
            setAttackStrength(8);
            setItemSlot(EquipmentSlot.MAINHAND, Items.GOLDEN_SWORD.getDefaultInstance());
            setDropChance(EquipmentSlot.MAINHAND, 0.2F);
        } else if (getTier() == 2) {
            setBaseMovementSpeed(0.35F);
            setAttackStrength(12);

            if (random.nextInt(5) == 1) {
                setItemSlot(EquipmentSlot.HEAD, Items.GOLDEN_HELMET.getDefaultInstance());
            }

            if (random.nextInt(5) == 1) {
                setItemSlot(EquipmentSlot.CHEST, Items.GOLDEN_CHESTPLATE.getDefaultInstance());
            }

            if (random.nextInt(5) == 1) {
                setItemSlot(EquipmentSlot.LEGS, Items.GOLDEN_LEGGINGS.getDefaultInstance());
            }

            if (random.nextInt(5) == 1) {
                setItemSlot(EquipmentSlot.FEET, Items.GOLDEN_BOOTS.getDefaultInstance());
            }
        } else if (isBrute()) {
            setBaseMovementSpeed(0.20F);
            setAttackStrength(18);
        }
    }
}
