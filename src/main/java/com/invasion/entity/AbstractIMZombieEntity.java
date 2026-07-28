package com.invasion.entity;

import com.invasion.item.InvItems;
import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.entity.pathfinding.IMMobNavigation;
import com.invasion.nexus.ai.scaffold.ScaffoldView;
import com.invasion.util.math.PosUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;

public abstract class AbstractIMZombieEntity extends TieredIMMobEntity
        implements Miner, RangedAttackMob, RangedNexusAttacker {

    private boolean fireImmune;

    public static boolean isTar(AbstractIMZombieEntity entity) {
        return entity.getTier() == 2 && entity.getFlavour() == 2;
    }

    protected AbstractIMZombieEntity(EntityType<? extends AbstractIMZombieEntity> type, Level world, float diggingSpeed) {
        super(type, world);
        setCanPickUpLoot(true);
    }

    @Override
    public boolean wantsToPickUp(ServerLevel world, ItemStack stack) {
        ItemStack heldItem = getItemBySlot(EquipmentSlot.MAINHAND);
        return isUsableWeapon(stack)
                && !isUsableWeapon(heldItem);
    }

    private static boolean isUsableWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE)
                || stack.is(InvItems.INFUSED_SWORD)
                || stack.is(Items.BOW);
    }

    public final boolean isHoldingBow() {
        return getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW);
    }

    protected final void addWeaponCombatGoals(double meleeSpeed) {
        goalSelector.addGoal(1, new PredicatedGoal(
                new EntityAIKillWithArrow<>(this, Player.class, 65, 16F),
                this::isHoldingBow));
        goalSelector.addGoal(2, new PredicatedGoal(
                new SkeletonAttackNexusGoal<>(this),
                this::isHoldingBow));
        goalSelector.addGoal(6, new PredicatedGoal(
                new com.invasion.entity.ai.goal.MobMeleeAttackGoal(
                        this, meleeSpeed, false),
                () -> !isHoldingBow()));
    }

    @Override
    public void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);

        if (tickCount % 5 != 0
                || isUsableWeapon(getItemBySlot(EquipmentSlot.MAINHAND))) {
            return;
        }

        for (ItemEntity item : world.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(1.25D),
                candidate -> !candidate.hasPickUpDelay()
                        && wantsToPickUp(world, candidate.getItem()))) {
            pickUpItem(world, item);
            if (isUsableWeapon(getItemBySlot(EquipmentSlot.MAINHAND))) {
                break;
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack bow = getMainHandItem();
        ItemStack arrow = getProjectile(bow);
        AbstractArrow projectile = ProjectileUtil.getMobArrow(
                this, arrow, pullProgress, bow);
        shootArrow(projectile, target.getX(), target.getY(0.3333333333333333),
                target.getZ());
    }

    @Override
    public void performRangedNexusAttack(net.minecraft.world.phys.Vec3 target) {
        SkeletonArrowEntity projectile =
                new SkeletonArrowEntity(level(), this, getMainHandItem());
        shootArrow(projectile, target.x, target.y, target.z);
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
                SoundEvents.SKELETON_SHOOT, 1,
                1 / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new Navigation(this);
    }

    @Override
    public boolean fireImmune() {
        return fireImmune || super.fireImmune();
    }

    protected void setFireImmune(boolean fireImmune) {
        this.fireImmune = fireImmune;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateAnimation(false);
        updateSound();
    }

    protected void updateSound() {

    }

    public abstract void updateAnimation(boolean override);

    public abstract int getTextureId();

    protected int getSwingSpeed() {
        return 10;
    }

    @Override
    public boolean isPushable() {
        return super.getTier() != 3;
    }

    @Override
    protected int decreaseAirSupply(int air) {
        if (getTier() == 2 && getFlavour() == 2) {
            return increaseAirSupply(air);
        }
        return super.decreaseAirSupply(air);
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity entity) {
        return getTier() == 3 && isSprinting() ? chargeAttack(entity) : super.doHurtTarget(serverLevel, entity);
    }

    protected boolean chargeAttack(Entity entity) {
        int knockback = 4;
        entity.hurt(damageSources().mobAttack(this), (float)getAttackStrength() + 3);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        if (entity instanceof LivingEntity l) {
            l.knockback(knockback, Mth.sin(yaw), Mth.cos(yaw), damageSources().mobAttack(this), (float)getAttackStrength());
        }
        setSprinting(false);
        playSound(SoundEvents.GENERIC_BIG_FALL, 1, 1);
        return true;
    }

    @Override
    public void knockback(double strength, double x, double z, DamageSource source, float damage, boolean force) {
        if (getTier() != 3) {
            super.knockback(strength, x, z, source, damage, force);
        }
    }

    @Override
    public void onFollowingEntity(Entity entity) {
        boolean builderHandlesTerrain = entity instanceof PigmanEngineerEntity
                || entity instanceof IMCreeperEntity;
        getNavigatorNew().setCanDestroyBlocks(!builderHandlesTerrain && canDestroyBlocksByDefault());
    }

    protected boolean canDestroyBlocksByDefault() {
        return true;
    }

    public boolean isBrute() {
        return getTier() == 3;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (isBrute() ? 0.75F : 1);
    }

    public float scaleAmount() {
        if (getTier() == 2)
            return 1.12F;
        if (getTier() == 3) {
            return 1.21F;
        }
        return 1.0F;
    }

    @Override
    protected Component getTypeName() {
        if (isBrute()) {
            return Component.translatable(getType().getDescriptionId() + ".brute");
        }
        return super.getTypeName();
    }

    protected static class Navigation extends IMMobNavigation {
        public Navigation(Mob entity) {
            super(entity);
        }

        @Override
        public NodeEvaluator createNodeMaker() {
            var nodeMaker = new NodeMaker();
            nodeMaker.setCanPassDoors(true);
            nodeMaker.setCanOpenDoors(true);
            nodeMaker.setCanFloat(true);
            nodeMaker.setCanClimbLadders(true);
            return nodeMaker;
        }

        class NodeMaker extends IMLandPathNodeMaker {
            @Override
            public float getDistancePenalty(Node previousNode, Node nextNode, CollisionGetter world) {
                world = currentContext.level();

                if (this.mob instanceof AbstractIMZombieEntity entity
                        && AbstractIMZombieEntity.isTar(entity)
                        && nextNode.type == PathType.WATER) {
                    float multiplier = 1 + ScaffoldView.of(world).getMobDensity(nextNode.asBlockPos()) * 3;

                    if (nextNode.y > previousNode.y && canMineBlock(world, nextNode.asBlockPos(), currentContext.getBlockState(nextNode.asBlockPos()))) {
                        multiplier += 2;
                    }

                    return 1.2F * multiplier;
                }
                return super.getDistancePenalty(previousNode, nextNode, world);
            }

            @Override
            public boolean canMineBlock(CollisionGetter world, BlockPos pos, BlockState state) {
                return super.canMineBlock(world, pos, state)
                        && getTargetPos() != null
                        && PosUtils.getInclination(getTargetPos(), pos) <= 2.144D;
            }
        }
    }
}
