package com.invasion.entity;

import java.util.Optional;
import com.invasion.entity.ai.goal.AttackNexusGoal;
import com.invasion.entity.ai.goal.CarryBlockingBlockGoal;
import com.invasion.entity.ai.goal.GoToNexusGoal;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import com.invasion.entity.pathfinding.IMMobNavigation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class IMEndermanEntity extends IMMobEntity {
    private static final EntityDataAccessor<Optional<BlockState>> CARRIED_BLOCK =
            SynchedEntityData.defineId(IMEndermanEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);

    public IMEndermanEntity(EntityType<? extends IMEndermanEntity> type, Level level) {
        super(type, level);
        flammability = 1;
        getNavigatorNew().setCanDestroyBlocks(true);
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARRIED_BLOCK, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CarryBlockingBlockGoal(this));
        goalSelector.addGoal(2, new AttackNexusGoal<>(this));
        goalSelector.addGoal(3, new GoToNexusGoal(this));
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
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        getCarriedBlock().ifPresent(state -> {
            ItemStack stack = new ItemStack(state.getBlock().asItem());
            if (!stack.isEmpty()) {
                spawnAtLocation(level, stack);
            }
        });
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        getCarriedBlock().ifPresent(state ->
                output.putString("carried_block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String id = input.getStringOr("carried_block", "");
        if (!id.isEmpty()) {
            net.minecraft.resources.Identifier identifier = net.minecraft.resources.Identifier.tryParse(id);
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
