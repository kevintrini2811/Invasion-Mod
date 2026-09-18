package com.invasion.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

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

    @Test
    void startsRepairAtNavigationArrivalWithoutAnotherPath() throws Exception {
        Class<? extends PigmanEngineerEntity> type = PigmanEngineerEntity.class;
        engineer = mock(type);
        ServerLevel level = mock(ServerLevel.class);
        var navigation = mock(net.minecraft.world.entity.ai.navigation.PathNavigation.class);
        var modifier = mock(com.invasion.entity.ai.builder.TerrainModifier.class);
        var nexus = mock(com.invasion.nexus.NexusAccess.class);
        var storage = new EngineerTowerStorage();
        var tower = new EngineerTower(base, Direction.NORTH);
        var modifierField = PigmanEngineerEntity.class.getDeclaredField("terrainModifier");
        modifierField.setAccessible(true);
        modifierField.set(engineer, modifier);
        when(engineer.level()).thenReturn(level);
        when(engineer.getNavigation()).thenReturn(navigation);
        when(engineer.getMoveControl()).thenReturn(mock(net.minecraft.world.entity.ai.control.MoveControl.class));
        when(engineer.getDeltaMovement()).thenReturn(net.minecraft.world.phys.Vec3.ZERO);
        when(engineer.hasNexus()).thenReturn(true);
        when(engineer.getNexus()).thenReturn(nexus);
        when(nexus.getOrigin()).thenReturn(base.above(10));
        when(engineer.onGround()).thenReturn(true);
        when(engineer.onClimbable()).thenReturn(true);
        when(engineer.blockPosition()).thenReturn(ladderBase);
        // Vanilla navigation can stop here, outside the old 0.4-block work radius.
        when(engineer.getX()).thenReturn(ladderBase.getX() + 0.5D + 0.6D);
        when(engineer.getY()).thenReturn((double) ladderBase.getY());
        when(engineer.getZ()).thenReturn(ladderBase.getZ() + 0.5D);
        BlockState repairMaterial = type == PigmanEngineerEntity.class
                ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
        BlockState originalMaterial = type == PigmanEngineerEntity.class
                ? Blocks.BRICKS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
        when(engineer.getBuildingBlock()).thenReturn(repairMaterial);
        // Ordinary mining may be disabled or reject blocks beside a ladder.
        when(engineer.canClearBlock(any())).thenReturn(false);
        when(level.hasChunkAt(any())).thenReturn(true);
        blocks.clear();
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(call -> {
            BlockPos pos = call.getArgument(0);
            return pos.getY() < base.getY() ? Blocks.STONE.defaultBlockState()
                    : blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        });
        apply(tower.plan(level, originalMaterial, pos -> 20));
        blocks.remove(base.above());
        blocks.remove(center.south());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 1; y <= 3; y++) blocks.put(center.offset(x, y, z), Blocks.STONE.defaultBlockState());
            }
        }
        storage.remember(tower);
        when(modifier.isReadyForTask(null)).thenReturn(true);
        when(modifier.requestTask(anyCollection(), any(), any())).thenReturn(true);
        doCallRealMethod().when(engineer).tryReuseExistingTower();
        doCallRealMethod().when(engineer).isBuildingTower();
        doCallRealMethod().when(engineer).isAtTowerBuildPosition();
        try (var storageAccess = mockStatic(EngineerTowerStorage.class,
                call -> call.getMethod().getName().equals("of") ? storage : call.callRealMethod());
                var config = mockStatic(com.invasion.compat.ConfiguredModMobs.class)) {
            storageAccess.when(() -> EngineerTowerStorage.of(level)).thenReturn(storage);
            config.when(() -> com.invasion.compat.ConfiguredModMobs.allowsEngineerTower(any(), anyBoolean()))
                    .thenReturn(true);
            assertTrue(engineer.tryReuseExistingTower());
        }
        assertTrue(engineer.isBuildingTower());
        assertTrue(engineer.isAtTowerBuildPosition());
        verify(navigation, never()).createPath(any(BlockPos.class), anyInt());
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<java.util.Collection<ModifyBlockEntry>> changes =
                org.mockito.ArgumentCaptor.forClass(java.util.Collection.class);
        verify(modifier).requestTask(changes.capture(), any(), any());
        assertEquals(27, changes.getValue().stream().filter(entry -> entry.newBlock().isAir()).count());
        apply(List.copyOf(changes.getValue()));
        assertEquals(originalMaterial, blocks.get(base));
        assertEquals(repairMaterial, blocks.get(base.above()));
        assertEquals(repairMaterial, blocks.get(center.south()));
        assertTrue(tower.plan(level, repairMaterial, pos -> 20).isEmpty());
        for (int second = 0; second < 30; second++) NexusBoundMobLifecycle.tickStationaryPathRecovery(engineer, nexus);
        verify(navigation, never()).moveTo(any(net.minecraft.world.level.pathfinder.Path.class), anyDouble());
        assertFalse(new com.invasion.entity.ai.goal.GoToNexusGoal(engineer).canUse());
        var mining = new com.invasion.entity.ai.goal.MineBlockGoal(engineer);
        assertFalse(mining.canUse());
        assertFalse(mining.canContinueToUse());
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
