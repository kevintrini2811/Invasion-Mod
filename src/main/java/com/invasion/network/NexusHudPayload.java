package com.invasion.network;

import com.invasion.InvasionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NexusHudPayload(boolean active, int defeatedMobs, int totalMobs, int nexusHealthPercent)
        implements CustomPacketPayload {
    public static final Type<NexusHudPayload> TYPE = new Type<>(InvasionMod.id("nexus_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NexusHudPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.active);
                buffer.writeVarInt(payload.defeatedMobs);
                buffer.writeVarInt(payload.totalMobs);
                buffer.writeVarInt(payload.nexusHealthPercent);
            },
            buffer -> new NexusHudPayload(
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()));

    public static NexusHudPayload hidden() {
        return new NexusHudPayload(false, 0, 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
