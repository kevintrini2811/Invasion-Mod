package com.invasion.entity.ai.builder;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;

public interface ITerrainBuild {
    Stream<ModifyBlockEntry> askBuildBridge(BlockPos pos);
}
