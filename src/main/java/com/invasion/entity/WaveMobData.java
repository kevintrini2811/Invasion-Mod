package com.invasion.entity;

import net.minecraft.world.entity.Entity;

public final class WaveMobData {
    private WaveMobData() {}

    public static void set(Entity entity, String key, int value) {
        String prefix = key + ":";
        entity.entityTags().stream().filter(tag -> tag.startsWith(prefix))
                .toList().forEach(entity::removeTag);
        entity.addTag(prefix + value);
    }

    public static int get(Entity entity, String key, int fallback) {
        String prefix = key + ":";
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(prefix)) {
                try { return Integer.parseInt(tag.substring(prefix.length())); }
                catch (NumberFormatException ignored) { return fallback; }
            }
        }
        return fallback;
    }
}
