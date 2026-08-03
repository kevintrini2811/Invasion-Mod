package com.invasion.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.invasion.InvasionMod;
import net.neoforged.fml.ModList;

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
