package com.invasion.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.invasion.nexus.WorldNexusStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

public final class VanillaMobSpawnReplacement {
    private static final Map<ServerLevel, Set<UUID>> PENDING = new HashMap<>();
    private static boolean converting;

    private VanillaMobSpawnReplacement() {
    }

    public static void bootstrap() {
        ServerEntityEvents.ENTITY_LOAD.register(VanillaMobSpawnReplacement::queueVanillaMob);
        ServerTickEvents.END_LEVEL_TICK.register(VanillaMobSpawnReplacement::processQueue);
    }

    private static void queueVanillaMob(
            net.minecraft.world.entity.Entity entity, ServerLevel world) {
        if (converting
                || !(entity instanceof Mob mob)
                || !isReplaceableType(mob.getType())
                || WorldNexusStorage.of(world).getNexus()
                        .filter(nexus -> nexus.isActive())
                        .isEmpty()) {
            return;
        }

        PENDING.computeIfAbsent(world, ignored -> new HashSet<>()).add(mob.getUUID());
    }

    private static void processQueue(ServerLevel world) {
        Set<UUID> pending = PENDING.remove(world);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        converting = true;
        try {
            WorldNexusStorage.of(world).getNexus()
                    .filter(nexus -> nexus.isActive())
                    .ifPresent(nexus -> {
                        for (UUID id : pending) {
                            net.minecraft.world.entity.Entity entity = world.getEntity(id);
                            if (entity instanceof Mob mob
                                    && mob.isAlive()
                                    && !mob.isRemoved()) {
                                convertMob(mob, nexus);
                            }
                        }
                    });
        } finally {
            converting = false;
        }
    }

    private static void convertMob(
            Mob mob, com.invasion.nexus.NexusAccess nexus) {
        if (mob.getType() == EntityTypes.ZOMBIE) {
            convert(mob, InvEntities.ZOMBIE, nexus);
        } else if (mob.getType() == EntityTypes.SKELETON) {
            convert(mob, InvEntities.SKELETON, nexus);
        } else if (mob.getType() == EntityTypes.CREEPER) {
            convert(mob, InvEntities.CREEPER, nexus);
        } else if (mob.getType() == EntityTypes.SPIDER) {
            convert(mob, InvEntities.SPIDER, nexus);
        }
    }

    private static boolean isReplaceableType(EntityType<?> type) {
        return type == EntityTypes.ZOMBIE
                || type == EntityTypes.SKELETON
                || type == EntityTypes.CREEPER
                || type == EntityTypes.SPIDER;
    }

    private static <T extends Mob & NexusEntity> void convert(
            Mob source, EntityType<T> targetType,
            com.invasion.nexus.NexusAccess nexus) {
        source.convertTo(
                targetType,
                ConversionParams.single(source, true, true),
                EntitySpawnReason.CONVERSION,
                converted -> converted.setNexus(nexus));
    }
}
