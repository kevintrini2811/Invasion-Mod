package com.invasion;

import com.invasion.block.InvBlocks;
import com.invasion.client.InvasionModClient;
import com.invasion.compat.AsyncCompatibility;
import com.invasion.entity.InvEntities;
import com.invasion.entity.VanillaMobSpawnReplacement;
import com.invasion.entity.NexusBoundMobLifecycle;
import com.invasion.entity.VillagerResurrectionHandler;
import com.invasion.entity.InfectionDeathHandler;
import com.invasion.entity.IMCivilianTargetHandler;
import com.invasion.item.InvItems;
import com.invasion.network.NexusHudPayload;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.particle.InvParticles;
import com.invasion.util.ChatUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(InvasionMod.MOD_ID)
public class InvasionMod {
    public static final String MOD_ID = "invmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(InvasionMod.class);
    public static MinecraftServer SERVER;
    private static final InvasionConfig CONFIG = new InvasionConfig();

    public InvasionMod(IEventBus modBus, ModContainer container) {
        // Read registration-affecting settings (notably debug mode) before the
        // registry events. The config writer is deferred until common setup,
        // because its generated entity section needs registered entity types.
        CONFIG.loadConfig(FMLPaths.CONFIGDIR.get().resolve("invasion_config.cfg").toFile());

        modBus.addListener(this::registerContent);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::addCreativeItems);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::startLevelTick);
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        NeoForge.EVENT_BUS.addListener(this::serverStarting);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);
        NeoForge.EVENT_BUS.addListener(this::playerJoined);
        NeoForge.EVENT_BUS.addListener(this::playerChangedDimension);
        NeoForge.EVENT_BUS.addListener(this::fuelBurnTime);

        VanillaMobSpawnReplacement.bootstrap();
        NexusBoundMobLifecycle.bootstrap();
        VillagerResurrectionHandler.bootstrap();
        InfectionDeathHandler.bootstrap();
        IMCivilianTargetHandler.bootstrap();

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            InvasionModClient.register(modBus);
        }
    }

    public static void log(@Nullable String s) {
        if (getConfig().enableLog && s != null) {
            LOGGER.warn(s);
        }
    }

    public static InvasionConfig getConfig() {
        return CONFIG;
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(NexusHudPayload.TYPE, NexusHudPayload.CODEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CONFIG.loadConfig(FMLPaths.CONFIGDIR.get().resolve("invasion_config.cfg").toFile());
    }

    private void registerContent(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.BLOCK) {
            InvBlocks.bootstrap();
        } else if (event.getRegistryKey() == Registries.ENTITY_TYPE) {
            InvEntities.bootstrap();
        } else if (event.getRegistryKey() == Registries.ITEM) {
            InvItems.bootstrap();
        } else if (event.getRegistryKey() == Registries.BLOCK_ENTITY_TYPE) {
            com.invasion.block.InvBlockEntities.bootstrap();
        } else if (event.getRegistryKey() == Registries.SOUND_EVENT) {
            InvSounds.boostrap();
        } else if (event.getRegistryKey() == Registries.PARTICLE_TYPE) {
            InvParticles.bootstrap();
        } else if (event.getRegistryKey() == Registries.MENU) {
            InvScreenHandlers.bootstrap();
        } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            InvItems.bootstrapCreativeTab();
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        InvEntities.registerAttributes(event);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        InvItems.addCreativeItems(event);
    }

    private void fuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        InvItems.fuelBurnTime(event);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                InvasionCommand.create(event.getDispatcher(), event.getBuildContext()));
    }

    private void startLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel world) {
            BountyHunter.of(world).tick();
            WorldNexusStorage.of(world).tick();
        }
    }

    private void serverStarted(ServerStartedEvent event) {
        ChatUtils.setServer(event.getServer());
        LOGGER.debug("ChatUtils: Server gesetzt.");
    }

    private void serverStarting(ServerStartingEvent event) {
        SERVER = event.getServer();
        AsyncCompatibility.registerSynchronizedEntities();
    }

    private void serverStopped(ServerStoppedEvent event) {
        ChatUtils.clearServer();
        SERVER = null;
        LOGGER.debug("ChatUtils: Server gelöscht.");
    }

    private void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof ServerLevel world) {
            WorldNexusStorage.of(world).onPlayerJoined(player);
        }
    }

    private void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof ServerLevel world) {
            PacketDistributor.sendToPlayer(player, NexusHudPayload.hidden());
            WorldNexusStorage.of(world).onPlayerJoined(player);
        }
    }
}
