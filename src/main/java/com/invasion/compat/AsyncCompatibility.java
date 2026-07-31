package com.invasion.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import com.invasion.InvasionMod;
import net.minecraftforge.fml.ModList;

public final class AsyncCompatibility {
    private static final String ASYNC_CONFIG =
            "com.axalotl.async.common.config.AsyncConfig";
    private static final String INVASION_NAMESPACE = "invmod:*";

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
            if (configuredEntities instanceof Set<?> synchronizedEntities
                    && synchronizedEntities.contains(INVASION_NAMESPACE)) {
                return;
            }

            Method syncEntity = configClass.getMethod(
                    "syncEntity", String.class);
            syncEntity.invoke(null, INVASION_NAMESPACE);
            InvasionMod.LOGGER.info(
                    "Registered {} with Async synchronizedEntities",
                    INVASION_NAMESPACE);
        } catch (ReflectiveOperationException | LinkageError exception) {
            InvasionMod.LOGGER.error(
                    "Async is installed, but its synchronizedEntities "
                            + "integration could not be initialized",
                    exception);
        }
    }
}
