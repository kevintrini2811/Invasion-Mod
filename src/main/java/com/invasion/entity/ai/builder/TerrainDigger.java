package com.invasion.entity.ai.builder;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.invasion.Notifiable;
import com.invasion.entity.Miner;

public class TerrainDigger implements ITerrainDig, Notifiable {
    private Miner digger;
    private ITerrainModify modifier;
    private float digRate;

    public TerrainDigger(Miner digger, ITerrainModify modifier, float digRate) {
        this.digger = digger;
        this.modifier = modifier;
        this.digRate = digRate;
    }

    public void setDigRate(float digRate) {
        this.digRate = digRate;
    }

    public float getDigRate() {
        return digRate;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean askClearPosition(BlockPos pos, Notifiable onFinished, float costMultiplier) {
        List<ModifyBlockEntry> removals = new ArrayList<>();
        for (BlockPos removal : digger.getBlockRemovalOrder(pos)) {
            BlockState state = digger.getTerrain().getBlockState(removal);
            if (!state.isAir() && state.blocksMotion()) {
                if (!digger.canClearBlock(removal)) {
                    return false;
                }
                removals.add(ModifyBlockEntry.ofDeletion(
                        removal,
                        (int) (costMultiplier * digger.getBlockRemovalCost(removal) / digRate)
                ));
            }
        }

        return !removals.isEmpty() && modifier.requestTask(removals, onFinished, this);
    }

    @Override
    public boolean askRemoveBlock(BlockPos pos, Notifiable onFinished, float costMultiplier) {
        return digger.canClearBlock(pos) && modifier.requestTask(onFinished, this, ModifyBlockEntry.ofDeletion(pos, (int) (costMultiplier * digger.getBlockRemovalCost(pos) / digRate)));
    }

    @Override
    public void notifyTask(Status result) {
        if (result == Status.SUCCESS) {
            ModifyBlockEntry entry = modifier.getLastBlockModified();
            digger.onBlockRemoved(entry.pos(), entry.getOldBlock());
        }
    }
}
