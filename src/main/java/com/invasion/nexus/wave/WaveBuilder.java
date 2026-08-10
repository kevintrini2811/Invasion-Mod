package com.invasion.nexus.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.invasion.InvasionMod;
import com.invasion.entity.InvEntities;
import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.wave.pool.Select;

public class WaveBuilder {
    public static final int WAVES_DEFINED = 11;
    private static final float ZOMBIE_T1_WEIGHT = 1;
    private static final float ZOMBIE_T2_WEIGHT = 2;
    private static final float SPIDER_T1_WEIGHT = 1;
    private static final float SPIDER_T2_WEIGHT = 2;

    private Random rand = new Random();

    public Wave generateWave(float difficulty, final float tierLevel, int lengthSeconds) {
        float basicMobsPerSecond = 0.12F * difficulty;
        int numberOfGroups = 7;
        int numberOfBigGroups = 1;
        float proportionInGroups = 0.5F;
        int mobsPerGroup = Math.round(proportionInGroups * basicMobsPerSecond * lengthSeconds / (numberOfGroups + numberOfBigGroups * 2));
        int mobsPerBigGroup = mobsPerGroup * 2;
        int remainingMobs = (int) (basicMobsPerSecond * lengthSeconds) - mobsPerGroup * numberOfGroups - mobsPerBigGroup * numberOfBigGroups;
        int mobsPerSteady = Math.round(0.7F * remainingMobs / numberOfGroups);
        int extraMobsForFinale = Math.round(0.3F * remainingMobs);
        int extraMobsForCleanup = (int) (basicMobsPerSecond * lengthSeconds * 0.2F);
        float timeForGroups = 0.5F;
        int groupTimeInterval = (int) (lengthSeconds * 1000 * timeForGroups / (numberOfGroups + numberOfBigGroups * 3));
        int steadyTimeInterval = (int) (lengthSeconds * 1000 * (1 - timeForGroups) / numberOfGroups);

        int time = 0;

        var steadyPool = generateSteadyPool(tierLevel);
        var groupPool = generateGroupPool(tierLevel);

        List<WaveEntry.Builder<?>> entryList = new ArrayList<>();
        for (int i = 0; i < numberOfGroups; i++) {
            if (rand.nextInt(2) == 0) {
                entryList.add(WaveEntry.builder(groupPool).begin(time).end(time + 3500).amount(mobsPerGroup).granularity(500).angle(25).minSpawns(3));
                entryList.add(WaveEntry.builder(steadyPool).begin(time += groupTimeInterval).end(time += steadyTimeInterval).amount(mobsPerSteady).granularity(2000).angle(160).minSpawns(5));
            } else {
                entryList.add(WaveEntry.builder(steadyPool).begin(time).end(time += steadyTimeInterval).amount(mobsPerSteady).granularity(2000).angle(160).minSpawns(5));
                entryList.add(WaveEntry.builder(groupPool).begin(time).end(time + 5000).amount(mobsPerGroup).granularity(500).angle(25).minSpawns(3));
                time += groupTimeInterval;
            }
        }

        time += (int) (groupTimeInterval * 0.75D);

        int steadyPoolFinalBegin = time + 5000;
        int steadyPoolFinalEnd = (int) (time + groupTimeInterval * 2.25F);
        int steadyPoolCount = extraMobsForFinale / 2;

        var cleanupEntry = WaveEntry.builder(steadyPool).begin(steadyPoolFinalBegin).end(steadyPoolFinalEnd).amount(steadyPoolCount).granularity(500).angle(160).minSpawns(5);

        return new Wave.Builder(time + 16000, steadyPoolFinalEnd * 3, entryList)
                .entry(WaveEntry.finite()
                        .entry(EntityPatterns.THROWER_T1, mobsPerBigGroup / 5)
                        .entry(generateGroupPool(tierLevel + 0.5F, 6), mobsPerBigGroup)
                        .begin(time).end(time + 8000).amount(mobsPerBigGroup + mobsPerBigGroup / 7).granularity(500).angle(45).minSpawns(3).alert("invmod.message.wave.grouplarge", 0))
                .entry(cleanupEntry)
                .entry(cleanupEntry)
                .entry(cleanupEntry)
                .entry(WaveEntry.builder(steadyPool).begin(time + 15000).end((int) (time + 10000 + groupTimeInterval * 2.25F)).amount(extraMobsForCleanup).granularity(500))
                .build();
    }

    private Select.PoolBuilder<EntityPattern, Float> generateGroupPool(float tierLevel) {
        return generateGroupPool(tierLevel, 6);
    }

    private Select.PoolBuilder<EntityPattern, Float> generateGroupPool(float tierLevel, float weight) {
        float[] weights = new float[6];
        for (int i = 0; i < 6; i++) {
            if (tierLevel - i * 0.5F > 0) {
                weights[i] = (tierLevel - i <= 1 ? tierLevel - i * 0.5F : 1);
            }
        }
        return Select.<EntityPattern>random()
                .entry(Select.<EntityPattern>random()
                        .entry(Select.<EntityPattern>random()
                                .entry(EntityPatterns.ZOMBIE_T1_ANY, ZOMBIE_T1_WEIGHT * weights[0])
                                .entry(EntityPatterns.ZOMBIE_T2_ANY_BASIC, ZOMBIE_T2_WEIGHT * Math.max(weights[2], 0.1F))
                                .entry(EntityPatterns.ZOMBIE_PIGMAN_T1_ANY, ZOMBIE_T1_WEIGHT * Math.max(weights[3], 0.1F))
                                .entry(EntityPatterns.ZOMBIE_PIGMAN_T2_ANY, 0.3F * Math.max(weights[2], 0.1F))
                                .entry(EntityPatterns.ZOMBIE_PIGMAN_T3_ANY, 0.1F * Math.max(weights[5], 0.05F)), 3.1F)
                        .entry(Select.<EntityPattern>random()
                                .entry(EntityPatterns.SPIDER_T1_ANY, SPIDER_T1_WEIGHT * weights[0])
                                .entry(EntityPatterns.SPIDER_T2_ANY, SPIDER_T2_WEIGHT * Math.max(weights[2], 0.1F)), 0.7F)
                        .entry(EntityPatterns.SKELETON_T1_ANY, 0.8F), weight * 0.8333333F)
                .entry(Select.<EntityPattern>random()
                        .entry(EntityPatterns.PIGMAN_ENGINEER_T1_ANY, 4F)
                        .entry(EntityPatterns.THROWER_T1, 1.1F * Math.max(weights[4], 0.1F))
                        .entry(EntityPatterns.THROWER_T2, 0.2F * Math.max(weights[5], 0.05F))
                        .entry(EntityPatterns.BURROWER, 0.12F)
                        .entry(EntityPatterns.IMP_T1, 0.35F)
                        .entry(EntityPatterns.ZOMBIE_T3_ANY, 1.1F * Math.max(weights[5], 0.03F))
                        .entry(EntityPatterns.ENDERMAN_T1, 0.15F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.WITCH, 0.15F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.SLIME, 0.15F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.BREEZE, 0.15F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.PHANTOM_T1, 0.08F * Math.max(weights[3], 0.05F))
                        .entry(EntityPatterns.CREEPER_T1_BASIC, 0.7F * Math.max(weights[3], 0.1F)), weight * 0.1666667F);
    }

    private Select.PoolBuilder<EntityPattern, Float> generateSteadyPool(float tierLevel) {
        float[] weights = new float[6];
        for (int i = 0; i < weights.length; i++) {
            if (tierLevel - i * 0.5F > 0) {
                weights[i] = (tierLevel - i <= 1 ? tierLevel - i * 0.5F : 1);
            }
        }

        return Select.<EntityPattern>random()
                .entry(Select.<EntityPattern>random()
                        .entry(Select.<EntityPattern>random()
                                .entry(EntityPatterns.ZOMBIE_T1_ANY, ZOMBIE_T1_WEIGHT * weights[0])
                                .entry(EntityPatterns.ZOMBIE_T2_ANY_BASIC, ZOMBIE_T2_WEIGHT * Math.max(weights[2], 0.1F))
                                .entry(EntityPatterns.ZOMBIE_PIGMAN_T1_ANY, ZOMBIE_T1_WEIGHT * Math.max(weights[3], 0.1F))
                                .entry(EntityPatterns.ZOMBIE_PIGMAN_T2_ANY, 0.3F * Math.max(weights[2], 0.1F))
                                .entry(EntityPatterns.ZOMBIE_PIGMAN_T3_ANY, 0.1F * Math.max(weights[5], 0.05F)), 3.1F)
                        .entry(Select.<EntityPattern>random()
                                .entry(EntityPatterns.SPIDER_T1_ANY, SPIDER_T1_WEIGHT * weights[0])
                                .entry(EntityPatterns.SPIDER_T2_ANY, SPIDER_T2_WEIGHT * Math.max(weights[2], 0.1F)), 0.7F)
                        .entry(EntityPatterns.SKELETON_T1_ANY, 0.8F), 9F)
                .entry(Select.<EntityPattern>random()
                        .entry(EntityPatterns.PIGMAN_ENGINEER_T1_ANY, 3F)
                        .entry(EntityPatterns.THROWER_T1, 0.5F * Math.max(weights[4], 0.1F))
                        .entry(EntityPatterns.THROWER_T2, 0.12F * Math.max(weights[5], 0.05F))
                        .entry(EntityPatterns.BURROWER, 0.08F)
                        .entry(EntityPatterns.IMP_T1, 0.25F)
                        .entry(EntityPatterns.ZOMBIE_T3_ANY, 1.1F * Math.max(weights[5], 0.03F))
                        .entry(EntityPatterns.ENDERMAN_T1, 0.12F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.WITCH, 0.12F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.SLIME, 0.12F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.BREEZE, 0.12F * Math.max(weights[4], 0.05F))
                        .entry(EntityPatterns.PHANTOM_T1, 0.06F * Math.max(weights[3], 0.05F))
                        .entry(EntityPatterns.CREEPER_T1_BASIC, 0.8F * Math.max(weights[3], 0.1F)), 1F);
    }

    @Nullable
    public static Wave generateMainInvasionWave(int waveNumber) {
        if (waveNumber < Waves.WAVES.size()) {
            return Waves.WAVES.get(waveNumber).build();
        }
        return generateExtendedWave(waveNumber).build();
    }

    private static Wave.Builder generateExtendedWave(int waveNumber) {
        float mobScale = (float) Math.pow(1.090000033378601D, waveNumber - 11);
        float timeScale = 1 + (waveNumber - 11) * 0.04F;

        var builder = Wave.builder((int) (timeScale * 120000), (int) (timeScale * 35000));
        EntityPattern tierThreePigman = waveNumber >= 15
                ? EntityPatterns.ZOMBIE_PIGMAN_T3_WITH_ZOGLIN
                : EntityPatterns.ZOMBIE_PIGMAN_T3_ANY;
        EntityPattern imp = waveNumber >= 13
                ? EntityPatterns.IMP_T1_WITH_BLAZE
                : EntityPatterns.IMP_T1;
        EntityPattern ghast = waveNumber >= 15 ? EntityPatterns.GHAST : null;

        // Hilfsfunktion: Nur ins Log schreiben (kein Chat)
        java.util.function.Consumer<String> announce = msg ->
                InvasionMod.LOGGER.debug("[Wave] " + msg + " (Welle " + waveNumber + ")");

        // ENTRY 1: Früher Teil der Wave mit Standard-Mobs
        announce.accept("Phase 1 gestartet: Standardmobs greifen an!");
        {
            var entry = WaveEntry.random()
                    .entry(EntityPatterns.ZOMBIE_T1_ANY, 200F)
                    .entry(EntityPatterns.ZOMBIE_T2_ANY_BASIC, 20F)
                    .entry(EntityPatterns.ZOMBIE_T3_ANY, 5F)
                    .entry(EntityPatterns.ZOMBIE_PIGMAN_T1_ANY, 50F)
                    .entry(EntityPatterns.ZOMBIE_PIGMAN_T2_ANY, 10F)
                    .entry(tierThreePigman, 3F)
                    .entry(EntityPatterns.SKELETON_T1_ANY, 30F)
                    .entry(EntityPatterns.THROWER_T1, 5F)
                    .entry(EntityPatterns.THROWER_T2, 0.5F)
                    .entry(EntityPatterns.BURROWER, 0.15F)
                    .entry(EntityPatterns.CREEPER_T1_BASIC, 0.8F)
                    .entry(imp, 2F)
                    .entry(EntityPatterns.ENDERMAN_T1, 0.2F)
                    .entry(EntityPatterns.WITCH, 0.2F)
                    .entry(ghast == null ? EntityPatterns.WITCH : ghast,
                            ghast == null ? 0.0F : 0.2F)
                    .entry(EntityPatterns.SLIME, 0.2F)
                    .entry(EntityPatterns.BREEZE, 0.2F)
                    .entry(EntityPatterns.PHANTOM_T1, 0.12F);

            entry.end((int) (timeScale * 30000))
                    .amount((int) (mobScale * 8))
                    .granularity(2000)
                    .angle(45)
                    .minSpawns(5);

            builder.entry(entry);
        }

        // ENTRY 2: Mittelteil – Spider, Engineer etc.
        announce.accept("Phase 2 gestartet: Spezialisten greifen an!");
        var midPool = WaveEntry.random()
                .entry(EntityPatterns.SPIDER_T2_ANY, 2F)
                .entry(EntityPatterns.PIGMAN_ENGINEER_T1_ANY, 1F)
                .entry(EntityPatterns.BURROWER, 0.2F)
                .entry(EntityPatterns.ENDERMAN_T1, 0.08F)
                .entry(EntityPatterns.WITCH, 0.08F)
                .entry(ghast == null ? EntityPatterns.WITCH : ghast,
                        ghast == null ? 0.0F : 0.08F)
                .entry(EntityPatterns.SLIME, 0.08F)
                .entry(EntityPatterns.BREEZE, 0.08F)
                .entry(EntityPatterns.PHANTOM_T1, 0.05F);

        builder.entry(
                midPool
                        .end((int) (timeScale * 90000))
                        .amount((int) (mobScale * 3))
                        .granularity(500)
        );

        // ENTRY 3: kurzer Burst (~65–67s)
        announce.accept("Phase 3 gestartet: Starker Angriff!");
        builder.entry(
                WaveEntry.random()
                        .entry(EntityPatterns.ZOMBIE_PIGMAN_T1_ANY, 1.5F)
                        .entry(EntityPatterns.ZOMBIE_PIGMAN_T2_ANY, 0.7F)
                        .entry(tierThreePigman, 0.35F)
                        .entry(EntityPatterns.ZOMBIE_T2_ANY_BASIC, 1.5F)
                        .entry(EntityPatterns.SPIDER_T2_ANY, 1F)
                        .entry(EntityPatterns.ZOMBIE_T1_ANY, 1F)
                        .entry(EntityPatterns.SKELETON_T1_ANY, 1F)
                        .entry(EntityPatterns.THROWER_T1, 0.5F)
                        .entry(EntityPatterns.THROWER_T2, 0.42F)
                        .entry(EntityPatterns.BURROWER, 0.12F)
                        .entry(EntityPatterns.ZOMBIE_T3_ANY, 0.5F)
                        .entry(EntityPatterns.CREEPER_T1_BASIC, 0.42F)
                        .entry(imp, 0.4F)
                        .entry(EntityPatterns.PHANTOM_T1, 0.08F)
                        .begin((int) (timeScale * 65000))
                        .end((int) (timeScale * 67000))
                        .amount((int) (mobScale * 7))
                        .granularity(500)
                        .angle(25)
                        .minSpawns(3)
        );

        // ENTRY 4: kurzer Burst (~95–97s)
        announce.accept("Phase 4 gestartet: Finale Angriffswelle!");
        builder.entry(
                WaveEntry.random()
                        .entry(EntityPatterns.ZOMBIE_T2_ANY_BASIC, 2F)
                        .entry(EntityPatterns.ZOMBIE_T1_ANY, 3F)
                        .entry(EntityPatterns.SPIDER_T3_ANY, 1F)
                        .begin((int) (timeScale * 95000))
                        .end((int) (timeScale * 97000))
                        .amount((int) (mobScale * 6))
                        .granularity(500)
                        .angle(45)
                        .minSpawns(2)
        );

        if (waveNumber >= 20 && waveNumber % 5 == 0) {
            int healthScalingSteps = (waveNumber - 20) / 5;
            EntityPattern wither = new EntityPattern.Builder(InvEntities.WITHER)
                    .addTier(healthScalingSteps, 1)
                    .build();
            builder.entry(WaveEntry.finite()
                    .entry(wither, 1)
                    .begin((int) (timeScale * 105000))
                    .end((int) (timeScale * 106000))
                    .amount(1)
                    .granularity(500)
                    .minSpawns(1));
        }

        return builder;
    }
}
