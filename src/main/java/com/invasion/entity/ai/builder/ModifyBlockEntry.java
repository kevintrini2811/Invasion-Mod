package com.invasion.entity.ai.builder;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record ModifyBlockEntry(
        BlockPos pos,
        AtomicReference<BlockState> oldBlock,
        BlockState newBlock,
        int cost
    ) {

    public static ModifyBlockEntry ofDeletion(BlockPos pos, int cost) {
        return new ModifyBlockEntry(pos, Blocks.AIR.defaultBlockState(), cost);
    }

    public ModifyBlockEntry(BlockPos pos, BlockState state, int cost) {
        this(pos, new AtomicReference<>(null), state, cost);
    }

    public int getCost() {
        return cost;
    }

    public BlockState getOldBlock() {
        return oldBlock.get();
    }

    public void setOldBlock(BlockState state) {
        oldBlock.set(state);
    }
}