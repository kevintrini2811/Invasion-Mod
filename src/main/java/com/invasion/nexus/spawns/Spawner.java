package com.invasion.nexus.spawns;

import com.invasion.nexus.EntityConstruct;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.MinMaxBounds.Ints;
import net.minecraft.util.RandomSource;

public interface Spawner {
    RandomSource getRandom();

    boolean attemptSpawn(EntityConstruct mobConstruct, Ints angle);

    int getNumberOfPointsInRange(Ints angle, SpawnType type);

    void sendSpawnAlert(String message, ChatFormatting color);

    void noSpawnPointNotice();
}