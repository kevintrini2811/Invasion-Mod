package com.invasion.nexus.spawns;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.Participants;
import com.invasion.nexus.wave.Wave;
import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.Test;

class SpawnFailureTest {
    private final NexusAccess nexus = mock(NexusAccess.class);
    private final Participants participants = mock(Participants.class);
    private final Wave wave = mock(Wave.class);

    private IMWaveSpawner spawner(int successful) throws Exception {
        IMWaveSpawner spawner = mock(IMWaveSpawner.class, CALLS_REAL_METHODS);
        set(spawner, "nexus", nexus);
        set(spawner, "currentWave", wave);
        set(spawner, "active", true);
        set(spawner, "pendingResumeElapsed", -1L);
        set(spawner, "spawnPointLayer", SpawnLayer.BOTH);
        set(spawner, "respawnQueue", new ArrayList<>());
        set(spawner, "successfulSpawns", successful);
        set(spawner, "spawnPointContainer", new SpawnPointContainer());
        when(nexus.getWorld()).thenReturn(mock(ServerLevel.class));
        when(nexus.getParticipants()).thenReturn(participants);
        when(wave.getTotalMobAmount()).thenReturn(5);
        when(wave.getWaveTotalTime()).thenReturn(1_000);
        return spawner;
    }

    @Test
    void blockedSpawnsExpireOnceWithExactMissingCount() throws Exception {
        IMWaveSpawner spawner = spawner(3);
        spawner.spawn(30_999);
        verifyNoInteractions(participants);
        assertFalse(spawner.isWaveComplete());
        spawner.spawn(1);
        assertTrue(spawner.isWaveComplete());
        spawner.spawn(1_000);
        verify(nexus).notifySpawnsSkipped(2);
        verify(participants).sendWarning("invmod.message.nexus.spawn_failed", 2, "both");
        verify(wave).discardPendingSpawns();
    }

    @Test
    void zeroSpawnPointsStillReachFailsafe() throws Exception {
        IMWaveSpawner spawner = spawner(0);
        spawner.spawn(31_000);
        assertTrue(spawner.isWaveComplete());
        verify(nexus).notifySpawnsSkipped(5);
        verify(participants).sendWarning("invmod.message.nexus.spawn_failed", 5, "both");
    }

    @Test
    void successfulWaveDoesNotWarnOrChangeCounters() throws Exception {
        IMWaveSpawner spawner = spawner(5);
        when(wave.isComplete()).thenReturn(true);
        spawner.spawn(31_000);
        assertTrue(spawner.isWaveComplete());
        verifyNoInteractions(participants);
        verify(nexus, never()).notifySpawnsSkipped(anyInt());
    }

    @Test
    void phaseTimeoutReportsUnscheduledMobsOnlyOnce() throws Exception {
        IMWaveSpawner spawner = spawner(1);
        spawner.finishSpawning();
        spawner.finishSpawning();
        assertTrue(spawner.isWaveComplete());
        verify(nexus).notifySpawnsSkipped(4);
        verify(participants).sendWarning("invmod.message.nexus.spawn_failed", 4, "both");
    }

    private static void set(IMWaveSpawner spawner, String name, Object value) throws Exception {
        var field = IMWaveSpawner.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(spawner, value);
    }
}
