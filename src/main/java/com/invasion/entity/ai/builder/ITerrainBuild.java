package com.invasion.entity.ai.builder;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface ITerrainBuild {
    Stream<ModifyBlockEntry> askBuildScaffoldLayer(BlockPos pos);

    Stream<ModifyBlockEntry> askBuildLadderTower(BlockPos pos, Direction orientation, int layersToBuild);

    Stream<ModifyBlockEntry> askBuildLadder(BlockPos pos, Direction orientation);

    Stream<ModifyBlockEntry> askBuildBridge(BlockPos pos);
}