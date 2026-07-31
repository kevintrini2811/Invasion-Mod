package com.invasion.entity.ai.builder;

import com.invasion.Notifiable;
import net.minecraft.core.BlockPos;

public interface ITerrainDig {
    boolean askRemoveBlock(BlockPos pos, Notifiable paramINotifyTask, float paramFloat);

    boolean askClearPosition(BlockPos pos, Notifiable paramINotifyTask, float paramFloat);
}