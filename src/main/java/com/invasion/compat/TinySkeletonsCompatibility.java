package com.invasion.compat;

import net.fabricmc.loader.api.FabricLoader;

/** Keeps all Tiny Skeletons integration behind an optional mod check. */
public final class TinySkeletonsCompatibility {
    private TinySkeletonsCompatibility() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("tinyskeletons");
    }
}
