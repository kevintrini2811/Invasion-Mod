package com.invasion.nexus.spawns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.advancements.predicates.MinMaxBounds.Ints;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.wave.EntityPattern;
import com.invasion.util.math.PolarAngle;

public class SpawnPointContainer {
    private final Map<SpawnType, List<SpawnPoint>> spawnPoints = new EnumMap<>(SpawnType.class);
    private final Map<SpawnType, Map<Column, Integer>> spawnPointColumns = new EnumMap<>(SpawnType.class);
    private boolean sorted;
    private Random random = new Random();

    public void addSpawnPointXZ(SpawnPoint spawnPoint) {
        List<SpawnPoint> spawnList = spawnPoints.computeIfAbsent(spawnPoint.type(), i -> new ArrayList<>());
        Map<Column, Integer> columns = spawnPointColumns.computeIfAbsent(
                spawnPoint.type(), i -> new HashMap<>());
        Column column = new Column(spawnPoint.pos().getX(), spawnPoint.pos().getZ());
        Integer oldIndex = columns.get(column);
        if (oldIndex == null) {
            columns.put(column, spawnList.size());
            spawnList.add(spawnPoint);
        } else if (spawnList.get(oldIndex).pos().getY() > spawnPoint.pos().getY()) {
            spawnList.set(oldIndex, spawnPoint);
        }
        this.sorted = false;
    }

    @Nullable
    public SpawnPoint getRandomSpawnPoint(SpawnType spawnType) {
        List<SpawnPoint> spawnList = spawnPoints.getOrDefault(spawnType, List.of());
        return spawnList.isEmpty() ? null : spawnList.get(random.nextInt(spawnList.size()));
    }

    public SpawnPoint getRandomSpawnPoint(SpawnType spawnType, Ints angle) {
        int minAngle = angle.min().orElse(-EntityPattern.MAX_ANGLE);
        int maxAngle = angle.max().orElse(EntityPattern.MAX_ANGLE);
        List<SpawnPoint> spawnList = spawnPoints.get(spawnType);
        if (spawnList.isEmpty()) {
            return null;
        }

        ensureSorted(spawnType, spawnList);

        int start = Collections.binarySearch(spawnList, PolarAngle.of(minAngle));
        if (start < 0) {
            start = -start - 1;
        }
        int end = Collections.binarySearch(spawnList, PolarAngle.of(maxAngle));
        if (end < 0) {
            end = -end - 1;
        }
        if (end > start) {
            return spawnList.get(start + this.random.nextInt(end - start));
        }
        if ((start > end) && (end > 0)) {
            int r = start + this.random.nextInt(spawnList.size() + end - start);
            if (r >= spawnList.size()) {
                r -= spawnList.size();
            }
            return spawnList.get(r);
        }
        return null;
    }

    public List<SpawnPoint> getRandomSpawnPoints(
            SpawnType spawnType, Ints angle, int limit) {
        List<SpawnPoint> spawnList = spawnPoints.getOrDefault(spawnType, List.of());
        if (spawnList.isEmpty() || limit <= 0) {
            return List.of();
        }

        int minAngle = angle.min().orElse(-EntityPattern.MAX_ANGLE);
        int maxAngle = angle.max().orElse(EntityPattern.MAX_ANGLE);
        int start = 0;
        int end = spawnList.size();
        boolean wraps = false;
        if (maxAngle - minAngle < 360) {
            ensureSorted(spawnType, spawnList);
            start = insertionPoint(spawnList, minAngle);
            end = insertionPoint(spawnList, maxAngle);
            wraps = start > end;
        }

        int candidateCount = wraps ? spawnList.size() - start + end : end - start;
        int sampleSize = Math.min(limit, candidateCount);
        if (sampleSize == 0) {
            return List.of();
        }

        HashSet<Integer> offsets = new HashSet<>(sampleSize);
        for (int j = candidateCount - sampleSize; j < candidateCount; j++) {
            int offset = random.nextInt(j + 1);
            if (!offsets.add(offset)) {
                offsets.add(j);
            }
        }
        List<SpawnPoint> candidates = new ArrayList<>(sampleSize);
        for (int offset : offsets) {
            int index = start + offset;
            if (index >= spawnList.size()) {
                index -= spawnList.size();
            }
            candidates.add(spawnList.get(index));
        }
        Collections.shuffle(candidates, random);
        return candidates;
    }

    private void ensureSorted(SpawnType spawnType, List<SpawnPoint> spawnList) {
        if (!sorted) {
            Collections.sort(spawnList);
            Map<Column, Integer> columns = spawnPointColumns.get(spawnType);
            columns.clear();
            for (int i = 0; i < spawnList.size(); i++) {
                SpawnPoint point = spawnList.get(i);
                columns.put(new Column(point.pos().getX(), point.pos().getZ()), i);
            }
            sorted = true;
        }
    }

    private static int insertionPoint(List<SpawnPoint> spawnList, int angle) {
        int index = Collections.binarySearch(spawnList, PolarAngle.of(angle));
        return index < 0 ? -index - 1 : index;
    }

    private record Column(int x, int z) {
    }

    public int getNumberOfSpawnPoints(SpawnType type) {
        return spawnPoints.getOrDefault(SpawnType.HUMANOID, List.of()).size();
    }

    public int getNumberOfSpawnPoints(SpawnType spawnType, Ints angle) {
        int minAngle = angle.min().orElse(-EntityPattern.MAX_ANGLE);
        int maxAngle = angle.max().orElse(EntityPattern.MAX_ANGLE);
        List<SpawnPoint> spawnList = spawnPoints.get(spawnType);
        if (spawnList.isEmpty() || (maxAngle - minAngle) >= 360) {
            return spawnList.size();
        }

        ensureSorted(spawnType, spawnList);

        int start = Collections.binarySearch(spawnList, PolarAngle.of(minAngle));
        if (start < 0) {
            start = -start - 1;
        }
        int end = Collections.binarySearch(spawnList, PolarAngle.of(maxAngle));
        if (end < 0) {
            end = -end - 1;
        }
        if (end > start) {
            return end - start;
        }
        if ((start > end) && (end > 0)) {
            return end + spawnList.size() - start;
        }
        return 0;
    }

    public void pointDisplayTest(Block block, Level world) {
        for (SpawnPoint point : spawnPoints.get(SpawnType.HUMANOID)) {
            world.setBlockAndUpdate(point.pos(), block.defaultBlockState());
        }
    }
}
