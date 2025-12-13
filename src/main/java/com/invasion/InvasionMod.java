package com.invasion;

import com.invasion.nexus.wave.EntityPatterns;
import com.invasion.util.ChatUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.invasion.block.InvBlocks;
import com.invasion.entity.InvEntities;
import com.invasion.item.InvItems;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.particle.InvParticles;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;



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
        return Identifier.of("invmod", name);
    }

    @Override
    public void onInitialize() {
        CONFIG.loadConfig(FabricLoader.getInstance().getConfigDir().resolve("invasion_config.cfg").toFile());
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> {
            dispatcher.register(InvasionCommand.create(dispatcher, registries));
        });
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            BountyHunter.of(world).tick();
            WorldNexusStorage.of(world).tick();
        });
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            ChatUtils.setServer(server);
            LOGGER.info("ChatUtils: Server gesetzt.");
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
            ChatUtils.clearServer();
            LOGGER.info("ChatUtils: Server gelöscht.");
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SERVER = server);
        InvBlocks.bootstrap();
        InvItems.bootstrap();
        InvSounds.boostrap();
        InvEntities.bootstrap();
        InvParticles.bootstrap();
        InvScreenHandlers.bootstrap();
        // Keine Drops von Invasions-Mobs (inkl. Mutant Monsters & Giant)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // Nur Serverwelt
            if (!(entity.getWorld() instanceof ServerWorld world)) return;
            if (!(entity instanceof MobEntity mob)) return;

            boolean isInvasionMob = false;

            // 1) Externe Mobs (Mutant Monsters, Giant etc.)
            if (EntityPatterns.isExternalInvasionMob(mob.getType())) {
                isInvasionMob = true;
            } else {
                // 2) Eigene Invasion-Mobs: über Team "invasion_allies"
                Scoreboard scoreboard = world.getScoreboard();
                Team team = scoreboard.getScoreHolderTeam(mob.getNameForScoreboard());
                if (team != null && "invasion_allies".equals(team.getName())) {
                    isInvasionMob = true;
                }
            }

            if (!isInvasionMob) return;

            // Alle Item-Entities in der Nähe des toten Mobs sofort entfernen
            var box = mob.getBoundingBox().expand(3.0);
            world.getEntitiesByClass(ItemEntity.class, box, item -> true)
                    .forEach(ItemEntity::discard);
        });

    }
}
