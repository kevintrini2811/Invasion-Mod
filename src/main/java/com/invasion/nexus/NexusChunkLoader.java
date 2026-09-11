package com.invasion.nexus;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Keeps the complete active Nexus arena ticking without nearby players. */
public final class NexusChunkLoader {
    private static final TicketType TICKET = new TicketType(
            TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION
                    | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

    private NexusChunkLoader() {
    }

    static void force(ServerLevel level, UUID owner, BlockPos origin,
            int radius, boolean add) {
        int extent = radius + 10;
        int centerX = origin.getX() >> 4;
        int centerZ = origin.getZ() >> 4;
        ChunkPos center = new ChunkPos(centerX, centerZ);
        int minChunkX = Math.floorDiv(origin.getX() - extent, 16);
        int maxChunkX = Math.floorDiv(origin.getX() + extent, 16);
        int minChunkZ = Math.floorDiv(origin.getZ() - extent, 16);
        int maxChunkZ = Math.floorDiv(origin.getZ() + extent, 16);
        int ticketRadius = Math.max(
                Math.max(centerX - minChunkX, maxChunkX - centerX),
                Math.max(centerZ - minChunkZ, maxChunkZ - centerZ));
        if (add) {
            level.getChunkSource().addTicketWithRadius(TICKET, center, ticketRadius);
        } else {
            level.getChunkSource().removeTicketWithRadius(TICKET, center, ticketRadius);
        }
    }
}
