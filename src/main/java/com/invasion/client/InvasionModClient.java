package com.invasion.client;

import com.invasion.InvScreenHandlers;
import com.invasion.block.InvBlockEntities;
import com.invasion.client.render.InvRenderers;
import com.invasion.client.screen.NexusScreen;
import com.invasion.network.NexusHudPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class InvasionModClient {
    private InvasionModClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(InvasionModClient::registerRenderers);
        modBus.addListener(InvasionModClient::registerGuiLayers);
        modBus.addListener(InvasionModClient::registerMenuScreens);
        NeoForge.EVENT_BUS.addListener(InvasionModClient::onDisconnect);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        InvRenderers.register(event);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        NexusHud.register(event);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(InvScreenHandlers.NEXUS, NexusScreen::new);
    }

    private static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        NexusHud.update(NexusHudPayload.hidden());
    }
}
