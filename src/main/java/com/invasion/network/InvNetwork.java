package com.invasion.network;

import com.invasion.InvasionMod;
import com.invasion.client.NexusHud;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class InvNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            InvasionMod.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private InvNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, NexusHudPayload.class,
                NexusHudPayload::encode, NexusHudPayload::decode,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> NexusHud.update(payload));
                    context.setPacketHandled(true);
                });
    }

    public static void send(ServerPlayer player, NexusHudPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
