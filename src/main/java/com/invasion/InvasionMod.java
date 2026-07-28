package com.invasion;

import com.invasion.nexus.wave.EntityPatterns;
import com.invasion.util.ChatUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.invasion.block.InvBlocks;
import com.invasion.entity.InvEntities;
import com.invasion.entity.VanillaMobSpawnReplacement;
import com.invasion.item.InvItems;
import com.invasion.nexus.WorldNexusStorage;
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

        ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
            ChatUtils.clearServer();
            LOGGER.debug("ChatUtils: Server gelöscht.");
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SERVER = server);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                WorldNexusStorage.of((ServerLevel)handler.player.level()).onPlayerJoined(handler.player));
        InvBlocks.bootstrap();
        InvItems.bootstrap();
        InvSounds.boostrap();
        InvEntities.bootstrap();
        VanillaMobSpawnReplacement.bootstrap();
        InvParticles.bootstrap();
        InvScreenHandlers.bootstrap();
        // Keine Drops von Invasions-Mobs (inkl. Mutant Monsters & Giant)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // Nur Serverwelt
            if (!(entity.level() instanceof ServerLevel world)) return;
            if (!(entity instanceof Mob mob)) return;
            if (!EntityPatterns.isExternalInvasionMob(mob.getType())) return;

            // Alle Item-Entities in der Nähe des toten Mobs sofort entfernen
            var box = mob.getBoundingBox().inflate(3.0);
            world.getEntitiesOfClass(ItemEntity.class, box, item -> true)
                    .forEach(ItemEntity::discard);
        });

    }
}
