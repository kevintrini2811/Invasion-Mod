package com.invasion.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.invasion.entity.ai.builder.ModifyBlockEntry;
import com.invasion.entity.ai.builder.EngineerTower;
import com.invasion.entity.ai.builder.EngineerTowerStorage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PigmanEngineerTowerTest {
    private final BlockPos base = new BlockPos(0, 64, 0);
    private final BlockPos center = base.above(3);
    private final BlockPos ladderBase = base.north();
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private PigmanEngineerEntity engineer;

    @BeforeEach
    void setUp() {
        ServerLevel level = mock(ServerLevel.class);
        engineer = mock(PigmanEngineerEntity.class);
        when(engineer.level()).thenReturn(level);
        when(engineer.getBuildingBlock()).thenReturn(Blocks.OAK_PLANKS.defaultBlockState());
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(call ->
                blocks.getOrDefault(call.getArgument(0), Blocks.AIR.defaultBlockState()));
        when(engineer.canClearBlock(any(BlockPos.class))).thenAnswer(call ->
                !blocks.getOrDefault(call.getArgument(0), Blocks.AIR.defaultBlockState()).is(Blocks.BEDROCK));
        when(engineer.getBlockRemovalCost(any(BlockPos.class))).thenReturn(20F);
    }

    @Test
    void reusesIntactTowerWithoutBuildingMoreBlocks() throws Exception {
        apply(plan());
        assertTrue(existing());
        assertTrue(plan().isEmpty());
    }

    @Test
    void repairsMissingLaddersSupportAndDeckBeforeClimbing() throws Exception {
        apply(plan());
        for (int height = 0; height <= 3; height++) {
            blocks.remove(ladderBase.above(height));
        }
        blocks.remove(base.above());
        blocks.remove(center.south().east());
        blocks.put(ladderBase.above(2), Blocks.STONE.defaultBlockState());
        assertTrue(existing(), "Recognize a tower even when all ladders are gone");
        assertTrue(buildable());
        apply(plan());
        for (int height = 0; height <= 3; height++) {
            assertEquals(Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH),
                    blocks.get(ladderBase.above(height)));
        }
        assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), blocks.get(base.above()));
        assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), blocks.get(center.south().east()));
        assertTrue(plan().isEmpty());
    }

    @Test
    void clearsExactlyThreeCompleteLayersAboveDeck() throws Exception {
        apply(plan());
        for (int height = 1; height <= 4; height++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    blocks.put(center.offset(x, height, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        List<ModifyBlockEntry> repairs = plan();
        assertEquals(27, repairs.size());
        apply(repairs);
        for (int height = 1; height <= 3; height++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    assertTrue(blocks.get(center.offset(x, height, z)).isAir());
                }
            }
        }
        assertTrue(blocks.get(center.above(4)).is(Blocks.STONE));
    }

    @Test
    void refusesProtectedHeadroomAndBlockedLadder() throws Exception {
        apply(plan());
        blocks.put(center.above(3), Blocks.BEDROCK.defaultBlockState());
        assertFalse(buildable());
        blocks.remove(center.above(3));
        blocks.put(ladderBase.above(), Blocks.BEDROCK.defaultBlockState());
        assertFalse(buildable());
    }

    @Test
    void doesNotRecognizeFlatBridgeAsTower() throws Exception {
        apply(plan());
        for (int height = 0; height < 3; height++) {
            blocks.remove(base.above(height));
        }
        for (int height = 0; height <= 3; height++) blocks.remove(ladderBase.above(height));
        assertFalse(existing());
    }

    @Test
    void repairsForeignTowerWithCurrentMaterialWithoutReplacingSurvivors() throws Exception {
        for (BlockState original : List.of(Blocks.BRICKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState())) {
            blocks.clear();
            when(engineer.getBuildingBlock()).thenReturn(original);
            apply(plan());
            BlockState repair = original.is(Blocks.BRICKS) ? Blocks.OAK_PLANKS.defaultBlockState()
                    : Blocks.BRICKS.defaultBlockState();
            when(engineer.getBuildingBlock()).thenReturn(repair);
            blocks.remove(base.above());
            blocks.remove(center.south());
            assertTrue(existing());
            apply(plan());
            assertEquals(original, blocks.get(base));
            assertEquals(repair, blocks.get(base.above()));
            assertEquals(repair, blocks.get(center.south()));
        }
    }

    @Test
    void recognizesUnfinishedForeignColumnWithOnlyTwoBlocks() throws Exception {
        blocks.put(base, Blocks.BRICKS.defaultBlockState());
        blocks.put(base.above(), Blocks.BRICKS.defaultBlockState());
        assertTrue(existing());
        apply(plan());
        assertEquals(Blocks.BRICKS.defaultBlockState(), blocks.get(base));
        assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), blocks.get(base.above(2)));
        assertTrue(plan().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private List<ModifyBlockEntry> plan() throws Exception {
        return (List<ModifyBlockEntry>) invoke("createTowerPlan",
                new Class<?>[] {BlockPos.class, BlockPos.class, Direction.class},
                ladderBase, base, Direction.NORTH);
    }

    private boolean existing() throws Exception {
        return EngineerTowerStorage.isLegacyTower(engineer.level(), new EngineerTower(base, Direction.NORTH));
    }

    private boolean buildable() throws Exception {
        return (boolean) invoke("canBuildTowerAt",
                new Class<?>[] {BlockPos.class, BlockPos.class}, ladderBase, base);
    }

    private Object invoke(String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = PigmanEngineerEntity.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(engineer, arguments);
    }

    private void apply(List<ModifyBlockEntry> entries) {
        for (ModifyBlockEntry entry : entries) {
            blocks.put(entry.pos(), entry.newBlock());
        }
    }
}
