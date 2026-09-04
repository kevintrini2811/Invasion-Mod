package com.invasion.entity;

import com.invasion.block.BlockMetadata;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface Miner extends NexusEntity {
    default float getDiggingSpeedMultiplier() {
        return 1.0F;
    }

    default float getMaxSelfDamage() {
        return 6;
    }

    default float getSelfDamage() {
        return 2;
    }

    default BlockPos[] getBlockRemovalOrder(BlockPos pos) {
        BlockPos entityPos = asEntity().blockPosition();
        if (entityPos.getY() >= pos.getY()) {
            return new BlockPos[] {
                pos,
                pos.above()
            };
        }

        return new BlockPos[] {
            pos.above(),
            entityPos.above(Mth.ceil(asEntity().getBbHeight())),
            pos
        };
    }

    default float getBlockRemovalCost(BlockPos pos) {
        return BlockMetadata.getStrength(pos, asEntity().level().getBlockState(pos), asEntity().level()) * 20;
    }

    default boolean canClearBlock(BlockPos pos) {
        return com.invasion.compat.ConfiguredModMobs.allowsMining(
                asEntity().getType(), true)
                && IMLandPathNodeMaker.canMineBlock(asEntity(), pos);
    }

    default void onBlockRemoved(BlockPos pos, BlockState state) {
        if (asEntity().getHealth() > asEntity().getMaxHealth() - getMaxSelfDamage()) {
            asEntity().hurt(asEntity().damageSources().generic(), getSelfDamage());
        }

        if (asEntity().tickCount % 5 == 0) {
            asEntity().playSound(state.getSoundType().getBreakSound(), 1.4F, 1F / (asEntity().getRandom().nextFloat() * 0.6F + 1));
        }
    }

    default BlockGetter getTerrain() {
        return asEntity().level();
    }
}
