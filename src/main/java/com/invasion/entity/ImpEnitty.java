package com.invasion.entity;

import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.KillEntityGoal;
import com.invasion.entity.ai.goal.NoNexusPathGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.ProvideSupportGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.target.RetaliateGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;

public class ImpEnitty extends IMMobEntity
        implements RangedAttackMob, RangedNexusAttacker {
    private static final int BLOCK_IGNITION_COOLDOWN = 40;
    private int nextBlockIgnitionTick;

    public ImpEnitty(EntityType<ImpEnitty> type, Level world) {
        super(type, world);
        getNavigatorNew().getActor().setCanClimb(true);
        setCanPickUpLoot(
                com.invasion.compat.AsyncCompatibility.canUseVanillaItemPickup());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.STEP_HEIGHT, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PredicatedGoal(
                new KillEntityGoal<>(this, Player.class, 40),
                () -> !isHoldingRangedWeapon()));
        goalSelector.addGoal(1, new PredicatedGoal(
                new EntityAIKillWithArrow<>(this, Player.class, 65, 16F),
                this::isHoldingRangedWeapon));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(2, new PredicatedGoal(
                new SkeletonAttackNexusGoal<>(this),
                this::isHoldingRangedWeapon));
        goalSelector.addGoal(3, new ProvideSupportGoal(this, 4, true));
        goalSelector.addGoal(4, new PredicatedGoal(
                new KillEntityGoal<>(this, Mob.class, 40),
                () -> !isHoldingRangedWeapon()));
        goalSelector.addGoal(4, new PredicatedGoal(
                new EntityAIKillWithArrow<>(this, Mob.class, 65, 16F),
                this::isHoldingRangedWeapon));
        goalSelector.addGoal(5, new GoToNexusGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, IMCreeperEntity.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new RetaliateGoal(this));
        targetSelector.addGoal(1, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(2, new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
        targetSelector.addGoal(5, new HurtByTargetGoal(this));
        targetSelector.addGoal(3, new NoNexusPathGoal(this, new CustomRangeActiveTargetGoal<>(this, PigmanEngineerEntity.class, 3.5F)));
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity entity) {
        if (super.doHurtTarget(serverLevel, entity)) {
            entity.igniteForSeconds(3);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurtServer(
            ServerLevel serverLevel, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA)) {
            return false;
        }
        return super.hurtServer(serverLevel, source, damage);
    }

    @Override
    public boolean wantsToPickUp(ServerLevel world, ItemStack stack) {
        return isUsableWeapon(stack)
                && !isUsableWeapon(getMainHandItem());
    }

    private static boolean isUsableWeapon(ItemStack stack) {
        return EquipmentUtil.isWeapon(stack);
    }

    private boolean isHoldingRangedWeapon() {
        return EquipmentUtil.isRangedWeapon(getMainHandItem());
    }

    @Override
    public void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (isOnFire()
                && !isInLava()
                && world.getBlockStates(getBoundingBox().deflate(0.001D))
                        .noneMatch(state -> state.is(BlockTags.FIRE))) {
            clearFire();
        }
        if (tickCount >= nextBlockIgnitionTick
                && world.getGameRules().get(GameRules.MOB_GRIEFING)
                && tryIgniteNearbyBlock(world)) {
            nextBlockIgnitionTick = tickCount + BLOCK_IGNITION_COOLDOWN;
        }
        if (tickCount % 5 != 0 || isUsableWeapon(getMainHandItem())) {
            return;
        }
        for (ItemEntity item : world.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(1.25D),
                candidate -> !candidate.hasPickUpDelay()
                        && wantsToPickUp(world, candidate.getItem()))) {
            pickUpItem(world, item);
            if (isUsableWeapon(getMainHandItem())) {
                break;
            }
        }
    }

    private boolean tryIgniteNearbyBlock(ServerLevel world) {
        for (BlockPos fuelPos : BlockPos.withinManhattan(
                blockPosition(), 2, 1, 2)) {
            if (!world.getBlockState(fuelPos).ignitedByLava()) {
                continue;
            }
            int firstDirection = getRandom().nextInt(Direction.values().length);
            for (int offset = 0; offset < Direction.values().length; offset++) {
                Direction direction = Direction.values()[
                        (firstDirection + offset) % Direction.values().length];
                BlockPos firePos = fuelPos.relative(direction);
                if (!world.isEmptyBlock(firePos)) {
                    continue;
                }
                var fireState = BaseFireBlock.getState(world, firePos);
                if (!fireState.canSurvive(world, firePos)) {
                    continue;
                }
                world.setBlockAndUpdate(firePos, fireState);
                world.gameEvent(this, GameEvent.BLOCK_PLACE, firePos);
                playSound(
                        SoundEvents.FLINTANDSTEEL_USE,
                        1.0F,
                        0.8F + getRandom().nextFloat() * 0.4F);
                return true;
            }
        }
        return false;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack weapon = getMainHandItem();
        AbstractArrow projectile = ProjectileUtil.getMobArrow(
                this, getProjectile(weapon), pullProgress, weapon);
        shootArrow(projectile, target.getX(), target.getY(0.3333333333333333),
                target.getZ());
    }

    @Override
    public void performRangedNexusAttack(net.minecraft.world.phys.Vec3 target) {
        shootArrow(new SkeletonArrowEntity(level(), this, getMainHandItem()),
                target.x, target.y, target.z);
    }

    private void shootArrow(
            AbstractArrow projectile, double targetX, double targetY,
            double targetZ) {
        double dX = targetX - getX();
        double dY = targetY - projectile.getY();
        double dZ = targetZ - getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        projectile.shoot(
                dX, dY + horizontalDistance * 0.2F, dZ, 1.1F, 12);
        playSound(
                getMainHandItem().is(Items.CROSSBOW)
                        ? SoundEvents.CROSSBOW_SHOOT
                        : SoundEvents.SKELETON_SHOOT,
                1, 1 / (getRandom().nextFloat() * 0.4F + 0.8F));
        projectile.igniteForSeconds(100);
        level().addFreshEntity(projectile);
    }
}
