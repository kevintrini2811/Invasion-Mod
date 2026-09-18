package com.invasion.compat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.invasion.entity.ai.builder.EngineerTower;
import com.invasion.entity.ai.builder.EngineerTowerStorage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.params.ParameterizedTest;

class ConfiguredEngineerTowerTest {
    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("towerBuilders")
    void configuredMobRepairsForeignTowerAndClearsThreeLayersBeforeClimbing(
            Class<? extends Mob> mobType, boolean blockedShaft) throws Exception {
        ServerLevel level = mock(ServerLevel.class);
        Mob mob = mock(mobType);
        when(mob.level()).thenReturn(level);
        PathNavigation navigation = mock(PathNavigation.class);
        when(mob.getNavigation()).thenReturn(navigation);
        when(mob.getLookControl()).thenReturn(mock(LookControl.class));
        when(mob.getMoveControl()).thenReturn(mock(net.minecraft.world.entity.ai.control.MoveControl.class));
        when(mob.getDeltaMovement()).thenReturn(net.minecraft.world.phys.Vec3.ZERO);
        EngineerTower tower = new EngineerTower(new BlockPos(0, 64, 0), Direction.NORTH);
        BlockPos work = blockedShaft ? tower.ladderBase().north() : tower.ladderBase();
        double[] position = {work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D};
        when(mob.getX()).thenAnswer(call -> position[0]);
        when(mob.getY()).thenAnswer(call -> position[1]);
        when(mob.getZ()).thenAnswer(call -> position[2]);
        when(mob.blockPosition()).thenAnswer(call -> BlockPos.containing(position[0], position[1], position[2]));
        when(navigation.moveTo(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenAnswer(call -> {
            for (int axis = 0; axis < 3; axis++) position[axis] = call.getArgument(axis);
            return true;
        });
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        if (blockedShaft) blocks.put(tower.ladderBase(), Blocks.STONE.defaultBlockState());
        blocks.put(tower.base(), Blocks.BRICKS.defaultBlockState());
        blocks.put(tower.base().above(), Blocks.BRICKS.defaultBlockState());
        blocks.put(tower.center().above(3), Blocks.STONE.defaultBlockState());
        blocks.put(tower.center().above(4), Blocks.STONE.defaultBlockState());
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(call ->
                blocks.getOrDefault(call.getArgument(0), Blocks.AIR.defaultBlockState()));
        when(level.setBlock(any(BlockPos.class), any(BlockState.class), anyInt())).thenAnswer(call -> {
            blocks.put(call.getArgument(0), call.getArgument(1));
            return true;
        });
        when(level.destroyBlock(any(BlockPos.class), anyBoolean(), eq(mob))).thenAnswer(call -> {
            blocks.remove(call.getArgument(0));
            return true;
        });
        Class<?> goalClass = Class.forName("com.invasion.compat.ConfiguredModMobs$ConfiguredTerrainGoal");
        Constructor<?> constructor = goalClass.getDeclaredConstructor(Mob.class);
        constructor.setAccessible(true);
        Object goal = constructor.newInstance(mob);
        var selectorField = Mob.class.getDeclaredField("goalSelector");
        selectorField.setAccessible(true);
        var selector = new net.minecraft.world.entity.ai.goal.GoalSelector();
        selectorField.set(mob, selector);
        when(((com.invasion.mixin.MobAccessor) mob).invmod$getGoalSelector()).thenReturn(selector);
        Class<?> climbClass = Class.forName("com.invasion.compat.ConfiguredModMobs$ClimbNexusLadderGoal");
        var climbConstructor = climbClass.getDeclaredConstructor(Mob.class);
        climbConstructor.setAccessible(true);
        var climb = (net.minecraft.world.entity.ai.goal.Goal) climbConstructor.newInstance(mob);
        selector.addGoal(3, climb);
        EngineerTowerStorage storage = mock(EngineerTowerStorage.class);
        try (var storageAccess = mockStatic(EngineerTowerStorage.class);
                var config = mockStatic(ConfiguredModMobs.class)) {
            storageAccess.when(() -> EngineerTowerStorage.of(level)).thenReturn(storage);
            config.when(() -> ConfiguredModMobs.buildingBlock(any(), any())).thenReturn(Blocks.OAK_PLANKS);
            assertEquals(true, invoke(goal, "beginTower", new Class<?>[] {EngineerTower.class, BlockPos.class}, tower, work));
            verify(storage).remember(tower);
            int actions = 0;
            while ((boolean) invoke(goal, "nextTowerBlock", new Class<?>[0])) {
                assertTrue(++actions < 50);
                invoke(goal, "start", new Class<?>[0]);
                var actionTicks = goalClass.getDeclaredField("actionTicks");
                actionTicks.setAccessible(true);
                for (int tick = 0; tick < 30 && actionTicks.getInt(goal) < 20; tick++) {
                    invoke(goal, "tick", new Class<?>[0]);
                }
                assertEquals(20, actionTicks.getInt(goal));
            }
        }
        assertEquals(Blocks.BRICKS.defaultBlockState(), blocks.get(tower.base()));
        assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), blocks.get(tower.base().above(2)));
        assertFalse(blocks.containsKey(tower.center().above(3)));
        assertEquals(Blocks.STONE.defaultBlockState(), blocks.get(tower.center().above(4)));
        assertTrue(tower.plan(level, Blocks.OAK_PLANKS.defaultBlockState(), pos -> 20).isEmpty());
        assertEquals(tower.ladderBase(), mob.blockPosition());
        var requestedLadder = climbClass.getDeclaredField("requestedLadder");
        requestedLadder.setAccessible(true);
        assertEquals(tower.ladderBase(), requestedLadder.get(climb));
        verify(mob, never()).setPos(anyDouble(), anyDouble(), anyDouble());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(classes = {
            Mob.class, com.invasion.entity.ZombieBuilderEntity.class, com.invasion.entity.ZombieMinerEntity.class})
    void searchesWhileWalkingAndStartsAtNavigationArrival(Class<? extends Mob> mobType) throws Exception {
        ServerLevel level = mock(ServerLevel.class);
        Mob mob = mock(mobType);
        var navigation = mock(PathNavigation.class);
        var rules = mock(net.minecraft.world.level.gamerules.GameRules.class);
        var nexus = mock(com.invasion.nexus.NexusAccess.class);
        var storage = mock(EngineerTowerStorage.class);
        var tower = new EngineerTower(new BlockPos(0, 64, 0), Direction.NORTH);
        BlockPos work = tower.ladderBase();
        when(mob.level()).thenReturn(level);
        when(mob.getNavigation()).thenReturn(navigation);
        when(mob.onGround()).thenReturn(true);
        when(mob.onClimbable()).thenReturn(true);
        when(mob.blockPosition()).thenReturn(work);
        when(mob.getX()).thenReturn(work.getX() + 0.5D + 0.6D);
        when(mob.getY()).thenReturn((double) work.getY());
        when(mob.getZ()).thenReturn(work.getZ() + 0.5D);
        when(navigation.isDone()).thenReturn(false);
        when(nexus.getOrigin()).thenReturn(tower.base().above(10));
        when(level.getGameRules()).thenReturn(rules);
        when(rules.get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)).thenReturn(true);
        when(level.getBlockState(any(BlockPos.class))).thenAnswer(call -> {
            BlockPos pos = call.getArgument(0);
            return pos.getY() < work.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        });
        when(storage.nearby(level, work, nexus.getOrigin())).thenReturn(java.util.List.of(tower));
        Class<?> goalClass = Class.forName("com.invasion.compat.ConfiguredModMobs$ConfiguredTerrainGoal");
        Constructor<?> constructor = goalClass.getDeclaredConstructor(Mob.class);
        constructor.setAccessible(true);
        var selector = new net.minecraft.world.entity.ai.goal.GoalSelector();
        var selectorField = Mob.class.getDeclaredField("goalSelector");
        selectorField.setAccessible(true);
        selectorField.set(mob, selector);
        when(((com.invasion.mixin.MobAccessor) mob).invmod$getGoalSelector()).thenReturn(selector);
        net.minecraft.world.entity.ai.goal.Goal goal;
        if (mob instanceof com.invasion.entity.ZombieBuilderEntity zombie) {
            when(zombie.getNexus()).thenReturn(nexus);
            ConfiguredModMobs.addEngineerTowerGoals(mob);
            goal = selector.getAvailableGoals().stream().map(wrapped -> wrapped.getGoal())
                    .filter(goalClass::isInstance).findFirst().orElseThrow();
        } else {
            goal = (net.minecraft.world.entity.ai.goal.Goal) constructor.newInstance(mob);
            selector.addGoal(2, goal);
        }
        try (var storageAccess = mockStatic(EngineerTowerStorage.class);
                var config = mockStatic(ConfiguredModMobs.class)) {
            storageAccess.when(() -> EngineerTowerStorage.of(level)).thenReturn(storage);
            if (mob instanceof com.invasion.entity.ZombieBuilderEntity) {
                config.when(() -> ConfiguredModMobs.towerNexus(mob)).thenCallRealMethod();
            } else {
                config.when(() -> ConfiguredModMobs.towerNexus(mob)).thenReturn(nexus);
            }
            config.when(() -> ConfiguredModMobs.allowsEngineerTower(any(), anyBoolean())).thenReturn(true);
            config.when(() -> ConfiguredModMobs.buildingBlock(any(), any())).thenReturn(Blocks.OAK_PLANKS);
            config.when(() -> ConfiguredModMobs.isWorkingOnEngineerTower(mob)).thenCallRealMethod();
            assertTrue(goal.canUse(), "Reuse must not wait until the Nexus path stalls");
            selector.getAvailableGoals().iterator().next().start();
            clearInvocations(navigation);
            assertTrue(ConfiguredModMobs.isWorkingOnEngineerTower(mob));
            for (int second = 0; second < 30; second++) {
                com.invasion.entity.NexusBoundMobLifecycle.tickStationaryPathRecovery(mob, nexus);
            }
            verify(navigation, never()).stop();
            invoke(goal, "clearTowerWork", new Class<?>[0]);
            assertFalse(ConfiguredModMobs.isWorkingOnEngineerTower(mob));
            if (mob instanceof com.invasion.entity.ZombieBuilderEntity) {
                mob.tickCount = 45;
                when(navigation.isDone()).thenReturn(true);
                config.when(() -> ConfiguredModMobs.allowsEngineerTower(any(), anyBoolean())).thenReturn(false);
                assertFalse(goal.canUse(), "Disabling engineer_tower must disable the zombie tower controller");
            }
        }
        verify(navigation, never()).createPath(any(BlockPos.class), anyInt());
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> towerBuilders() {
        return java.util.stream.Stream.of(Mob.class, com.invasion.entity.ZombieBuilderEntity.class,
                com.invasion.entity.ZombieMinerEntity.class).flatMap(type -> java.util.stream.Stream.of(
                        org.junit.jupiter.params.provider.Arguments.of(type, false),
                        org.junit.jupiter.params.provider.Arguments.of(type, true)));
    }

    private Object invoke(Object goal, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = goal.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(goal, args);
    }
}
