package com.invasion.client;

import com.invasion.InvScreenHandlers;
import com.invasion.client.render.InvRenderers;
import com.invasion.compat.MutantMonstersCompatibility;
import com.invasion.compat.MutantMonstersRenderers;
import com.invasion.client.screen.NexusScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import com.invasion.block.InvBlockEntities;

public class InvasionModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                com.invasion.network.NexusHudPayload.TYPE,
                (payload, context) -> context.client().execute(() -> NexusHud.update(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> NexusHud.update(com.invasion.network.NexusHudPayload.hidden()));
        NexusHud.bootstrap();
        InvRenderers.bootstrap();
        if (MutantMonstersCompatibility.isLoaded()) {
            MutantMonstersRenderers.bootstrap();
        }
        BlockEntityRendererRegistry.register(
                InvBlockEntities.NEXUS, context -> new BeaconRenderer<>());

        MenuScreens.register(InvScreenHandlers.NEXUS, NexusScreen::new);

    }

}
