package com.invasion;

import com.invasion.block.InvBlocks;
import com.invasion.compat.AsyncCompatibility;
import com.invasion.entity.InvEntities;
import com.invasion.entity.InvEntities.Attributes;
import com.invasion.item.InvItems;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.nexus.wave.EntityPatterns;
import com.invasion.particle.InvParticles;
import com.invasion.util.ChatUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(InvasionMod.MOD_ID)
public final class InvasionMod {
    public static final String MOD_ID = "invmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(InvasionMod.class);
    public static MinecraftServer SERVER;
    private static final InvasionConfig CONFIG = new InvasionConfig();

    public InvasionMod(IEventBus modBus, ModContainer container) {
        modBus.addListener(InvasionMod::registerContent);
        modBus.addListener(InvasionMod::commonSetup);
        modBus.addListener(Attributes::register);
        modBus.addListener(InvasionMod::buildCreativeTab);

        NeoForge.EVENT_BUS.addListener(InvasionMod::registerCommands);
        NeoForge.EVENT_BUS.addListener(InvasionMod::tickLevel);
        NeoForge.EVENT_BUS.addListener(InvasionMod::serverStarting);
        NeoForge.EVENT_BUS.addListener(InvasionMod::serverStarted);
        NeoForge.EVENT_BUS.addListener(InvasionMod::serverStopped);
        NeoForge.EVENT_BUS.addListener(InvasionMod::livingDeath);
        NeoForge.EVENT_BUS.addListener(InvasionMod::fuelBurnTime);
    }

    public static void log(@Nullable String message) {
        if (CONFIG.enableLog && message != null) {
            LOGGER.warn(message);
        }
    }

    public static InvasionConfig getConfig() {
        return CONFIG;
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    private static void registerContent(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            InvBlocks.bootstrap();
        } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            com.invasion.block.InvBlockEntities.bootstrap();
        } else if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            InvEntities.bootstrap();
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            InvItems.bootstrap();
        } else if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            InvItems.bootstrapCreativeTab();
        } else if (event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
            InvSounds.boostrap();
        } else if (event.getRegistryKey().equals(Registries.PARTICLE_TYPE)) {
            InvParticles.bootstrap();
        } else if (event.getRegistryKey().equals(Registries.MENU)) {
            InvScreenHandlers.bootstrap();
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CONFIG.loadConfig(FMLPaths.CONFIGDIR.get().resolve("invasion_config.cfg").toFile());
            AsyncCompatibility.registerSynchronizedEntities();
        });
    }

    private static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            InvItems.SPAWN_EGGS.forEach(event::accept);
        }
    }

    private static void fuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        if (event.getItemStack().is(InvItems.NEXUS_CATALYST)) {
            event.setBurnTime(10);
        } else if (event.getItemStack().is(InvItems.STABLE_NEXUS_CATALYST)) {
            event.setBurnTime(16);
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(InvasionCommand.create(event.getDispatcher(), event.getBuildContext()));
    }

    private static void tickLevel(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BountyHunter.of(level).tick();
            WorldNexusStorage.of(level).tick();
        }
    }

    private static void serverStarting(ServerStartingEvent event) {
        SERVER = event.getServer();
    }

    private static void serverStarted(ServerStartedEvent event) {
        ChatUtils.setServer(event.getServer());
        LOGGER.info("ChatUtils: Server gesetzt.");
    }

    private static void serverStopped(ServerStoppedEvent event) {
        ChatUtils.clearServer();
        SERVER = null;
        LOGGER.info("ChatUtils: Server gelöscht.");
    }

    private static void livingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        boolean invasionMob = EntityPatterns.isExternalInvasionMob(mob.getType());
        if (!invasionMob) {
            Scoreboard scoreboard = level.getScoreboard();
            Team team = scoreboard.getPlayersTeam(mob.getScoreboardName());
            invasionMob = team != null && "invasion_allies".equals(team.getName());
        }
        if (!invasionMob) {
            return;
        }

        level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(3.0))
                .forEach(ItemEntity::discard);
    }
}
