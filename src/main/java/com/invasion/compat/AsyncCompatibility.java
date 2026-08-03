package com.invasion.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.invasion.InvasionMod;
import net.neoforged.fml.ModList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class AsyncCompatibility {
    private static final String ASYNC_CONFIG =
            "com.axalotl.async.common.config.AsyncConfig";
    private static final List<String> SYNCHRONIZED_ENTITIES = List.of(
            "invmod:*",
            "minecraft:pig",
            "minecraft:piglin",
            "minecraft:piglin_brute",
            "minecraft:villager");

    private AsyncCompatibility() {
    }

    /**
     * Async 0.2.4 routes vanilla Mob item pickup through async-api. On
     * NeoForge 26.2 that hook can load ItemEntity through the FML plugin
     * classloader and crash with a loader constraint violation. Invasion mobs
     * that need equipment use their own server-side pickup scan, so disabling
     * Mob.aiStep's automatic pickup is the safe compatibility path.
     */
    public static boolean canUseVanillaItemPickup() {
        return !ModList.get().isLoaded("async");
    }

    /**
     * Equivalent to Mob.pickUpItem without entering Async's broken wrapper.
     * Keep this path for Invasion's explicit equipment scans even when Async
     * is not installed so their behaviour does not depend on mixin presence.
     */
    public static void pickUpEquipment(
            Mob mob, ServerLevel level, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        ItemStack equipped = mob.equipItemIfPossible(level, stack.copy());
        if (equipped.isEmpty()) {
            return;
        }

        mob.onItemPickup(entity);
        mob.take(entity, equipped.getCount());
        stack.shrink(equipped.getCount());
        if (stack.isEmpty()) {
            entity.discard();
        }
    }

    public static void registerSynchronizedEntities() {
        if (!ModList.get().isLoaded("async")) {
            return;
        }

        try {
            Class<?> configClass = Class.forName(ASYNC_CONFIG);
            Field synchronizedEntitiesField =
                    configClass.getField("synchronizedEntities");
            Object configuredEntities = synchronizedEntitiesField.get(null);
            Method syncEntity = configClass.getMethod(
                    "syncEntity", String.class);
            for (String entity : SYNCHRONIZED_ENTITIES) {
                if (configuredEntities instanceof Set<?> synchronizedEntities
                        && synchronizedEntities.contains(entity)) {
                    continue;
                }
                syncEntity.invoke(null, entity);
                InvasionMod.LOGGER.info(
                        "Registered {} with Async synchronizedEntities",
                        entity);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            InvasionMod.LOGGER.error(
                    "Async is installed, but its synchronizedEntities "
                            + "integration could not be initialized",
                    exception);
        }
    }
}
