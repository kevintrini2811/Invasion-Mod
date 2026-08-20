package com.invasion.entity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import com.invasion.compat.FriendsAndFoesCompatibility;
import com.invasion.nexus.Combatant;

public final class VillagerResurrectionHandler {
    private VillagerResurrectionHandler() {
    }

    public static void bootstrap() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
                VillagerResurrectionHandler::afterDeath);
    }

    private static void afterDeath(Entity entity, DamageSource source) {
        if (!(entity instanceof Mob victim)
                || !(victim instanceof AbstractVillager
                        || victim instanceof AbstractPiglin
                        || victim instanceof Pig
                        || victim instanceof Hoglin
                        || victim instanceof Pillager
                        || victim instanceof Vindicator
                        || victim instanceof Evoker
                        || FriendsAndFoesCompatibility.isTargetableIllager(
                                victim.getType()))
                || !(victim.level() instanceof ServerLevel world)
                || !(source.getEntity() instanceof Combatant<?> killer)) {
            return;
        }

        Mob zombie = victim instanceof AbstractVillager
                        || victim instanceof Pillager
                        || victim instanceof Vindicator
                        || victim instanceof Evoker
                        || FriendsAndFoesCompatibility.isTargetableIllager(
                                victim.getType())
                ? InvEntities.ZOMBIE_VILLAGER.create(
                        world, EntitySpawnReason.CONVERSION)
                : victim instanceof AbstractPiglin
                        ? InvEntities.ZOMBIFIED_PIGLIN.create(
                                world, EntitySpawnReason.CONVERSION)
                        : victim instanceof Hoglin
                                ? InvEntities.ZOGLIN.create(
                                        world, EntitySpawnReason.CONVERSION)
                                : InvEntities.ZOMBIE_PIGMAN.create(
                                        world, EntitySpawnReason.CONVERSION);
        if (zombie == null) {
            return;
        }

        zombie.snapTo(
                victim.getX(), victim.getY(), victim.getZ(),
                victim.getYRot(), victim.getXRot());
        zombie.setDeltaMovement(victim.getDeltaMovement());
        zombie.setBaby(victim.isBaby());
        zombie.setCustomName(victim.getCustomName());
        zombie.setCustomNameVisible(victim.isCustomNameVisible());
        zombie.setNoAi(victim.isNoAi());
        if (victim.isPersistenceRequired()) {
            zombie.setPersistenceRequired();
        }
        if (victim instanceof Pillager
                || victim instanceof Vindicator
                || victim instanceof Evoker
                || FriendsAndFoesCompatibility.isTargetableIllager(
                        victim.getType())) {
            transferEquipment(victim, zombie);
        }
        if (zombie instanceof Combatant<?> combatant) {
            combatant.setNexus(killer.getNexus());
            combatant.resetHealth();
        }
        world.addFreshEntity(zombie);
    }

    private static void transferEquipment(Mob victim, Mob zombie) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            zombie.setItemSlot(slot, victim.getItemBySlot(slot).copy());
            zombie.setDropChance(
                    slot, victim.getDropChances().byEquipment(slot));
            victim.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
}
