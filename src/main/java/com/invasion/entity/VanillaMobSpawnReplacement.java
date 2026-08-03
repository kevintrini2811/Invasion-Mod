package com.invasion.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.WorldNexusStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;

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
        // These mobs may already be loaded when the Nexus is activated.
        if (world.getGameTime() % 20L == 0L
                && WorldNexusStorage.of(world).getNexus()
                        .filter(nexus -> nexus.isActive())
                        .isPresent()) {
            Set<UUID> pending = PENDING.computeIfAbsent(
                    world, ignored -> new HashSet<>());
            for (Entity entity : world.getAllEntities()) {
                if (entity.getType() == EntityTypes.WITHER
                        || entity.getType() == EntityTypes.BLAZE) {
                    pending.add(entity.getUUID());
                }
            }
        }
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
        } else if (mob.getType() == EntityTypes.HUSK) {
            convert(mob, InvEntities.HUSK, nexus);
        } else if (mob.getType() == EntityTypes.DROWNED) {
            convert(mob, InvEntities.DROWNED, nexus);
        } else if (mob.getType() == EntityTypes.ZOMBIFIED_PIGLIN) {
            convert(mob, InvEntities.ZOMBIFIED_PIGLIN, nexus);
        } else if (mob.getType() == EntityTypes.SKELETON) {
            convert(mob, InvEntities.SKELETON, nexus);
        } else if (mob.getType() == EntityTypes.BOGGED) {
            convert(mob, InvEntities.BOGGED, nexus);
        } else if (mob.getType() == EntityTypes.PARCHED) {
            convert(mob, InvEntities.PARCHED, nexus);
        } else if (mob.getType() == EntityTypes.STRAY) {
            convert(mob, InvEntities.STRAY, nexus);
        } else if (mob.getType() == EntityTypes.WITHER_SKELETON) {
            convert(mob, InvEntities.WITHER_SKELETON, nexus);
        } else if (mob.getType() == EntityTypes.CREEPER) {
            convert(mob, InvEntities.CREEPER, nexus);
        } else if (mob.getType() == EntityTypes.SPIDER) {
            convert(mob, InvEntities.SPIDER, nexus);
        } else if (mob.getType() == EntityTypes.CAVE_SPIDER) {
            convert(mob, InvEntities.CAVE_SPIDER, nexus);
        } else if (mob.getType() == EntityTypes.ENDERMAN) {
            convert(mob, InvEntities.ENDERMAN, nexus);
        } else if (mob.getType() == EntityTypes.PHANTOM) {
            convert(mob, InvEntities.PHANTOM, nexus);
        } else if (mob.getType() == EntityTypes.ZOGLIN) {
            convert(mob, InvEntities.ZOGLIN, nexus);
        } else if (mob.getType() == EntityTypes.WITHER) {
            convert(mob, InvEntities.WITHER, nexus);
        } else if (mob.getType() == EntityTypes.BLAZE) {
            convert(mob, InvEntities.BLAZE, nexus);
        }
    }

    private static boolean isReplaceableType(EntityType<?> type) {
        return type == EntityTypes.ZOMBIE
                || type == EntityTypes.HUSK
                || type == EntityTypes.DROWNED
                || type == EntityTypes.ZOMBIFIED_PIGLIN
                || type == EntityTypes.SKELETON
                || type == EntityTypes.BOGGED
                || type == EntityTypes.PARCHED
                || type == EntityTypes.STRAY
                || type == EntityTypes.WITHER_SKELETON
                || type == EntityTypes.CREEPER
                || type == EntityTypes.SPIDER
                || type == EntityTypes.CAVE_SPIDER
                || type == EntityTypes.ENDERMAN
                || type == EntityTypes.PHANTOM
                || type == EntityTypes.ZOGLIN
                || type == EntityTypes.WITHER
                || type == EntityTypes.BLAZE;
    }

    private static <T extends Mob & Combatant<?> & EntityConstruct.BuildableMob>
            void convert(
            Mob source, EntityType<T> targetType,
            com.invasion.nexus.NexusAccess nexus) {
        if (!(source.level() instanceof ServerLevel world)) {
            return;
        }

        T converted = targetType.create(world, EntitySpawnReason.CONVERSION);
        if (converted == null) {
            return;
        }

        Entity vehicle = source.getVehicle();
        source.stopRiding();

        converted.snapTo(
                source.getX(), source.getY(), source.getZ(),
                source.getYRot(), source.getXRot());
        converted.setDeltaMovement(source.getDeltaMovement());
        converted.setBaby(source.isBaby());
        converted.setCustomName(source.getCustomName());
        converted.setCustomNameVisible(source.isCustomNameVisible());
        converted.setNoAi(source.isNoAi());
        converted.setCanPickUpLoot(source.canPickUpLoot());
        if (source instanceof Drowned
                && converted instanceof IMDrownedEntity) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                converted.setItemSlot(
                        slot, source.getItemBySlot(slot).copy());
            }
        }
        if (source instanceof Husk
                && converted instanceof IMHuskEntity) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                converted.setItemSlot(
                        slot, source.getItemBySlot(slot).copy());
            }
        }
        if (source instanceof ZombifiedPiglin piglin
                && converted instanceof IMZombifiedPiglinEntity imPiglin) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot != EquipmentSlot.HEAD) {
                    converted.setItemSlot(
                            slot, source.getItemBySlot(slot).copy());
                }
            }
            imPiglin.setPersistentAngerEndTime(
                    piglin.getPersistentAngerEndTime());
            imPiglin.setPersistentAngerTarget(
                    piglin.getPersistentAngerTarget());
        }
        if (source instanceof Bogged bogged
                && converted instanceof IMBoggedEntity imBogged) {
            imBogged.setSheared(bogged.isSheared());
        }
        if (source instanceof Phantom phantom
                && converted instanceof IMPhantomEntity imPhantom) {
            imPhantom.setPhantomSize(phantom.getPhantomSize());
        }
        if (converted instanceof AbstractIMZombieEntity) {
            converted.setCanPickUpLoot(true);
        }
        if (source.isPersistenceRequired()) {
            converted.setPersistenceRequired();
        }
        converted.setNexus(nexus);
        double reducedMaxHealth = converted instanceof IMWitherEntity
                ? 150.0D
                : converted.getMaxHealth() * 0.3D;
        converted.getAttribute(Attributes.MAX_HEALTH).setBaseValue(reducedMaxHealth);
        converted.setHealth((float)reducedMaxHealth);
        if (converted instanceof EntityIMLiving imMob) {
            // Nexus-bound wave mobs are intentionally persistent, but natural
            // replacements must still occupy a slot in Minecraft's monster cap.
            imMob.setCountsTowardMobCap(true);
        }

        // Remove the original before adding its replacement. convertTo adds the
        // new entity first, which lets both entities coexist in the tracker for
        // part of a tick and can appear as duplicate spawns on the client.
        source.discard();
        if (!world.addFreshEntity(converted)) {
            return;
        }
        if (vehicle != null && !vehicle.isRemoved()) {
            converted.startRiding(vehicle);
        }
    }
}
