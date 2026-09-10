package com.invasion.nexus.wave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.spawns.SpawnType;
import com.invasion.nexus.spawns.Spawner;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.predicates.MinMaxBounds.Ints;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WaveSchedulingTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void schedulesPlannedSpawnsAcrossWaveDuration() {
        Wave wave = waveWithTwoSpawns();
        RecordingSpawner spawner = new RecordingSpawner(true);

        assertEquals(1, wave.doNextSpawns(500, spawner));
        assertEquals(1, spawner.attempts);
        assertFalse(wave.isComplete());

        assertEquals(1, wave.doNextSpawns(500, spawner));
        assertEquals(2, spawner.attempts);
        assertFalse(wave.isComplete());

        assertEquals(0, wave.doNextSpawns(1, spawner));
        assertTrue(wave.isComplete());
    }

    @Test
    void retriesBlockedSpawnAfterRetryDelayAndPastSchedulingWindow() {
        Wave wave = Wave.builder(1_000, 0)
                .entry(WaveEntry.finite()
                        .entry(new EntityPattern.Builder(EntityTypes.ZOMBIE).build(), 1)
                        .end(1_000)
                        .amount(1)
                        .granularity(1_000))
                .build();
        RecordingSpawner spawner = new RecordingSpawner(false);

        assertEquals(0, wave.doNextSpawns(1_000, spawner));
        assertEquals(1, spawner.attempts);

        spawner.allowSpawn = true;
        assertEquals(0, wave.doNextSpawns(999, spawner));
        assertEquals(1, spawner.attempts);
        assertEquals(1, wave.doNextSpawns(1, spawner));
        assertEquals(2, spawner.attempts);
        assertTrue(wave.isComplete());
    }

    @Test
    void replayingSavedElapsedTimeRestoresSchedulingProgress() {
        Wave reloadedWave = waveWithTwoSpawns();
        RecordingSpawner replaySpawner = new RecordingSpawner(true);
        int replayedSpawns = 0;

        for (int elapsed = 0; elapsed < 400; elapsed += 100) {
            replayedSpawns += reloadedWave.doNextSpawns(100, replaySpawner);
        }

        assertEquals(1, replayedSpawns);
        assertEquals(400, reloadedWave.getTimeInWave());
        assertEquals(1, reloadedWave.doNextSpawns(100, replaySpawner));
        assertEquals(2, replaySpawner.attempts);
    }

    private static Wave waveWithTwoSpawns() {
        return Wave.builder(1_000, 250)
                .entry(WaveEntry.finite()
                        .entry(new EntityPattern.Builder(EntityTypes.ZOMBIE).build(), 2)
                        .end(1_000)
                        .amount(2)
                        .granularity(500))
                .build();
    }

    private static final class RecordingSpawner implements Spawner {
        private final RandomSource random = RandomSource.create(1L);
        private boolean allowSpawn;
        private int attempts;

        private RecordingSpawner(boolean allowSpawn) {
            this.allowSpawn = allowSpawn;
        }

        @Override
        public RandomSource getRandom() {
            return random;
        }

        @Override
        public boolean attemptSpawn(EntityConstruct mobConstruct, Ints angle) {
            attempts++;
            return allowSpawn;
        }

        @Override
        public int getNumberOfPointsInRange(Ints angle, SpawnType type) {
            return 10;
        }

        @Override
        public void sendSpawnAlert(String message, ChatFormatting color) {
        }

        @Override
        public void noSpawnPointNotice() {
        }
    }
}
