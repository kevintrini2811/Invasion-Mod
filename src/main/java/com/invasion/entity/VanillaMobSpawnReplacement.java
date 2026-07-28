package com.invasion.entity;

import com.invasion.nexus.WorldNexusStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

public final class VanillaMobSpawnReplacement {
    private VanillaMobSpawnReplacement() {
    }

    public static void bootstrap() {
        ServerEntityEvents.ENTITY_LOAD.register(VanillaMobSpawnReplacement::replaceVanillaMob);
    }

    private static void replaceVanillaMob(
            net.minecraft.world.entity.Entity entity, ServerLevel world) {
        if (!(entity instanceof Mob mob)
                || !isReplaceableType(mob.getType())
                || WorldNexusStorage.of(world).getNexus()
                        .filter(nexus -> nexus.isActive())
                        .isEmpty()) {
            return;
        }

        world.getServer().execute(() -> {
            if (mob.isRemoved() || !mob.isAlive()) {
                return;
            }

            WorldNexusStorage.of(world).getNexus()
                    .filter(nexus -> nexus.isActive())
                    .ifPresent(nexus -> {
                        if (mob.getType() == EntityTypes.ZOMBIE) {
                            convert(mob, InvEntities.ZOMBIE, nexus);
                        } else if (mob.getType() == EntityTypes.SKELETON) {
                            convert(mob, InvEntities.SKELETON, nexus);
                        } else if (mob.getType() == EntityTypes.CREEPER) {
                            convert(mob, InvEntities.CREEPER, nexus);
                        } else if (mob.getType() == EntityTypes.SPIDER) {
                            convert(mob, InvEntities.SPIDER, nexus);
                        }
                    });
        });
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
