package com.invasion.entity;

import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

/** Releases IM Silverfish when a permanently infected host dies. */
public final class InfectionDeathHandler {
    private InfectionDeathHandler() {
    }

    public static void bootstrap() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
                InfectionDeathHandler::onLivingDeath);
    }

    private static void onLivingDeath(
            LivingEntity host, DamageSource damageSource) {
        if (!(host.level() instanceof ServerLevel level)
                || !host.entityTags().contains(IMSilverfishEntity.INFECTED_TAG)) {
            return;
        }

        // Consume the marker immediately so another death callback cannot
        // release a second group from the same host.
        host.removeTag(IMSilverfishEntity.INFECTED_TAG);
        NexusAccess nexus = host instanceof IHasNexus nexusHost
                ? nexusHost.getNexus()
                : WorldNexusStorage.of(level).getNexus()
                        .filter(NexusAccess::isActive).orElse(null);
        int amount = 1 + level.getRandom().nextInt(4);
        for (int i = 0; i < amount; i++) {
            IMSilverfishEntity silverfish = InvEntities.SILVERFISH.create(
                    level, EntitySpawnReason.TRIGGERED);
            if (silverfish == null) {
                continue;
            }
            silverfish.snapTo(
                    host.getX() + (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                    host.getY() + 0.1D,
                    host.getZ() + (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            silverfish.setNexus(nexus);
            level.addFreshEntity(silverfish);
        }
    }
}
