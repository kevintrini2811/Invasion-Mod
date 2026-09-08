package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import com.invasion.compat.FriendsAndFoesCompatibility;
import com.invasion.compat.ConfiguredModMobs;
import com.invasion.nexus.NexusAccess;
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
        Entity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (!(entity instanceof Mob victim)
                || victim instanceof Combatant<?>
                || ConfiguredModMobs.isInvasionAlly(victim)
                || !(victim instanceof AbstractVillager
                        || victim instanceof AbstractPiglin
                        || victim instanceof Pig
                        || victim instanceof Hoglin
                        || victim instanceof Pillager
                        || victim instanceof Vindicator
                        || victim instanceof Evoker
                        || FriendsAndFoesCompatibility.isTargetableIllager(
                                victim.getType()))
                || !(victim.level() instanceof ServerLevel world)) {
            return;
        }

        NexusAccess nexus;
        if (source.getEntity() instanceof Combatant<?> killer) {
            nexus = killer.getNexus();
        } else if (source.getEntity() instanceof Mob killer
                && ConfiguredModMobs.isInvasionAlly(killer)) {
            nexus = ConfiguredModMobs.activeNexus(killer);
            if (nexus == null) return;
        } else {
            return;
        }

        Mob zombie = victim instanceof AbstractVillager
                        || victim instanceof Pillager
                        || victim instanceof Vindicator
                        || victim instanceof Evoker
                        || FriendsAndFoesCompatibility.isTargetableIllager(
                                victim.getType())
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
                || victim instanceof Evoker
                || FriendsAndFoesCompatibility.isTargetableIllager(
                        victim.getType())) {
            transferEquipment(victim, zombie);
        }
        if (zombie instanceof Combatant<?> combatant) {
            combatant.setNexus(nexus);
            combatant.resetHealth();
        }
        world.addFreshEntity(zombie);
    }

    private static void transferEquipment(Mob victim, Mob zombie) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            zombie.setItemSlot(slot, victim.getItemBySlot(slot).copy());
            victim.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
}
