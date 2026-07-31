package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.Notifiable;
import com.invasion.block.InvBlocks;
import com.invasion.entity.ai.goal.IMCreeperIgniteGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.ProvideSupportGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.entity.pathfinding.IMMobNavigation;
import com.invasion.entity.pathfinding.path.PathAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class IMCreeperEntity extends TieredIMMobEntity implements Leader {
    private static final Item[] CLASSIC_MUSIC_DISCS = {
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS,
            Items.MUSIC_DISC_CHIRP, Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL,
            Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL, Items.MUSIC_DISC_STRAD,
            Items.MUSIC_DISC_WARD
    };

    private static final int MAX_STATIONARY_TICKS = 20 * 10;
    private static final double STATIONARY_TOLERANCE_SQR = 0.2 * 0.2;
    private static final double NEXUS_IGNITION_RANGE = 4.0D;
    private static final int NEXUS_EXPLOSION_DAMAGE_PER_TIER = 5;

    private static final EntityDataAccessor<Integer> FUSE_SPEED = SynchedEntityData.defineId(IMCreeperEntity.class, EntityDataSerializers.INT);

    private int currentFuseTime;
    private int lastFuseTime;
    private int fuseTime = 30;

    private boolean explosionDeath;
    private boolean commitToExplode;
    private boolean manuallyIgnited;

    private Direction explodeDirection = Direction.UP;
    @Nullable
    private Vec3 stationaryAnchor;
    private int stationaryTicks;

    public IMCreeperEntity(EntityType<IMCreeperEntity> type, Level world) {
        super(type, world);
        setCanPickUpLoot(true);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return slot == EquipmentSlot.HEAD
                && stack.canEquip(slot, this)
                && canReplaceCurrentItem(stack, getItemBySlot(slot));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.21);
    }

    public static boolean rollChargedVariant(
            RandomSource random, int chancePercent) {
        return random.nextInt(100) < Mth.clamp(chancePercent, 0, 100);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FUSE_SPEED, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new IMCreeperIgniteGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 0.25D, 0.300000011920929D));
        goalSelector.addGoal(3, new MobMeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new ProvideSupportGoal(this, 4.0F, true));
        goalSelector.addGoal(7, new GoToNexusGoal(this));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 4.8F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, 20.0F, true), this::hasNexus));
        targetSelector.addGoal(1, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false), () -> !hasNexus()));
        targetSelector.addGoal(2, new PredicatedGoal(new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true), () -> !hasNexus()));
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        return new Navigation(this);
    }

    @Override
    public boolean onPathBlocked(Path path, Notifiable notifee) {
        if (!path.isDone()) {
            commitToExplosion(path.getNextNodePos());
        }
        return commitToExplode;
    }

    @Override
    public boolean handlePathAction(BlockPos pos, PathAction action, Notifiable asker) {
        if (action.getType() != PathAction.Type.DIG || getTarget() != null) {
            return false;
        }
        commitToExplosion(pos);
        return true;
    }

    private void commitToExplosion(BlockPos obstacle) {
        Vec3 delta = com.invasion.util.math.PosUtils.center(obstacle).subtract(position());
        float facing = (float)(Math.atan2(delta.x(), delta.z()) * Mth.RAD_TO_DEG) - 90;
        explodeDirection = Direction.fromYRot(facing);
        commitToExplode = true;
        setFuseSpeed(1);
    }

    @Override
    public void tick() {
        if (explosionDeath) {
            explode();
        } else if (isAlive()) {
            tickNexusFuse();
            tickStationaryFuse();
            if (manuallyIgnited) {
                setFuseSpeed(1);
            }
            this.lastFuseTime = currentFuseTime;
            int speed = getFuseSpeed();

            if (speed > 0) {
                if (commitToExplode) {
                    getMoveControl().setWantedPosition(getX() + explodeDirection.getStepX(), getY(), getZ() + explodeDirection.getStepZ(), 0.1);
                }
                if (currentFuseTime == 0) {
                    playSound(SoundEvents.CREEPER_PRIMED, 1, 0.5F);
                }
            }
            currentFuseTime += speed;
            if (currentFuseTime < 0) {
                currentFuseTime = 0;
            }
            if (currentFuseTime >= fuseTime) {
                currentFuseTime = fuseTime;
                explosionDeath = true;
                // IM: Explosion moved to next tick so other mobs are allowed to tick their martyr reactions
            }
        }

        super.tick();
    }

    private void tickNexusFuse() {
        if (level().isClientSide()
                || commitToExplode
                || !hasNexus()
                || !hasGoal(HasAiGoals.Goal.BREAK_NEXUS)) {
            return;
        }
        if (findDistanceToNexus() <= NEXUS_IGNITION_RANGE) {
            getNavigation().stop();
            setTarget(null);
            commitToExplosion(getNexus().getOrigin());
        }
    }

    private void tickStationaryFuse() {
        if (level().isClientSide() || commitToExplode) {
            return;
        }
        if (stationaryAnchor == null) {
            stationaryAnchor = position();
            return;
        }
        if (position().distanceToSqr(stationaryAnchor) > STATIONARY_TOLERANCE_SQR) {
            stationaryAnchor = position();
            stationaryTicks = 0;
            return;
        }
        if (++stationaryTicks >= MAX_STATIONARY_TICKS) {
            BlockPos explosionTarget = hasNexus() ? getNexus().getOrigin() : blockPosition();
            commitToExplosion(explosionTarget);
        }
    }

    @Override
    public boolean isMartyr() {
        return explosionDeath;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (!(target instanceof Goat)) {
            super.setTarget(target);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean causedByPlayer) {
        spawnAtLocation(Items.GUNPOWDER);
        Entity entity = source.getEntity();
        if (entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton
                || entity instanceof IMSkeletonEntity) {
            spawnAtLocation(CLASSIC_MUSIC_DISCS[random.nextInt(CLASSIC_MUSIC_DISCS.length)]);
        }
    }

    public boolean isPowered() {
        return getTier() > 1;
    }

    @Override
    protected InteractionResult mobInteract(
            Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ItemTags.CREEPER_IGNITERS)) {
            return super.mobInteract(player, hand);
        }

        SoundEvent sound = stack.is(Items.FIRE_CHARGE)
                ? SoundEvents.FIRECHARGE_USE
                : SoundEvents.FLINTANDSTEEL_USE;
        level().playSound(
                player, getX(), getY(), getZ(), sound, getSoundSource(),
                1.0F, random.nextFloat() * 0.4F + 0.8F);
        if (!level().isClientSide()) {
            manuallyIgnited = true;
            setFuseSpeed(1);
            if (stack.isDamageableItem()) {
                stack.hurtAndBreak(1, player,
                        p -> p.broadcastBreakEvent(hand));
            } else {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName() {
        return isPowered()
                ? Component.translatable("entity.invmod.charged_creeper")
                : super.getName();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return true;
    }

    public float getClientFuseTime(float tickDelta) {
        return Mth.lerp(tickDelta, (float)lastFuseTime, (float)currentFuseTime) / (fuseTime - 2);
    }

    protected void explode() {
        if (!level().isClientSide()) {
            // IN - Added explosion power based on tier
            float explosionPower = 2.1F * Math.max(getTier(), 1);
            if (hasNexus()
                    && findDistanceToNexus() <= explosionPower * 2.0F) {
                getNexus().damage(damageSources().explosion(this, this),
                        NEXUS_EXPLOSION_DAMAGE_PER_TIER * Math.max(getTier(), 1));
            }
            level().explode(this, getX(), getY(), getZ(), explosionPower, false, ExplosionInteraction.MOB);
            discard();
        }
    }

    public int getFuseSpeed() {
        return entityData.get(FUSE_SPEED);
    }

    public void setFuseSpeed(int speed) {
        entityData.set(
                FUSE_SPEED,
                commitToExplode || manuallyIgnited ? 1 : speed);
    }


    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putShort("Fuse", (short)fuseTime);
        nbt.putInt("stationaryTicks", stationaryTicks);
        nbt.putBoolean("ignited", manuallyIgnited);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        fuseTime = nbt.contains("Fuse") ? nbt.getShort("Fuse") : fuseTime;
        stationaryTicks = nbt.getInt("stationaryTicks");
        manuallyIgnited = nbt.getBoolean("ignited");
    }

    @Override
    protected void initTieredAttributes() {

    }

    protected static class Navigation extends IMMobNavigation {
        public Navigation(Mob entity) {
            super(entity);
        }

        @Override
        public NodeEvaluator createNodeMaker() {
            var nodeMaker = new NodeMaker();
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
                BlockState state = world.getBlockState(nextNode.asBlockPos());
                if (!state.isAir() && !state.isPathfindable(world,
                        nextNode.asBlockPos(), PathComputationType.LAND)
                        && !state.is(InvBlocks.NEXUS_CORE)) {
                    // TODO: I'm not sure about this...
                    return 12;
                }
                return super.getDistancePenalty(previousNode, nextNode, world);
            }
        }
    }
}
