package com.invasion.entity;

import com.invasion.nexus.WorldNexusStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class ZombieSpawnReplacement {
    private ZombieSpawnReplacement() {
    }

    public static void bootstrap() {
        ServerEntityEvents.ENTITY_LOAD.register(ZombieSpawnReplacement::replaceVanillaZombie);
    }

    private static void replaceVanillaZombie(
            net.minecraft.world.entity.Entity entity, ServerLevel world) {
        if (!(entity instanceof Zombie zombie)
                || zombie.getType() != EntityTypes.ZOMBIE
                || WorldNexusStorage.of(world).getNexus()
                        .filter(nexus -> nexus.isActive())
                        .isEmpty()) {
            return;
        }

        world.getServer().execute(() -> {
            if (zombie.isRemoved() || !zombie.isAlive()) {
                return;
            }

            WorldNexusStorage.of(world).getNexus()
                    .filter(nexus -> nexus.isActive())
                    .ifPresent(nexus -> zombie.convertTo(
                            InvEntities.ZOMBIE,
                            ConversionParams.single(zombie, true, true),
                            EntitySpawnReason.CONVERSION,
                            converted -> converted.setNexus(nexus)));
        });
    }
}
