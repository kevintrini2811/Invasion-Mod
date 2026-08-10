package com.invasion.compat;

import net.neoforged.fml.ModList;

/** Keeps all Tiny Skeletons integration behind an optional mod check. */
public final class TinySkeletonsCompatibility {
    private TinySkeletonsCompatibility() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("tinyskeletons");
    }
}
