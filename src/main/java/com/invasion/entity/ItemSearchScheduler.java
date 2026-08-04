package com.invasion.entity;

import net.minecraft.world.entity.Entity;

final class ItemSearchScheduler {
    private static final int SEARCH_INTERVAL = 15;

    private ItemSearchScheduler() {
    }

    static boolean shouldSearch(Entity entity) {
        return Math.floorMod(entity.tickCount + entity.getId(), SEARCH_INTERVAL) == 0;
    }
}
