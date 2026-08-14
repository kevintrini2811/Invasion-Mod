package com.invasion.client;

import com.invasion.InvScreenHandlers;
import com.invasion.block.InvBlockEntities;
import com.invasion.client.render.legacy.InvRenderers;
import com.invasion.compat.MutantMonstersCompatibility;
import com.invasion.compat.MutantMonstersRenderers;
import com.invasion.client.screen.NexusScreen;
import com.invasion.item.InvItems;
import com.invasion.network.NexusHudPayload;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;

public final class InvasionModClient {
    private InvasionModClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(InvasionModClient::clientSetup);
        modBus.addListener(InvasionModClient::registerRenderers);
        modBus.addListener(InvasionModClient::registerGuiLayers);
        modBus.addListener(InvasionModClient::registerItemColors);
        MinecraftForge.EVENT_BUS.addListener(InvasionModClient::onDisconnect);
    }

    private static void registerItemColors(
            RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> ((SpawnEggItem) stack.getItem())
                        .getColor(tintIndex),
                InvItems.SPAWN_EGGS.toArray(Item[]::new));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(InvItems.SEARING_BOW,
                    new ResourceLocation("pull"),
                    (stack, level, entity, seed) -> entity == null
                            || entity.getUseItem() != stack ? 0.0F
                            : (stack.getUseDuration()
                                    - entity.getUseItemRemainingTicks()) / 20.0F);
            ItemProperties.register(InvItems.SEARING_BOW,
                    new ResourceLocation("pulling"),
                    (stack, level, entity, seed) -> entity != null
                            && entity.isUsingItem() && entity.getUseItem() == stack
                                    ? 1.0F : 0.0F);
            MenuScreens.register(InvScreenHandlers.NEXUS, NexusScreen::new);
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        InvRenderers.register(event);
        if (MutantMonstersCompatibility.isLoaded()) MutantMonstersRenderers.register(event);
    }

    private static void registerGuiLayers(RegisterGuiOverlaysEvent event) {
        NexusHud.register(event);
    }

    private static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        NexusHud.update(NexusHudPayload.hidden());
    }
}
