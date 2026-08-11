package com.invasion;

import com.invasion.compat.AsyncCompatibility;
import com.invasion.entity.BoundIMMobRegistry;
import com.invasion.entity.InfectionDeathHandler;
import com.invasion.entity.IMCivilianTargetHandler;
import com.invasion.entity.IronGolemTargetHandler;
import com.invasion.entity.PlayerAllyTargetHandler;
import com.invasion.util.ChatUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.invasion.block.InvBlocks;
import com.invasion.entity.InvEntities;
import com.invasion.entity.VanillaMobSpawnReplacement;
import com.invasion.entity.NexusBoundMobLifecycle;
import com.invasion.entity.IMBlazeEntity;
import com.invasion.entity.VillagerResurrectionHandler;
import com.invasion.item.InvItems;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.network.NexusHudPayload;
import com.invasion.particle.InvParticles;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;



public class InvasionMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(InvasionMod.class);
    public static MinecraftServer SERVER;
    private static final InvasionConfig CONFIG = new InvasionConfig();

    public static void log(@Nullable String s) {
        if (InvasionMod.getConfig().enableLog && s != null) {
            InvasionMod.LOGGER.warn(s);
        }
    }

    public static InvasionConfig getConfig() {
        return CONFIG;
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("invmod", name);
    }

    @Override
    public void onInitialize() {
        BoundIMMobRegistry.bootstrap();
        NexusBoundMobLifecycle.bootstrap();
        IMBlazeEntity.bootstrap();
        PayloadTypeRegistry.clientboundPlay().register(NexusHudPayload.TYPE, NexusHudPayload.CODEC);
        CONFIG.loadConfig(FabricLoader.getInstance().getConfigDir().resolve("invasion_config.cfg").toFile());
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> {
            dispatcher.register(InvasionCommand.create(dispatcher, registries));
        });
        ServerTickEvents.START_LEVEL_TICK.register(world -> {
            BountyHunter.of(world).tick();
            WorldNexusStorage.of(world).tick();
        });
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            ChatUtils.setServer(server);
            LOGGER.debug("ChatUtils: Server gesetzt.");
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SERVER = server;
            AsyncCompatibility.registerSynchronizedEntities();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
            ChatUtils.clearServer();
            LOGGER.debug("ChatUtils: Server gelöscht.");
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                WorldNexusStorage.of((ServerLevel)handler.player.level()).onPlayerJoined(handler.player));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            if (ServerPlayNetworking.canSend(player, NexusHudPayload.TYPE)) {
                ServerPlayNetworking.send(player, NexusHudPayload.hidden());
            }
            WorldNexusStorage.of(destination).onPlayerJoined(player);
        });
        InvBlocks.bootstrap();
        InvItems.bootstrap();
        InvSounds.boostrap();
        InvEntities.bootstrap();
        InvMobEffects.bootstrap();
        VanillaMobSpawnReplacement.bootstrap();
        VillagerResurrectionHandler.bootstrap();
        InfectionDeathHandler.bootstrap();
        IMCivilianTargetHandler.bootstrap();
        IronGolemTargetHandler.bootstrap();
        PlayerAllyTargetHandler.bootstrap();
        InvParticles.bootstrap();
        InvScreenHandlers.bootstrap();
    }
}
