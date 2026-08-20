package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import com.invasion.nexus.Combatant;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

public final class VillagerResurrectionHandler {
    private VillagerResurrectionHandler() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(VillagerResurrectionHandler::afterDeath);
    }

    private static void afterDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (!(entity instanceof Mob victim)
                || !(victim instanceof AbstractVillager
                        || victim instanceof AbstractPiglin
                        || victim instanceof Pig
                        || victim instanceof Hoglin
                        || victim instanceof Pillager
                        || victim instanceof Vindicator
                        || victim instanceof Evoker)
                || !(victim.level() instanceof ServerLevel world)
                || !(source.getEntity() instanceof Combatant<?> killer)) {
            return;
        }

        Mob zombie = victim instanceof AbstractVillager
                        || victim instanceof Pillager
                        || victim instanceof Vindicator
                        || victim instanceof Evoker
                ? InvEntities.ZOMBIE_VILLAGER.create(world)
                : victim instanceof AbstractPiglin
                        ? InvEntities.ZOMBIFIED_PIGLIN.create(world)
                        : victim instanceof Hoglin
                                ? InvEntities.ZOGLIN.create(world)
                                : InvEntities.ZOMBIE_PIGMAN.create(world);
        if (zombie == null) {
            return;
        }

        zombie.moveTo(
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
                || victim instanceof Evoker) {
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
                    slot, victim.getEquipmentDropChance(slot));
            victim.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
}
