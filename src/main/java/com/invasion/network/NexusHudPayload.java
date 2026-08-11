package com.invasion.network;

import net.minecraft.network.FriendlyByteBuf;

public record NexusHudPayload(boolean active, boolean continuous, int wave,
                              int phase, int phaseCount, int defeatedMobs, int totalMobs,
                              int nexusHealthPercent) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeBoolean(continuous);
        buffer.writeVarInt(wave);
        buffer.writeVarInt(phase);
        buffer.writeVarInt(phaseCount);
        buffer.writeVarInt(defeatedMobs);
        buffer.writeVarInt(totalMobs);
        buffer.writeVarInt(nexusHealthPercent);
    }

    public static NexusHudPayload decode(FriendlyByteBuf buffer) {
        return new NexusHudPayload(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static NexusHudPayload hidden() {
        return new NexusHudPayload(false, false, 0, 0, 0, 0, 0, 0);
    }
}
