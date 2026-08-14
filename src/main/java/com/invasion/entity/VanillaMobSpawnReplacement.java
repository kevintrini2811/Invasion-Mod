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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.TickEvent;

public final class VanillaMobSpawnReplacement {
    private static final Map<ServerLevel, Set<UUID>> PENDING = new HashMap<>();
    private static final Map<ServerLevel, Set<UUID>> LOADED_REPLACEABLE =
            new HashMap<>();
    private static boolean converting;

    private VanillaMobSpawnReplacement() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::queueVanillaMob);
        MinecraftForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::removeVanillaMob);
        MinecraftForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::blockNaturalSpawn);
        MinecraftForge.EVENT_BUS.addListener(VanillaMobSpawnReplacement::processQueue);
    }

    private static void blockNaturalSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel world)) return;
        if (event.getSpawnType() == MobSpawnType.NATURAL
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

    private static void processQueue(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel world)) {
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
                && !world.isDay()
                && hasActiveNexus(world);
    }

    private static boolean hasActiveNexus(ServerLevel world) {
        return WorldNexusStorage.of(world).getNexus()
                        .filter(nexus -> nexus.isActive())
                        .isPresent();
    }

    private static void convertMob(
            Mob mob, com.invasion.nexus.NexusAccess nexus) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (isTinySkeleton(typeId, "baby_skeleton")) {
            convert(mob, InvEntities.SKELETON, nexus);
        } else if (isTinySkeleton(typeId, "baby_parched")) {
            convert(mob, InvEntities.SKELETON, nexus);
        } else if (isTinySkeleton(typeId, "baby_stray")) {
            convert(mob, InvEntities.STRAY, nexus);
        } else if (isTinySkeleton(typeId, "baby_wither_skeleton")) {
            convert(mob, InvEntities.WITHER_SKELETON, nexus);
        } else if (mob.getType() == EntityType.ZOMBIE) {
            convert(mob, InvEntities.ZOMBIE, nexus);
        } else if (mob.getType() == EntityType.HUSK) {
            convert(mob, InvEntities.HUSK, nexus);
        } else if (mob.getType() == EntityType.DROWNED) {
            if (mob.isInWater() && mob.getRandom().nextFloat() < 0.1F) {
                convert(mob, InvEntities.GUARDIAN, nexus);
            } else {
                convert(mob, InvEntities.DROWNED, nexus);
            }
        } else if (mob.getType() == EntityType.ELDER_GUARDIAN) {
            convert(mob, InvEntities.ELDER_GUARDIAN, nexus);
        } else if (mob.getType() == EntityType.ZOMBIFIED_PIGLIN) {
            convert(mob, InvEntities.ZOMBIFIED_PIGLIN, nexus);
        } else if (mob.getType() == EntityType.SKELETON) {
            convert(mob, InvEntities.SKELETON, nexus);
        } else if (mob.getType() == EntityType.SKELETON) {
            convert(mob, InvEntities.SKELETON, nexus);
        } else if (mob.getType() == EntityType.STRAY) {
            convert(mob, InvEntities.STRAY, nexus);
        } else if (mob.getType() == EntityType.WITHER_SKELETON) {
            convert(mob, InvEntities.WITHER_SKELETON, nexus);
        } else if (mob.getType() == EntityType.WITCH) {
            convert(mob, InvEntities.WITCH, nexus);
        } else if (mob.getType() == EntityType.GHAST) {
            convert(mob, InvEntities.GHAST, nexus);
        } else if (mob.getType() == EntityType.ZOMBIE_VILLAGER) {
            convert(mob, InvEntities.ZOMBIE_VILLAGER, nexus);
        } else if (mob.getType() == EntityType.CREEPER) {
            convert(mob, InvEntities.CREEPER, nexus);
        } else if (mob.getType() == EntityType.SPIDER) {
            convert(mob, InvEntities.SPIDER, nexus);
        } else if (mob.getType() == EntityType.CAVE_SPIDER) {
            convert(mob, InvEntities.CAVE_SPIDER, nexus);
        } else if (mob.getType() == EntityType.ENDERMAN) {
            convert(mob, InvEntities.ENDERMAN, nexus);
        } else if (mob.getType() == EntityType.PHANTOM) {
            convert(mob, InvEntities.PHANTOM, nexus);
        } else if (mob.getType() == EntityType.ZOGLIN) {
            convert(mob, InvEntities.ZOGLIN, nexus);
        } else if (mob.getType() == EntityType.WITHER) {
            convert(mob, InvEntities.WITHER, nexus);
        } else if (mob.getType() == EntityType.WARDEN) {
            convert(mob, InvEntities.WARDEN, nexus);
        } else if (mob.getType() == EntityType.BLAZE) {
            convert(mob, InvEntities.BLAZE, nexus);
        } else if (mob.getType() == EntityType.SILVERFISH) {
            convert(mob, InvEntities.SILVERFISH, nexus);
        } else if (mob.getType() == EntityType.SLIME) {
            convert(mob, InvEntities.SLIME, nexus);
        } else if (mob.getType() == EntityType.MAGMA_CUBE) {
            convert(mob, InvEntities.MAGMA_CUBE, nexus);
        } else if (mob.getType() == EntityType.ENDERMITE) {
            convert(mob, InvEntities.ENDERMITE, nexus);
        }
    }

    private static boolean isReplaceableType(EntityType<?> type) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return isTinySkeleton(typeId, "baby_skeleton")
                || isTinySkeleton(typeId, "baby_parched")
                || isTinySkeleton(typeId, "baby_stray")
                || isTinySkeleton(typeId, "baby_wither_skeleton")
                || type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.SKELETON
                || type == EntityType.SKELETON
                || type == EntityType.STRAY
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.WITCH
                || type == EntityType.GHAST
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.CREEPER
                || type == EntityType.SPIDER
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.ENDERMAN
                || type == EntityType.PHANTOM
                || type == EntityType.ZOGLIN
                || type == EntityType.WITHER
                || type == EntityType.WARDEN
                || type == EntityType.BLAZE
                || type == EntityType.SILVERFISH
                || type == EntityType.SLIME
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.ENDERMITE;
    }

    private static boolean isTinySkeleton(ResourceLocation typeId, String path) {
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

        T converted = targetType.create(world);
        if (converted == null) {
            return;
        }
        Entity vehicle = source.getVehicle();
        source.stopRiding();

        converted.moveTo(
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
            imPiglin.setRemainingPersistentAngerTime(
                    piglin.getRemainingPersistentAngerTime());
            imPiglin.setPersistentAngerTarget(
                    piglin.getPersistentAngerTarget());
        }
        if (source instanceof Phantom phantom
                && converted instanceof IMPhantomEntity imPhantom) {
            imPhantom.setPhantomSize(phantom.getPhantomSize());
        }
        if (source instanceof Slime slime
                && converted instanceof IMSlimeEntity imSlime) {
            imSlime.setSize(slime.getSize(), true);
        }
        if (source instanceof net.minecraft.world.entity.monster.MagmaCube magmaCube
                && converted instanceof IMMagmaCubeEntity imMagmaCube) {
            imMagmaCube.setSize(magmaCube.getSize(), true);
        }
        if (converted instanceof AbstractIMZombieEntity) {
            converted.setCanPickUpLoot(true);
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
            pathfinderMount.goalSelector.addGoal(2, new VanillaMountNexusGoal(
                    pathfinderMount, converted, nexusRider));
            return true;
        }
        try {
            Path path = mount.getNavigation().createPath(nexus.getOrigin(), 1);
            if (path != null && path.canReach()) {
                return true;
            }
            logForcedDismount(vehicle, converted,
                    "mount cannot navigate to the Nexus");
            return false;
        } catch (RuntimeException | LinkageError error) {
            logForcedDismount(vehicle, converted,
                    "mount navigation failed with "
                            + error.getClass().getSimpleName());
            return false;
        }
    }

    private static void logForcedDismount(Entity mount, Mob rider, String reason) {
        ResourceLocation mountType = BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType());
        ResourceLocation riderType = BuiltInRegistries.ENTITY_TYPE.getKey(rider.getType());
        InvasionMod.LOGGER.warn(
                "Invasion Mod dismounted rider {} from mount {} because {}. "
                        + "Please report this mount combination to the "
                        + "Invasion Mod developer on CurseForge.",
                riderType, mountType, reason);
    }

    private static boolean isVanillaJockeyMount(Mob mount) {
        EntityType<?> type = mount.getType();
        return type == EntityType.CHICKEN
                || type == EntityType.SKELETON_HORSE
                || type == EntityType.STRIDER;
    }
}
