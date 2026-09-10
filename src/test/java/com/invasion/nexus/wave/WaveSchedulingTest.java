package com.invasion.nexus.wave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WaveSchedulingTest {
    @Test
    void completesOnlyAfterScheduledDuration() {
        Wave wave = Wave.builder(1_000, 250).build();

        wave.doNextSpawns(1_000, null);
        assertFalse(wave.isComplete());

        wave.doNextSpawns(1, null);
        assertTrue(wave.isComplete());
        assertEquals(1_001, wave.getTimeInWave());
        assertEquals(250, wave.getWaveBreakTime());
    }

    @Test
    void resetRestoresWaveStart() {
        Wave wave = Wave.builder(1_000, 250).build();
        wave.doNextSpawns(1_001, null);

        wave.resetWave();

        assertEquals(0, wave.getTimeInWave());
        assertFalse(wave.isComplete());
    }

    @Test
    void restoresSavedElapsedTime() {
        Wave wave = Wave.builder(1_000, 250).build();

        wave.setWaveToTime(600);

        assertEquals(600, wave.getTimeInWave());
        assertFalse(wave.isComplete());
    }
}
