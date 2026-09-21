package com.invasion.nexus.wave;

import net.minecraft.predicate.NumberRange.IntRange;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.spawns.Spawner;
import com.invasion.nexus.wave.pool.Select;
import com.invasion.nexus.spawns.SpawnType;

public class WaveEntry {
    static final int DEFAULT_NEXT_ALERT_TIME = Integer.MAX_VALUE;
    static final int MAX_ANGLE = 360;
    static final int MAX_VALID_ANGLE = 180;
    static final IntRange FULL_RANGE = IntRange.between(-MAX_VALID_ANGLE, MAX_VALID_ANGLE);

    static int wrapAngle(int angle) {
        while (angle > MAX_VALID_ANGLE) {
            angle -= MAX_ANGLE;
        }
        return angle;
    }

    static int clampAngle(int angle) {
        while (angle < 0) {
            angle += MAX_ANGLE;
        }
        return angle == 0 ? MAX_ANGLE : angle;
    }

    private final IntRange time;
    private IntRange angle;

    private final int amount;
    private final int granularity;

    private int amountQueued;
    private int elapsed;
    private int toNextSpawn;

    private int minPointsInRange;
    private int nextAlert = DEFAULT_NEXT_ALERT_TIME;

    private final Select<EntityPattern> mobPool;
    private final Map<Integer, String> alerts;

    private final List<EntityConstruct> spawnList = new ArrayList<>();

    private WaveEntry(IntRange time, IntRange angle, int amount, int granularity, Select<EntityPattern> mobPool, Map<Integer, String> alerts, int minPointsInRange) {
        this.time = time;
        this.angle = angle;
        this.amount = amount;
        this.granularity = granularity;
        this.mobPool = mobPool;
        this.alerts = alerts;
        this.minPointsInRange = minPointsInRange;
    }

    public int doNextSpawns(int elapsedMillis, Spawner spawner) {
        toNextSpawn -= elapsedMillis;
        if (nextAlert <= elapsed - toNextSpawn) {
            sendNextAlert(spawner);
        }

        if (toNextSpawn <= 0) {
            elapsed += granularity;
            toNextSpawn += granularity;
            if (toNextSpawn < 0) {
                elapsed -= toNextSpawn;
                toNextSpawn = 0;
            }

            int amountToSpawn = Math.round(amount * elapsed / (time.max().orElse(0) - time.min().orElse(0))) - amountQueued;
            if (amountToSpawn > 0) {
                if (amountToSpawn + amountQueued > amount) {
                    amountToSpawn = amount - amountQueued;
                }
                while (amountToSpawn > 0) {
                    EntityPattern pattern = mobPool.selectNext(spawner.getRandom());
                    if (pattern != null) {
                        EntityConstruct mobConstruct = pattern.generateEntityConstruct(spawner.getRandom(), angle);
                        if (mobConstruct != null) {
                            amountToSpawn--;
                            this.amountQueued += 1;
                            this.spawnList.add(mobConstruct);
                        }
                    } else {
                        InvasionMod.log("A selection pool in wave entry " + toString() + " returned empty. Pool: " + mobPool.toString());
                    }
                }
            }
        }

        if (!spawnList.isEmpty()) {
            int numberOfSpawns = 0;
            if (spawner.getNumberOfPointsInRange(angle, SpawnType.HUMANOID) >= minPointsInRange) {
                for (int i = spawnList.size() - 1; i >= 0; i--) {
                    if (spawner.attemptSpawn(spawnList.get(i), angle)) {
                        numberOfSpawns++;
                        spawnList.remove(i);
                    }
                }
            } else {
                reviseSpawnAngles(spawner);
            }
            return numberOfSpawns;
        }
        return 0;
    }

    public void resetToBeginning() {
        elapsed = 0;
        amountQueued = 0;
        mobPool.reset();
    }

    public void setToTime(int millis) {
        elapsed = millis;
    }

    public IntRange getTime() {
        return time;
    }

    public int getAmount() {
        return amount;
    }

    private void sendNextAlert(Spawner spawner) {
        @Nullable
        String message = alerts.remove(nextAlert);
        if (message != null) {
            spawner.sendSpawnAlert(message, Formatting.RED);
        }
        nextAlert = DEFAULT_NEXT_ALERT_TIME;
        if (alerts.size() > 0) {
            for (Integer key : alerts.keySet()) {
                if (key.intValue() < nextAlert) {
                    nextAlert = key.intValue();
                }
            }
        }
    }

    private void reviseSpawnAngles(Spawner spawner) {
        int angleRange = clampAngle(angle.max().get() - angle.min().orElse(0));
        List<Integer> validAngles = getAllowedAngles(spawner, angleRange);
        if (!validAngles.isEmpty()) {
            int min = Util.getRandom(validAngles, spawner.getRandom());
            int max = wrapAngle(min + angleRange);
            angle = IntRange.between(min, max);
        }

        if (minPointsInRange > 1) {
            InvasionMod.LOGGER.warn("Can't find a direction with enough spawn points: " + minPointsInRange + ". Lowering requirement.");
            this.minPointsInRange = 1;
        } else if (angle.max().get() - angle.min().orElse(0) < MAX_ANGLE) {
            InvasionMod.LOGGER.warn("Can't find a direction with enough spawn points: " + minPointsInRange + ". Switching to 360 degree mode for this entry");
            angle = FULL_RANGE;
        } else {
            InvasionMod.log("Wave entry cannot find a single spawn point");
            spawner.noSpawnPointNotice();
        }
    }

    private List<Integer> getAllowedAngles(Spawner spawner, int angleRange) {
        List<Integer> validAngles = new ArrayList<>();
        for (int angle = -MAX_VALID_ANGLE; angle < MAX_VALID_ANGLE; angle += angleRange) {
            int nextAngle = wrapAngle(angle + angleRange);
            if (spawner.getNumberOfPointsInRange(IntRange.between(angle, nextAngle), SpawnType.HUMANOID) >= minPointsInRange) {
                validAngles.add(angle);
            }
        }
        return validAngles;
    }

    @Override
    public String toString() {
        return "WaveEntry@" + Integer.toHexString(hashCode()) + "#time=" + time + "#amount=" + amount;
    }

    public static Builder<Integer> finite() {
        return new Builder<>(Select.<EntityPattern>finite());
    }

    public static Builder<Float> random() {
        return new Builder<>(Select.<EntityPattern>random());
    }

    public static <K> Builder<K> builder(Select.PoolBuilder<EntityPattern, K> pool) {
        return new Builder<>(pool);
    }

    public static final class Builder<K> {
        private int timeBegin;
        private int timeEnd;
        private int amount;
        private int granularity;

        private int minAngle = -MAX_VALID_ANGLE;
        private int maxAngle = MAX_VALID_ANGLE;
        private int minPointsInRange = 1;
        private final Select.PoolBuilder<EntityPattern, K> mobPool;
        private final Map<Integer, String> alerts = new HashMap<>();

        private Builder(Select.PoolBuilder<EntityPattern, K> pool) {
            this.mobPool = pool;
        }

        public Builder<K> begin(int begin) {
            timeBegin = begin;
            return this;
        }

        public Builder<K> end(int end) {
            timeEnd = end;
            return this;
        }

        public Builder<K> amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder<K> granularity(int granularity) {
            this.granularity = granularity;
            return this;
        }

        public Builder<K> angle(int range) {
            this.minAngle = Random.create().nextInt(MAX_ANGLE) - MAX_VALID_ANGLE;
            this.maxAngle = wrapAngle(minAngle + range);
            return this;
        }

        public Builder<K> minSpawns(int points) {
            minPointsInRange = points;
            return this;
        }

        public Builder<K> entry(EntityPattern entry, K amount) {
            return entry(() -> Select.unary(entry), amount);
        }

        public Builder<K> entry(Select.Builder<EntityPattern> entry, K amount) {
            mobPool.entry(entry, amount);
            return this;
        }

        public Builder<K> alert(String translation, int time) {
            alerts.put(time, translation);
            return this;
        }

        public WaveEntry build() {
            return new WaveEntry(
                    IntRange.between(timeBegin, timeEnd),
                    IntRange.between(minAngle, maxAngle),
                    amount, granularity,
                    mobPool.build(),
                    new HashMap<>(alerts), minPointsInRange);
        }
    }
}