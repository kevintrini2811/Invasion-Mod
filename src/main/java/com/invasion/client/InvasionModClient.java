package com.invasion.client;

import com.invasion.InvScreenHandlers;
import com.invasion.block.InvBlockEntities;
import com.invasion.client.render.InvRenderers;
import com.invasion.client.screen.NexusScreen;
import com.invasion.item.InvItems;
import com.invasion.entity.InvEntities;
import com.invasion.network.NexusHudPayload;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class InvasionModClient {
    private InvasionModClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(InvasionModClient::clientSetup);
        modBus.addListener(InvasionModClient::registerRenderers);
        modBus.addListener(InvasionModClient::registerGuiLayers);
        modBus.addListener(InvasionModClient::registerMenuScreens);
        modBus.addListener(InvasionModClient::registerItemColors);
        NeoForge.EVENT_BUS.addListener(InvasionModClient::onDisconnect);
    }

    private static void registerItemColors(
            RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> FastColor.ARGB32.opaque(
                        ((SpawnEggItem) stack.getItem()).getColor(tintIndex)),
                InvItems.SPAWN_EGGS.toArray(Item[]::new));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(InvEntities.WITCH, WitchRenderer::new);
            EntityRenderers.register(
                    InvEntities.WITCH_POTION, ThrownItemRenderer::new);
            ItemProperties.register(InvItems.SEARING_BOW,
                    ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> entity == null
                            || entity.getUseItem() != stack ? 0.0F
                            : (stack.getUseDuration(entity)
                                    - entity.getUseItemRemainingTicks()) / 20.0F);
            ItemProperties.register(InvItems.SEARING_BOW,
                    ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null
                            && entity.isUsingItem() && entity.getUseItem() == stack
                                    ? 1.0F : 0.0F);
        });
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
