package com.invasion.entity;

import net.minecraft.world.entity.Entity;

public final class ItemSearchScheduler {
    private static final int SEARCH_INTERVAL = 15;

    private ItemSearchScheduler() {
    }

    public static boolean shouldSearch(Entity entity) {
        return Math.floorMod(entity.tickCount + entity.getId(), SEARCH_INTERVAL) == 0;
    }
}
