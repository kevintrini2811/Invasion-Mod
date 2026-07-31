package com.invasion.entity;

import java.util.Optional;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.CarryBlockingBlockGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.ai.goal.MobMeleeAttackGoal;
import com.invasion.entity.ai.goal.target.CustomRangeActiveTargetGoal;
import com.invasion.entity.ai.goal.target.RetaliateGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.entity.pathfinding.IMMobNavigation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public final class IMEndermanEntity extends IMMobEntity {
    private static final EntityDataAccessor<Optional<BlockState>> CARRIED_BLOCK =
            SynchedEntityData.defineId(IMEndermanEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);

    public IMEndermanEntity(EntityType<? extends IMEndermanEntity> type, Level level) {
        super(type, level);
        flammability = 1;
        getNavigatorNew().setCanDestroyBlocks(true);
        setPathfindingMalus(BlockPathTypes.WATER, -1);
        setPathfindingMalus(BlockPathTypes.WATER_BORDER, -1);
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
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected PathNavigation createNavigation(Level level) {
        return new IMMobNavigation(this, createIMNavigation().getActor()) {
            @Override
            public IMLandPathNodeMaker createNodeMaker() {
                IMLandPathNodeMaker nodeMaker = new IMLandPathNodeMaker() {
                    @Override
                    public boolean canMineBlock(CollisionGetter world, BlockPos pos, BlockState state) {
                        return canDestroyBlocks() && !state.isAir();
                    }
                };
                nodeMaker.setCanPassDoors(true);
                nodeMaker.setCanOpenDoors(true);
                nodeMaker.setCanFloat(true);
                nodeMaker.setCanClimbLadders(true);
                return nodeMaker;
            }
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CARRIED_BLOCK, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CarryBlockingBlockGoal(this));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(3, new GoToNexusGoal(this));
        goalSelector.addGoal(4, new MobMeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(0, new RetaliateGoal(this));
        targetSelector.addGoal(1,
                new CustomRangeActiveTargetGoal<>(this, Player.class, this::getSenseRange, false));
        targetSelector.addGoal(2,
                new CustomRangeActiveTargetGoal<>(this, Player.class, this::getAggroRange, true));
    }

    public Optional<BlockState> getCarriedBlock() {
        return entityData.get(CARRIED_BLOCK);
    }

    public boolean isCarryingBlock() {
        return getCarriedBlock().isPresent();
    }

    public void setCarriedBlock(BlockState state) {
        if (!isCarryingBlock() && !state.isAir()) {
            entityData.set(CARRIED_BLOCK, Optional.of(state));
            getNavigatorNew().setCanDestroyBlocks(false);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        if (target != null && distanceToSqr(target) > 256 && tickCount % 10 == 0) {
            teleportTowards(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            for (int attempt = 0; attempt < 64; attempt++) {
                if (teleportRandomly()) {
                    return true;
                }
            }
            return false;
        }

        boolean damaged = super.hurt(source, amount);
        if (damaged && source.getEntity() == null && random.nextInt(10) != 0) {
            teleportRandomly();
        }
        return damaged;
    }

    private boolean teleportRandomly() {
        return teleportSafely(
                getX() + (random.nextDouble() - 0.5) * 64,
                getY() + random.nextInt(64) - 32,
                getZ() + (random.nextDouble() - 0.5) * 64);
    }

    private boolean teleportTowards(Entity target) {
        Vec3 away = new Vec3(
                getX() - target.getX(),
                getY(0.5) - target.getEyeY(),
                getZ() - target.getZ()).normalize();
        return teleportSafely(
                getX() + (random.nextDouble() - 0.5) * 8 - away.x * 16,
                getY() + random.nextInt(16) - 8 - away.y * 16,
                getZ() + (random.nextDouble() - 0.5) * 8 - away.z * 16);
    }

    private boolean teleportSafely(double x, double y, double z) {
        if (level().isClientSide() || !isAlive()) {
            return false;
        }

        BlockPos.MutableBlockPos ground = new BlockPos.MutableBlockPos(x, y, z);
        while (ground.getY() > level().getMinBuildHeight()
                && !level().getBlockState(ground).blocksMotion()) {
            ground.move(Direction.DOWN);
        }
        BlockState groundState = level().getBlockState(ground);
        if (!groundState.blocksMotion() || groundState.getFluidState().is(FluidTags.WATER)) {
            return false;
        }

        Vec3 oldPosition = position();
        if (!randomTeleport(x, y, z, true)) {
            return false;
        }
        level().gameEvent(GameEvent.TELEPORT, oldPosition, GameEvent.Context.of(this));
        if (!isSilent()) {
            level().playSound(null, xo, yo, zo, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1, 1);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
        }
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean causedByPlayer) {
        super.dropCustomDeathLoot(source, looting, causedByPlayer);
        VanillaLoot.drop(this, EntityType.ENDERMAN, source,
                causedByPlayer ? lastHurtByPlayer : null);
        getCarriedBlock().ifPresent(state -> {
            ItemStack stack = new ItemStack(state.getBlock().asItem());
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        });
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        getCarriedBlock().ifPresent(state ->
                output.putString("carried_block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        String id = input.getString("carried_block");
        if (!id.isEmpty()) {
            net.minecraft.resources.ResourceLocation identifier = net.minecraft.resources.ResourceLocation.tryParse(id);
            if (identifier != null) {
                BuiltInRegistries.BLOCK.getOptional(identifier)
                        .filter(block -> block != Blocks.AIR)
                        .ifPresent(block -> {
                            entityData.set(CARRIED_BLOCK, Optional.of(block.defaultBlockState()));
                            getNavigatorNew().setCanDestroyBlocks(false);
                        });
            }
        }
    }
}
