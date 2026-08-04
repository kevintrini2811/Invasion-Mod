package com.invasion.entity;

import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Releases IM support parasites when a permanently infected host dies. */
public final class InfectionDeathHandler {
    private InfectionDeathHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(InfectionDeathHandler::onLivingDeath);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity host = event.getEntity();
        if (!(host.level() instanceof ServerLevel level)
                || !host.getTags().contains(IMSilverfishEntity.INFECTED_TAG)) {
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
            LivingEntity parasite = host instanceof EnderMan
                    ? InvEntities.ENDERMITE.create(level)
                    : InvEntities.SILVERFISH.create(level);
            if (parasite == null) {
                continue;
            }
            parasite.moveTo(
                    host.getX() + (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                    host.getY() + 0.1D,
                    host.getZ() + (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            if (parasite instanceof IHasNexus nexusMob) {
                nexusMob.setNexus(nexus);
            }
            level.addFreshEntity(parasite);
        }
    }
}
