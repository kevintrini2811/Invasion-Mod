package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.AbstractVillager;
import com.invasion.nexus.Combatant;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class VillagerResurrectionHandler {
    private VillagerResurrectionHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(VillagerResurrectionHandler::afterDeath);
    }

    private static void afterDeath(LivingDeathEvent event) {
        Entity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (!(victim instanceof AbstractVillager villager)
                || !(villager.level() instanceof ServerLevel world)
                || !(source.getEntity() instanceof Combatant<?> killer)
                || !killer.hasNexus()) {
            return;
        }

        IMZombieVillagerEntity zombie = InvEntities.ZOMBIE_VILLAGER.create(world);
        if (zombie == null) {
            return;
        }

        zombie.moveTo(
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
