package com.invasion.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.invasion.InvasionMod;
import com.invasion.entity.ai.goal.VanillaMountNexusGoal;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.WorldNexusStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class VanillaMobSpawnReplacement {
    private static final Map<ServerLevel, Set<UUID>> PENDING = new HashMap<>();
    private static final Map<ServerLevel, Set<UUID>> LOADED_REPLACEABLE =
            new HashMap<>();
    private static boolean converting;

    private VanillaMobSpawnReplacement() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::queueVanillaMob);
        NeoForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::removeVanillaMob);
        NeoForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::blockNaturalSpawn);
        NeoForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::processQueue);
    }

    private static void blockNaturalSpawn(FinalizeSpawnEvent event) {
        ServerLevel world = event.getLevel().getLevel();
        if (event.getSpawnType() == EntitySpawnReason.NATURAL
                && world.getDifficulty() != Difficulty.HARD
                && hasActiveNexus(world)
                && isReplaceableType(event.getEntity().getType())) {
            event.setSpawnCancelled(true);
        }
    }

    private static void queueVanillaMob(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel world)) {
            return;
        }
        net.minecraft.world.entity.Entity entity = event.getEntity();
        if (converting || !(entity instanceof Mob mob)
                || !isReplaceableType(mob.getType())) {
            return;
        }
        LOADED_REPLACEABLE.computeIfAbsent(
                world, ignored -> new HashSet<>()).add(mob.getUUID());
        if (hasActiveNexus(world)) {
            PENDING.computeIfAbsent(
                    world, ignored -> new HashSet<>()).add(mob.getUUID());
        }
    }

    private static void removeVanillaMob(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel world)) {
            return;
        }
        Set<UUID> loaded = LOADED_REPLACEABLE.get(world);
        if (loaded != null) {
            loaded.remove(event.getEntity().getUUID());
            if (loaded.isEmpty()) {
                LOADED_REPLACEABLE.remove(world);
            }
        }
    }

    private static void processQueue(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel world)) {
            return;
        }

        // These mobs may already be loaded when the Nexus is activated.
        if (world.getGameTime() % 20L == 0L
                && hasActiveNexus(world)) {
            Set<UUID> loaded = LOADED_REPLACEABLE.get(world);
            if (loaded != null && !loaded.isEmpty()) {
                PENDING.computeIfAbsent(world, ignored -> new HashSet<>())
                        .addAll(loaded);
            }
        }

        Set<UUID> pending = PENDING.remove(world);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        if (!hasActiveNexus(world)) {
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

    static boolean isNightSpawnActive(ServerLevel world) {
        return world.getDifficulty() == Difficulty.HARD
                && !world.isBrightOutside()
                && hasActiveNexus(world);
    }

    private static boolean hasActiveNexus(ServerLevel world) {
        return WorldNexusStorage.of(world).getNexus()
                        .filter(nexus -> nexus.isActive())
                        .isPresent();
    }

    private static void convertMob(
            Mob mob, com.invasion.nexus.NexusAccess nexus) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (isTinySkeleton(typeId, "baby_skeleton")) {
            convert(mob, InvEntities.SKELETON, nexus);
        } else if (isTinySkeleton(typeId, "baby_bogged")) {
            convert(mob, InvEntities.BOGGED, nexus);
        } else if (isTinySkeleton(typeId, "baby_parched")) {
            convert(mob, InvEntities.PARCHED, nexus);
        } else if (isTinySkeleton(typeId, "baby_stray")) {
            convert(mob, InvEntities.STRAY, nexus);
        } else if (isTinySkeleton(typeId, "baby_wither_skeleton")) {
            convert(mob, InvEntities.WITHER_SKELETON, nexus);
        } else if (mob.getType() == EntityTypes.ZOMBIE) {
            if (mob.getRandom().nextInt(10) == 0) {
                convert(mob, InvEntities.FAT_ZOMBIE, nexus);
            } else {
                convert(mob, InvEntities.ZOMBIE, nexus);
            }
        } else if (mob.getType() == EntityTypes.HUSK) {
            convert(mob, InvEntities.HUSK, nexus);
        } else if (mob.getType() == EntityTypes.DROWNED) {
            if (mob.isInWater()
                    && mob.getRandom().nextFloat() < 0.1F) {
                convert(mob, InvEntities.GUARDIAN, nexus);
            } else {
                convert(mob, InvEntities.DROWNED, nexus);
            }
        } else if (mob.getType() == EntityTypes.ELDER_GUARDIAN) {
            convert(mob, InvEntities.ELDER_GUARDIAN, nexus);
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
        } else if (mob.getType() == EntityTypes.WITCH) {
            convert(mob, InvEntities.WITCH, nexus);
        } else if (mob.getType() == EntityTypes.GHAST) {
            convert(mob, InvEntities.GHAST, nexus);
        } else if (mob.getType() == EntityTypes.ZOMBIE_VILLAGER) {
            convert(mob, InvEntities.ZOMBIE_VILLAGER, nexus);
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
        } else if (mob.getType() == EntityTypes.WARDEN) {
            convert(mob, InvEntities.WARDEN, nexus);
        } else if (mob.getType() == EntityTypes.BLAZE) {
            convert(mob, InvEntities.BLAZE, nexus);
        } else if (mob.getType() == EntityTypes.SILVERFISH) {
            convert(mob, InvEntities.SILVERFISH, nexus);
        } else if (mob.getType() == EntityTypes.SLIME) {
            convert(mob, InvEntities.SLIME, nexus);
        } else if (mob.getType() == EntityTypes.MAGMA_CUBE) {
            convert(mob, InvEntities.MAGMA_CUBE, nexus);
        } else if (mob.getType() == EntityTypes.BREEZE) {
            convert(mob, InvEntities.BREEZE, nexus);
        } else if (mob.getType() == EntityTypes.ENDERMITE) {
            convert(mob, InvEntities.ENDERMITE, nexus);
        }
    }

    private static boolean isReplaceableType(EntityType<?> type) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return isTinySkeleton(typeId, "baby_skeleton")
                || isTinySkeleton(typeId, "baby_bogged")
                || isTinySkeleton(typeId, "baby_parched")
                || isTinySkeleton(typeId, "baby_stray")
                || isTinySkeleton(typeId, "baby_wither_skeleton")
                || type == EntityTypes.ZOMBIE
                || type == EntityTypes.HUSK
                || type == EntityTypes.DROWNED
                || type == EntityTypes.ELDER_GUARDIAN
                || type == EntityTypes.ZOMBIFIED_PIGLIN
                || type == EntityTypes.SKELETON
                || type == EntityTypes.BOGGED
                || type == EntityTypes.PARCHED
                || type == EntityTypes.STRAY
                || type == EntityTypes.WITHER_SKELETON
                || type == EntityTypes.WITCH
                || type == EntityTypes.GHAST
                || type == EntityTypes.ZOMBIE_VILLAGER
                || type == EntityTypes.CREEPER
                || type == EntityTypes.SPIDER
                || type == EntityTypes.CAVE_SPIDER
                || type == EntityTypes.ENDERMAN
                || type == EntityTypes.PHANTOM
                || type == EntityTypes.ZOGLIN
                || type == EntityTypes.WITHER
                || type == EntityTypes.WARDEN
                || type == EntityTypes.BLAZE
                || type == EntityTypes.SILVERFISH
                || type == EntityTypes.SLIME
                || type == EntityTypes.MAGMA_CUBE
                || type == EntityTypes.BREEZE
                || type == EntityTypes.ENDERMITE;
    }

    private static boolean isTinySkeleton(Identifier typeId, String path) {
        return typeId != null
                && typeId.getNamespace().equals("tinyskeletons")
                && typeId.getPath().equals(path);
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
        if (converted instanceof IMFatZombieEntity fatZombie) {
            fatZombie.setTier(3);
        }

        Entity vehicle = source.getVehicle();
        source.stopRiding();

        converted.snapTo(
                source.getX(), source.getY(), source.getZ(),
                source.getYRot(), source.getXRot());
        converted.setDeltaMovement(source.getDeltaMovement());
        converted.setBaby(source.isBaby());
        if (BuiltInRegistries.ENTITY_TYPE.getKey(source.getType())
                .getNamespace().equals("tinyskeletons")
                && converted instanceof IMSkeletonEntity skeleton) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                converted.setItemSlot(
                        slot, source.getItemBySlot(slot).copy());
            }
            skeleton.initializeTinySkeletonAbilities();
        }
        converted.setCustomName(source.getCustomName());
        converted.setCustomNameVisible(source.isCustomNameVisible());
        converted.setNoAi(source.isNoAi());
        converted.setCanPickUpLoot(
                source.canPickUpLoot()
                        && com.invasion.compat.AsyncCompatibility
                                .canUseVanillaItemPickup());
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
        if (source instanceof Slime slime
                && converted instanceof IMSlimeEntity imSlime) {
            imSlime.setSize(slime.getSize(), true);
        }
        if (source instanceof net.minecraft.world.entity.monster.cubemob.MagmaCube magmaCube
                && converted instanceof IMMagmaCubeEntity imMagmaCube) {
            imMagmaCube.setSize(magmaCube.getSize(), true);
        }
        if (converted instanceof AbstractIMZombieEntity) {
            converted.setCanPickUpLoot(
                    com.invasion.compat.AsyncCompatibility
                            .canUseVanillaItemPickup());
        }
        if (source.isPersistenceRequired()) {
            converted.setPersistenceRequired();
        }
        double reducedMaxHealth = source.getMaxHealth() * 0.3D;
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
        // Bind only after the replacement has joined the level. This makes
        // setNexus update the loaded/bound registry against the final entity
        // lifecycle state instead of relying on a pre-spawn registration.
        converted.setNexus(nexus);
        if (canContinueRiding(vehicle, converted, nexus)) {
            converted.startRiding(vehicle);
        }
    }

    /**
     * Vanilla jockey mounts receive a Nexus-aware driver goal. Modded mounts
     * can make assumptions about their vanilla passengers, so only keep those
     * when their own navigation can already reach the Nexus.
     */
    private static boolean canContinueRiding(
            Entity vehicle, Mob converted,
            com.invasion.nexus.NexusAccess nexus) {
        if (vehicle == null) {
            return false;
        }
        if (!(vehicle instanceof Mob mount)) {
            logForcedDismount(vehicle, converted, "mount is not a mob");
            return false;
        }
        if (vehicle.isRemoved()) {
            logForcedDismount(vehicle, converted, "mount was removed");
            return false;
        }

        if (isVanillaJockeyMount(mount)
                && mount instanceof net.minecraft.world.entity.PathfinderMob pathfinderMount
                && converted instanceof NexusEntity nexusRider) {
            pathfinderMount.goalSelector.addGoal(
                    2, new VanillaMountNexusGoal(
                            pathfinderMount, converted, nexusRider));
            return true;
        }

        try {
            Path path = mount.getNavigation().createPath(
                    nexus.getOrigin(), 1);
            if (path != null && path.canReach()) {
                return true;
            }
            logForcedDismount(vehicle, converted,
                    "mount cannot navigate to the Nexus");
            return false;
        } catch (RuntimeException | LinkageError error) {
            // Compatibility code in a modded mount may reject an unfamiliar
            // IM passenger or navigation implementation. Dismount safely.
            logForcedDismount(vehicle, converted,
                    "mount navigation failed with "
                            + error.getClass().getSimpleName());
            return false;
        }
    }

    private static void logForcedDismount(
            Entity mount, Mob rider, String reason) {
        Identifier mountType = BuiltInRegistries.ENTITY_TYPE.getKey(
                mount.getType());
        Identifier riderType = BuiltInRegistries.ENTITY_TYPE.getKey(
                rider.getType());
        InvasionMod.LOGGER.warn(
                "Invasion Mod dismounted rider {} from mount {} because {}. "
                        + "Please report this mount combination to the "
                        + "Invasion Mod developer on CurseForge.",
                riderType, mountType, reason);
    }

    private static boolean isVanillaJockeyMount(Mob mount) {
        EntityType<?> type = mount.getType();
        return type == EntityTypes.CHICKEN
                || type == EntityTypes.SKELETON_HORSE
                || type == EntityTypes.ZOMBIE_HORSE
                || type == EntityTypes.STRIDER
                || type == EntityTypes.ZOMBIE_NAUTILUS
                || type == EntityTypes.CAMEL_HUSK;
    }
}
