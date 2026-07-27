package com.invasion.nexus.test;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Debug path payloads were removed from vanilla in 26.2. Keeping this hook as a
 * no-op avoids changing the pathfinding code and can be wired to a custom debug
 * payload later.
 */
public final class PathingDebugger {
    private PathingDebugger() {
    }

    public static void sendPathToClients(Entity sender, @Nullable Path path, float scale) {
    }
}
