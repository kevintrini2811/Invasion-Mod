package com.invasion.nexus.test;

import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.advancements.predicates.MinMaxBounds.Ints;
import net.minecraft.core.BlockPos;
import com.invasion.nexus.spawns.IMWaveSpawner;
import com.invasion.nexus.spawns.SpawnPoint;
import com.invasion.nexus.spawns.SpawnPointContainer;
import com.invasion.nexus.spawns.SpawnType;
import com.invasion.nexus.wave.WaveBuilder;
import com.invasion.nexus.wave.Wave;
import com.invasion.nexus.wave.WaveSpawnerException;

public class Tester {
    private final Random rand = new Random();

    private final Consumer<String> logger;

    public Tester(Consumer<String> logger) {
        this.logger = logger;
    }

    public void doWaveBuilderTest(float difficulty, float tierLevel, int lengthSeconds) {
        logger.accept("Doing wave builder test. Difficulty: " + difficulty + ", tier: " + tierLevel + ", length: " + lengthSeconds + " seconds");
        logger.accept("Generating dummy Nexus and fake spawn points...");
        DummyNexus nexus = new DummyNexus();
        SpawnPointContainer spawnPoints = new SpawnPointContainer();
        for (int i = -170; i < -100; i += 3) {
            spawnPoints.addSpawnPointXZ(new SpawnPoint(new BlockPos(i, 0, 0), i, SpawnType.HUMANOID));
        }
        for (int i = 90; i < 180; i += 3) {
            spawnPoints.addSpawnPointXZ(new SpawnPoint(new BlockPos(i, 0, 0), i, SpawnType.HUMANOID));
        }
        logger.accept("Setting radius to 45");
        IMWaveSpawner spawner = new IMWaveSpawner(nexus, 45);
        spawner.giveSpawnPoints(spawnPoints);
        spawner.debugMode(true);
        spawner.setPermitSpawns(false);

        WaveBuilder waveBuilder = new WaveBuilder();
        Wave wave = waveBuilder.generateWave(difficulty, tierLevel, lengthSeconds);

        int successfulSpawns = 0;
        int definedSpawns = 0;
        try {
            spawner.beginNextWave(wave);
            logger.accept("Starting wave.Wave duration: " + spawner.getWaveDuration());
            while (!spawner.isWaveComplete()) {
                spawner.spawn(100);
            }
            logger.accept("Wave finished spawning. Wave rest time: " + spawner.getWaveRestTime());
            successfulSpawns += spawner.getSuccessfulSpawnsThisWave();
            definedSpawns += spawner.getTotalDefinedMobsThisWave();
        } catch (WaveSpawnerException e) {
            logger.accept(e.getMessage());
        } catch (Exception e) {
            logger.accept(e.getMessage());
            e.printStackTrace();
        }

        logger.accept("Successful spawns for wave: " + spawner.getSuccessfulSpawnsThisWave());
        logger.accept("Test finished. Total successful spawns: " + successfulSpawns + "  Total defined spawns: " + definedSpawns);
    }

    public void doWaveSpawnerTest(int startWave, int endWave) {
        logger.accept("Doing wave spawner test. Start wave: " + startWave + "  End wave: " + endWave);
        logger.accept("Generating dummy Nexus and fake spawn points...");
        DummyNexus nexus = new DummyNexus();
        SpawnPointContainer spawnPoints = new SpawnPointContainer();
        for (int i = -170; i < -100; i += 3) {
            spawnPoints.addSpawnPointXZ(new SpawnPoint(new BlockPos(i, 0, 0), i, SpawnType.HUMANOID));
        }
        for (int i = 90; i < 180; i += 3) {
            spawnPoints.addSpawnPointXZ(new SpawnPoint(new BlockPos(i, 0, 0), i, SpawnType.HUMANOID));
        }
        logger.accept("Setting radius to 45");
        IMWaveSpawner spawner = new IMWaveSpawner(nexus, 45);
        spawner.giveSpawnPoints(spawnPoints);
        spawner.debugMode(true);
        spawner.setPermitSpawns(false);

        int successfulSpawns = 0;
        int definedSpawns = 0;
        for (; startWave <= endWave; startWave++) {
            try {
                spawner.beginNextWave(startWave);
                logger.accept("Starting wave " + startWave + ". Wave duration: " + spawner.getWaveDuration());
                while (!spawner.isWaveComplete()) {
                    spawner.spawn(100);
                }
                logger.accept("Wave finished spawning. Wave rest time: " + spawner.getWaveRestTime());
                successfulSpawns += spawner.getSuccessfulSpawnsThisWave();
                definedSpawns += spawner.getTotalDefinedMobsThisWave();
            } catch (WaveSpawnerException e) {
                logger.accept(e.getMessage());
            }
        }

        logger.accept("Successful spawns last wave: " + spawner.getSuccessfulSpawnsThisWave());
        logger.accept("Test finished. Total successful spawns: " + successfulSpawns + "  Total defined spawns: "
                + definedSpawns);
    }

    public void doSpawnPointSelectionTest() {
        logger.accept("Doing SpawnPointContainer test");
        logger.accept("Filling with spawn points...");
        SpawnPointContainer spawnPoints = new SpawnPointContainer();
        for (int i = -180; i < 180; i += this.rand.nextInt(3)) {
            spawnPoints.addSpawnPointXZ(new SpawnPoint(new BlockPos(i, 0, 0), i, SpawnType.HUMANOID));
        }
        logger.accept(spawnPoints.getNumberOfSpawnPoints(SpawnType.HUMANOID) + " random points in container");

        logger.accept("Cycling through ranges... format: min <= x < max");
        for (int i = -180; i < 180; i += 25) {
            int i2 = i + 40;
            if (i2 >= 180)
                i2 -= 360;
            logger.accept(i + " to " + i2);
            for (int j = 0; j < 4; j++) {
                SpawnPoint point = spawnPoints.getRandomSpawnPoint(SpawnType.HUMANOID, Ints.between(i, i2));
                if (point != null) {
                    logger.accept(point.toString());
                }
            }
        }
        logger.accept("Beginning random stress test");

        int count = 0;
        int count2 = 0;
        for (int i = 0; i < 1105000; i++) {
            int r = this.rand.nextInt(361) - 180;
            int r2 = this.rand.nextInt(361) - 180;
            for (int j = 0; j < 17; j++) {
                count++;
                SpawnPoint point = spawnPoints.getRandomSpawnPoint(SpawnType.HUMANOID, Ints.between(r, r2));
                if (point != null) {
                    if (r < r2) {
                        if ((point.getAngle() < r) || (point.getAngle() >= r2)) {
                            count2++;
                            logger.accept(point.toString() + " with specified: " + r + ", " + r2);
                        }

                    } else if ((point.getAngle() >= r) && (point.getAngle() < r2)) {
                        count2++;
                        logger.accept(point.toString() + " with specified: " + r + ", " + r2);
                    }
                }
            }

        }

        logger.accept("Tested " + count + " random spawn point retrievals. " + count2 + " results out of bounds.");

        logger.accept("Finished test.");
    }
}