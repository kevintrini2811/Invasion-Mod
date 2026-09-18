package com.invasion.entity.pathfinding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.nexus.NexusAccess;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BuilderIMMobNavigationTest {
    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void claimsTowerBeforeOrdinaryNavigationOrRecovery(boolean pathDone) throws Exception {
        var mob = mock(PigmanEngineerEntity.class);
        var nexus = mock(NexusAccess.class);
        var navigation = mock(BuilderIMMobNavigation.class);
        var mobField = PathNavigation.class.getDeclaredField("mob");
        mobField.setAccessible(true);
        mobField.set(navigation, mob);
        when(mob.hasNexus()).thenReturn(true);
        when(mob.getNexus()).thenReturn(nexus);
        when(mob.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        when(nexus.getOrigin()).thenReturn(new BlockPos(0, 74, 0));
        when(navigation.isDone()).thenReturn(pathDone);
        AtomicBoolean building = new AtomicBoolean();
        when(mob.isBuildingTower()).thenAnswer(call -> building.get());
        when(mob.isAtTowerBuildPosition()).thenReturn(true);
        when(mob.tryStartTowerBuild()).thenAnswer(call -> { building.set(true); return true; });
        when(mob.tryReuseExistingTower()).thenAnswer(call -> { building.set(true); return true; });
        doCallRealMethod().when(navigation).tick();
        navigation.tick();
        assertTrue(building.get());
        verify(navigation).stop();
        if (pathDone) verify(mob).tryStartTowerBuild();
        else verify(mob).tryReuseExistingTower();
        // A second navigation tick holds the work position instead of restarting the job.
        navigation.tick();
        verify(navigation, times(2)).stop();
        if (pathDone) verify(mob, times(1)).tryStartTowerBuild();
        else verify(mob, times(1)).tryReuseExistingTower();
    }
}
