package com.invasion.entity;

import org.jetbrains.annotations.Nullable;

import com.invasion.Notifiable;
import com.invasion.block.InvBlocks;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.IMCreeperIgniteGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.ProvideSupportGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.entity.pathfinding.IMMobNavigation;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
    private static final EntityDataAccessor<Integer> FUSE_SPEED = SynchedEntityData.defineId(IMCreeperEntity.class, EntityDataSerializers.INT);

    private int currentFuseTime;
    private int lastFuseTime;
    private int fuseTime = 30;

    private boolean explosionDeath;
    private boolean commitToExplode;

    private Direction explodeDirection = Direction.UP;

    public IMCreeperEntity(EntityType<IMCreeperEntity> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.21);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE_SPEED, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new IMCreeperIgniteGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 0.25D, 0.300000011920929D));
        goalSelector.addGoal(4, new AttackNexusGoal<>(this));
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
            Vec3 delta = path.getEntityPosAtNode(this, path.getNextNodeIndex()).subtract(position());
            float facing = (float) (Math.atan2(delta.x(), delta.z()) * Mth.RAD_TO_DEG) - 90;
            explodeDirection = Direction.fromYRot(facing);
            commitToExplode = true;
            setFuseSpeed(1);
        }
        return false;
    }

    @Override
    public void tick() {
        if (explosionDeath) {
            explode();
        } else if (isAlive()) {
            this.lastFuseTime = currentFuseTime;
            int speed = getFuseSpeed();

            if (speed > 0) {
                if (commitToExplode) {
                    getMoveControl().setWantedPosition(getX() + explodeDirection.getUnitVec3i().getX(), getY(), getZ() + explodeDirection.getUnitVec3i().getZ(), 0.1);
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
    protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(world, source, causedByPlayer);
        Entity entity = source.getEntity();
        if (entity != this && entity instanceof Creeper) {
            spawnAtLocation(world, Items.CREEPER_HEAD);
        }
    }

    public boolean isPowered() {
        return getTier() > 1;
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
        return true;
    }

    public float getClientFuseTime(float tickDelta) {
        return Mth.lerp(tickDelta, (float)lastFuseTime, (float)currentFuseTime) / (fuseTime - 2);
    }

    protected void explode() {
        if (!level().isClientSide()) {
            // IN - Added explosion power based on tier
            float explosionPower = 2.1F * Math.max(getTier(), 1);
            level().explode(this, getX(), getY(), getZ(), explosionPower, false, ExplosionInteraction.MOB);
            discard();
        }
    }

    public int getFuseSpeed() {
        return entityData.get(FUSE_SPEED);
    }

    public void setFuseSpeed(int speed) {
        entityData.set(FUSE_SPEED, commitToExplode ? 1 : speed);
    }


    @Override
    public void addAdditionalSaveData(ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putShort("Fuse", (short)fuseTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        fuseTime = nbt.getShortOr("Fuse", (short) fuseTime);
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
                world = currentContext.level();

                BlockState state = world.getBlockState(nextNode.asBlockPos());
                if (!state.isAir() && !state.isPathfindable(PathComputationType.LAND) && !state.is(InvBlocks.NEXUS_CORE)) {
                    // TODO: I'm not sure about this...
                    return 12;
                }
                return super.getDistancePenalty(previousNode, nextNode, world);
            }
        }
    }
}
