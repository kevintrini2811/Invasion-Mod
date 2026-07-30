package com.invasion.entity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;

public final class VillagerResurrectionHandler {
    private VillagerResurrectionHandler() {
    }

    public static void bootstrap() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
                VillagerResurrectionHandler::afterDeath);
    }

    private static void afterDeath(Entity victim, DamageSource source) {
        if (!(victim instanceof Villager villager)
                || !(villager.level() instanceof ServerLevel world)
                || !(source.getEntity() instanceof NexusEntity killer)) {
            return;
        }

        IMZombieVillagerEntity zombie = InvEntities.ZOMBIE_VILLAGER.create(
                world, EntitySpawnReason.CONVERSION);
        if (zombie == null) {
            return;
        }

        zombie.snapTo(
                villager.getX(), villager.getY(), villager.getZ(),
                villager.getYRot(), villager.getXRot());
        zombie.setDeltaMovement(villager.getDeltaMovement());
        zombie.setBaby(villager.isBaby());
        zombie.setCustomName(villager.getCustomName());
        zombie.setCustomNameVisible(villager.isCustomNameVisible());
        zombie.setNoAi(villager.isNoAi());
        if (villager.isPersistenceRequired()) {
            zombie.setPersistenceRequired();
        }
        zombie.setNexus(killer.getNexus());
        zombie.resetHealth();
        world.addFreshEntity(zombie);
    }
}
