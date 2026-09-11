package com.invasion.nexus;

import java.util.UUID;
import com.invasion.InvasionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

/** Keeps the complete active Nexus arena ticking without nearby players. */
public final class NexusChunkLoader {
    private NexusChunkLoader() {
    }

    public static void register() {
        ForgeChunkManager.setForcedChunkLoadingCallback(
                InvasionMod.MOD_ID, (level, helper) ->
                        helper.getEntityTickets().keySet().forEach(owner -> {
                NexusAccess nexus = WorldNexusStorage.of(level).getNexus(owner);
                if (nexus == null || !nexus.isActive()) {
                    helper.removeAllTickets(owner);
                }
            }));
    }

    static void force(ServerLevel level, UUID owner, BlockPos origin,
            int radius, boolean add) {
        int extent = radius + 10;
        int minChunkX = Math.floorDiv(origin.getX() - extent, 16);
        int maxChunkX = Math.floorDiv(origin.getX() + extent, 16);
        int minChunkZ = Math.floorDiv(origin.getZ() - extent, 16);
        int maxChunkZ = Math.floorDiv(origin.getZ() + extent, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ForgeChunkManager.forceChunk(level, InvasionMod.MOD_ID,
                        owner, chunkX, chunkZ, add, true);
            }
        }
    }
}
