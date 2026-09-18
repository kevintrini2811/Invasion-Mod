package com.invasion.compat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.invasion.nexus.NexusAccess;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfiguredLadderClimbTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void centersImmediatelyAndOwnsMovementWithRequestedOrCompletedPath(boolean requested) throws Exception {
        Mob mob = mock(Mob.class);
        ServerLevel level = mock(ServerLevel.class);
        PathNavigation navigation = mock(PathNavigation.class);
        MoveControl movement = mock(MoveControl.class);
        BlockPos ladder = new BlockPos(0, 64, 0);
        when(mob.level()).thenReturn(level);
        when(mob.getNavigation()).thenReturn(navigation);
        when(mob.getMoveControl()).thenReturn(movement);
        when(mob.blockPosition()).thenReturn(ladder.east());
        when(mob.getX()).thenReturn(1.4D);
        when(mob.getY()).thenReturn(64D);
        when(mob.getZ()).thenReturn(0.2D);
        when(level.getBlockState(any())).thenAnswer(call -> {
            BlockPos pos = call.getArgument(0);
            return pos.getX() == 0 && pos.getZ() == 0 && pos.getY() >= 64 && pos.getY() <= 67
                    ? Blocks.LADDER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        });
        Class<?> type = Class.forName("com.invasion.compat.ConfiguredModMobs$ClimbNexusLadderGoal");
        var constructor = type.getDeclaredConstructor(Mob.class);
        constructor.setAccessible(true);
        Goal goal = (Goal) constructor.newInstance(mob);
        if (requested) {
            var field = type.getDeclaredField("requestedLadder");
            field.setAccessible(true);
            field.set(goal, ladder);
        } else {
            Path path = new Path(List.of(new Node(0, 64, 0)), ladder, true);
            path.advance();
            assertTrue(path.isDone());
            when(navigation.getPath()).thenReturn(path);
        }
        try (var config = mockStatic(ConfiguredModMobs.class)) {
            config.when(() -> ConfiguredModMobs.towerNexus(mob)).thenReturn(mock(NexusAccess.class));
            assertTrue(goal.canUse());
            goal.start();
            verify(mob).setPos(0.5D, 64D, 0.5D);
            verify(navigation).stop();
            verify(movement).setWantedPosition(0.5D, 64D, 0.5D, 0D);
            goal.tick();
            verify(mob, times(2)).setPos(0.5D, 64D, 0.5D);
            verify(mob).setDeltaMovement(0D, 0.2D, 0D);
            goal.stop();
            verify(mob).setNoGravity(false);
            when(mob.getX()).thenReturn(10D);
            assertFalse(goal.canUse(), "Never teleport to a distant ladder");
        }
    }
}
