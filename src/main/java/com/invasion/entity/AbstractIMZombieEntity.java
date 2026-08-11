package com.invasion.entity;

import com.invasion.entity.ai.goal.EntityAIKillWithArrow;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.SkeletonAttackNexusGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.entity.pathfinding.IMMobNavigation;
import com.invasion.nexus.ai.scaffold.ScaffoldView;
import com.invasion.util.math.PosUtils;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
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
import net.minecraft.world.level.pathfinder.BlockPathTypes;
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
    public boolean wantsToPickUp(ItemStack stack) {
        ItemStack heldItem = getItemBySlot(EquipmentSlot.MAINHAND);
        if (isUsableWeapon(stack)) {
            return !isUsableWeapon(heldItem);
        }
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        if (isBrute()
                && slot != EquipmentSlot.HEAD
                && slot != EquipmentSlot.LEGS
                && slot != EquipmentSlot.FEET) {
            return false;
        }
        return slot.isArmor()
                && stack.canEquip(slot, this)
                && canReplaceCurrentItem(stack, getItemBySlot(slot));
    }

    private static boolean isUsableWeapon(ItemStack stack) {
        return EquipmentUtil.isWeapon(stack);
    }

    public final boolean isHoldingRangedWeapon() {
        return EquipmentUtil.isRangedWeapon(
                getItemBySlot(EquipmentSlot.MAINHAND));
    }

    protected final void addWeaponCombatGoals(double meleeSpeed) {
        // While assigned to a nexus, ranged zombies must keep advancing just
        // like melee zombies. A high-priority player-shooting goal otherwise
        // makes them follow remembered players even without line of sight.
        goalSelector.addGoal(6, new PredicatedGoal(
                new EntityAIKillWithArrow<>(
                        this, LivingEntity.class, 65, 16F),
                this::isHoldingRangedWeapon));
        goalSelector.addGoal(2, new PredicatedGoal(
                new SkeletonAttackNexusGoal<>(this),
                this::isHoldingRangedWeapon));
        goalSelector.addGoal(6, new PredicatedGoal(
                new com.invasion.entity.ai.goal.MobMeleeAttackGoal(
                        this, meleeSpeed, false),
                () -> !isHoldingRangedWeapon()));
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel world = (ServerLevel) level();

        if (!ItemSearchScheduler.shouldSearch(this)) {
            return;
        }

        for (ItemEntity item : world.getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(1.25D),
                candidate -> !candidate.hasPickUpDelay()
                        && wantsToPickUp(candidate.getItem()))) {
            pickUpItem(item);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        ItemStack weapon = getMainHandItem();
        if (weapon.is(Items.TRIDENT)) {
            shootTrident(target.getX(), target.getY(0.3333333333333333), target.getZ());
            return;
        }
        ItemStack arrow = getProjectile(weapon);
        if (arrow.isEmpty()) arrow = Items.ARROW.getDefaultInstance();
        AbstractArrow projectile = ProjectileUtil.getMobArrow(
                this, arrow, pullProgress);
        shootArrow(projectile, target.getX(), target.getY(0.3333333333333333),
                target.getZ());
    }

    @Override
    public void performRangedNexusAttack(net.minecraft.world.phys.Vec3 target) {
        if (getMainHandItem().is(Items.TRIDENT)) {
            shootTrident(target.x, target.y, target.z);
            return;
        }
        SkeletonArrowEntity projectile =
                new SkeletonArrowEntity(level(), this, getMainHandItem());
        shootArrow(projectile, target.x, target.y, target.z);
    }

    private void shootTrident(double targetX, double targetY, double targetZ) {
        if (!(level() instanceof ServerLevel world)) return;
        ItemStack tridentStack = getMainHandItem().copy();
        ThrownTrident projectile = new ThrownTrident(world, this, tridentStack);
        double dX = targetX - getX();
        double dY = targetY - projectile.getY();
        double dZ = targetZ - getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        Projectile.spawnProjectileUsingShoot(
                projectile, world, tridentStack,
                dX, dY + horizontalDistance * 0.2F, dZ,
                1.6F, 14 - world.getDifficulty().getId() * 4);
        playSound(SoundEvents.DROWNED_SHOOT, 1.0F,
                1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
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
                1,
                1 / (random.nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new Navigation(this);
    }

    @Override
    public com.invasion.entity.pathfinding.Navigation getNavigatorNew() {
        if (!(navigation instanceof com.invasion.entity.pathfinding.Navigation)) {
            navigation = createNavigation(level());
        }
        return (com.invasion.entity.pathfinding.Navigation) navigation;
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
    public boolean doHurtTarget(Entity entity) {
        return getTier() == 3 && isSprinting() ? chargeAttack(entity) : super.doHurtTarget(entity);
    }

    protected boolean chargeAttack(Entity entity) {
        int knockback = 4;
        entity.hurt(damageSources().mobAttack(this), (float)getAttackStrength() + 3);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        if (entity instanceof LivingEntity l) {
            l.knockback(knockback, Mth.sin(yaw), Mth.cos(yaw));
        }
        setSprinting(false);
        playSound(SoundEvents.GENERIC_BIG_FALL, 1, 1);
        return true;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (getTier() != 3) {
            super.knockback(strength, x, z);
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
                // DynamicPathNodeNavigator does not carry a CollisionGetter in
                // 1.20.1. WalkNodeEvaluator already stores the prepared region.
                world = level;
                if (this.mob instanceof AbstractIMZombieEntity entity
                        && AbstractIMZombieEntity.isTar(entity)
                        && nextNode.type == BlockPathTypes.WATER) {
                    float multiplier = 1 + ScaffoldView.of(world).getMobDensity(nextNode.asBlockPos()) * 3;

                    if (nextNode.y > previousNode.y && canMineBlock(world,
                            nextNode.asBlockPos(), world.getBlockState(nextNode.asBlockPos()))) {
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
