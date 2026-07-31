package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.InvScreenHandlers;
import com.invasion.client.render.InvRenderers;
import com.invasion.client.render.animation.AnimationLoader;
import com.invasion.client.screen.NexusScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = InvasionMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InvasionModClient {
    private InvasionModClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        InvRenderers.registerRenderers(event);
    }

    @SubscribeEvent
    public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        InvRenderers.registerParticles(event);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(InvScreenHandlers.NEXUS, NexusScreen::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(AnimationLoader.INSTANCE);
    }
}
