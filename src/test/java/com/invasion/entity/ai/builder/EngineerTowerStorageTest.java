package com.invasion.entity.ai.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EngineerTowerStorageTest {
    private final BlockPos base = new BlockPos(0, 64, 0);
    private final EngineerTower tower = new EngineerTower(base, Direction.NORTH);
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final ServerLevel level = mock(ServerLevel.class);

    @BeforeEach
    void setUp() {
        when(level.hasChunkAt(any(BlockPos.class))).thenReturn(true);
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(call ->
                blocks.getOrDefault(call.getArgument(0), Blocks.AIR.defaultBlockState()));
    }

    @Test
    void recognizesEveryPairOfSurvivingBlocksRegardlessOfMaterial() {
        var full = tower.plan(level, Blocks.DIAMOND_BLOCK.defaultBlockState(), pos -> 20);
        for (int first = 0; first < full.size(); first++) {
            for (int second = first + 1; second < full.size(); second++) {
                blocks.clear();
                blocks.put(full.get(first).pos(), full.get(first).newBlock());
                blocks.put(full.get(second).pos(), full.get(second).newBlock());
                assertTrue(tower.hasRemains(level), "Missing pair " + first + ", " + second);
            }
        }
        blocks.clear();
        blocks.put(base, Blocks.DIAMOND_BLOCK.defaultBlockState());
        assertFalse(tower.hasRemains(level));
    }

    @Test
    void searchesSixteenBlocksButNotSeventeen() {
        BlockPos objective = base.above(10);
        assertTrue(EngineerTowerStorage.inRange(tower, base.east(16), objective));
        assertFalse(EngineerTowerStorage.inRange(tower, base.east(17), objective));
        assertTrue(EngineerTowerStorage.inRange(tower, base.north(16), objective));
        assertFalse(EngineerTowerStorage.inRange(tower, base.north(17), objective));
    }

    @SuppressWarnings("unchecked")
    @Test
    void savedFootprintSurvivesReloadWithOnlyTwoDeckBlocks() throws Exception {
        EngineerTowerStorage storage = new EngineerTowerStorage();
        storage.remember(tower);
        blocks.put(tower.center().west(), Blocks.DIAMOND_BLOCK.defaultBlockState());
        blocks.put(tower.center().east(), Blocks.EMERALD_BLOCK.defaultBlockState());
        var field = EngineerTowerStorage.class.getDeclaredField("TYPE");
        field.setAccessible(true);
        SavedDataType<EngineerTowerStorage> type = (SavedDataType<EngineerTowerStorage>) field.get(null);
        Codec<EngineerTowerStorage> codec = type.codec();
        var encoded = codec.encodeStart(JsonOps.INSTANCE, storage).getOrThrow();
        EngineerTowerStorage loaded = codec.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(java.util.List.of(tower), loaded.nearby(level, base.east(16), base.above(10)));
        when(level.hasChunkAt(tower.center().west())).thenReturn(false);
        assertTrue(loaded.nearby(level, base.east(16), base.above(10)).isEmpty());
    }
}
