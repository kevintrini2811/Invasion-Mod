package com.invasion;

import com.invasion.block.InvBlocks;
import com.invasion.client.InvasionModClient;
import com.invasion.compat.AsyncCompatibility;
import com.invasion.compat.InfernalMobsCompatibility;
import com.invasion.compat.MutantMonstersCompatibility;
import com.invasion.compat.FriendsAndFoesCompatibility;
import com.invasion.compat.ConfiguredModMobs;
import com.invasion.entity.InvEntities;
import com.invasion.entity.BoundIMMobRegistry;
import com.invasion.entity.VanillaMobSpawnReplacement;
import com.invasion.entity.NexusBoundMobLifecycle;
import com.invasion.entity.VillagerResurrectionHandler;
import com.invasion.entity.InfectionDeathHandler;
import com.invasion.entity.IMCivilianTargetHandler;
import com.invasion.entity.IMMobFriendlyFireHandler;
import com.invasion.entity.IronGolemTargetHandler;
import com.invasion.entity.PlayerAllyTargetHandler;
import com.invasion.item.InvItems;
import com.invasion.item.InfusedSwordChargeHandler;
import com.invasion.network.NexusHudPayload;
import com.invasion.network.InvNetwork;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.particle.InvParticles;
import com.invasion.util.ChatUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

@Mod(InvasionMod.MOD_ID)
public class InvasionMod {
    public static final String MOD_ID = "invmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(InvasionMod.class);
    public static MinecraftServer SERVER;
    public static InvasionMod INSTANCE;
    private static final InvasionConfig CONFIG = new InvasionConfig();
    private RegisterEvent activeRegisterEvent;
    private final List<PendingRegistration<?, ?>> pendingRegistrations = new ArrayList<>();

    public InvasionMod() {
        this(FMLJavaModLoadingContext.get().getModEventBus(),
                ModLoadingContext.get().getActiveContainer());
    }

    public InvasionMod(IEventBus modBus, ModContainer container) {
        INSTANCE = this;
        // Read registration-affecting settings (notably debug mode) before the
        // registry events. The config writer is deferred until common setup,
        // because its generated entity section needs registered entity types.
        CONFIG.loadConfig(FMLPaths.CONFIGDIR.get().resolve("invasion_config.cfg").toFile());

        modBus.addListener(this::registerContent);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::addCreativeItems);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::startLevelTick);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(this::playerJoined);
        MinecraftForge.EVENT_BUS.addListener(this::playerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(this::playerSleepInBed);
        MinecraftForge.EVENT_BUS.addListener(this::blockPlaced);
        MinecraftForge.EVENT_BUS.addListener(this::livingDeath);
        MinecraftForge.EVENT_BUS.addListener(this::fuelBurnTime);

        BoundIMMobRegistry.bootstrap();
        IMMobFriendlyFireHandler.bootstrap();
        VanillaMobSpawnReplacement.bootstrap();
        NexusBoundMobLifecycle.bootstrap();
        VillagerResurrectionHandler.bootstrap();
        InfectionDeathHandler.bootstrap();
        IMCivilianTargetHandler.bootstrap();
        IronGolemTargetHandler.bootstrap();
        PlayerAllyTargetHandler.bootstrap();
        InfusedSwordChargeHandler.bootstrap();
        InfernalMobsCompatibility.bootstrap();
        ConfiguredModMobs.bootstrap();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            InvasionModClient.register(modBus, container);
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

    public static ResourceLocation id(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        InvNetwork.register();
        CONFIG.loadConfig(FMLPaths.CONFIGDIR.get().resolve("invasion_config.cfg").toFile());
    }

    private void registerContent(RegisterEvent event) {
        activeRegisterEvent = event;
        try {
            pendingRegistrations.stream()
                    .filter(entry -> !entry.registered
                            && event.getRegistryKey().equals(entry.registryKey))
                    .forEach(entry -> registerPending(event, entry));
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                InvBlocks.bootstrap();
            } else if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
                InvEntities.bootstrap();
                MutantMonstersCompatibility.bootstrapEntities();
                FriendsAndFoesCompatibility.registerEntity();
            } else if (event.getRegistryKey().equals(Registries.MOB_EFFECT)) {
                InvMobEffects.bootstrap();
            } else if (event.getRegistryKey().equals(Registries.ITEM)) {
                InvItems.bootstrap(event);
                FriendsAndFoesCompatibility.registerItem();
            } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
                com.invasion.block.InvBlockEntities.bootstrap();
            } else if (event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
                InvSounds.boostrap();
            } else if (event.getRegistryKey().equals(Registries.PARTICLE_TYPE)) {
                InvParticles.bootstrap();
            } else if (event.getRegistryKey().equals(Registries.MENU)) {
                InvScreenHandlers.bootstrap();
            } else if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
                InvItems.bootstrapCreativeTab(event);
            }
        } finally {
            activeRegisterEvent = null;
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        InvEntities.registerAttributes(event);
        MutantMonstersCompatibility.registerAttributes(event);
        FriendsAndFoesCompatibility.registerAttributes(event);
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

    private void startLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START
                && event.level instanceof ServerLevel world) {
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
        ConfiguredModMobs.refresh();
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
            InvNetwork.send(player, NexusHudPayload.hidden());
            WorldNexusStorage.of(world).onPlayerJoined(player);
        }
    }

    private void playerSleepInBed(PlayerSleepInBedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SERVER != null
                && java.util.stream.StreamSupport.stream(SERVER.getAllLevels().spliterator(), false)
                        .anyMatch(level -> WorldNexusStorage.of(level).hasStableNexus())) {
            event.setResult(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
            player.sendSystemMessage(Component.translatable("invmod.message.nexus.sleep_blocked")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    private void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer && SERVER != null) {
            SERVER.getAllLevels().forEach(level -> WorldNexusStorage.of(level).recordPlayerBlockPlacement());
        }
    }

    private void livingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob)
                || !(event.getSource().getEntity() instanceof ServerPlayer)
                || SERVER == null) {
            return;
        }
        boolean ranged = event.getSource().getDirectEntity()
                instanceof net.minecraft.world.entity.projectile.Projectile;
        SERVER.getAllLevels().forEach(level -> WorldNexusStorage.of(level).recordPlayerMobKill(ranged));
    }
    public <R, T extends R> T register(
            ResourceKey<? extends Registry<R>> registryKey, ResourceLocation id, T value) {
        if (activeRegisterEvent == null) {
            throw new IllegalStateException("Content was initialized outside its register event: " + id);
        }
        PendingRegistration<R, T> entry =
                new PendingRegistration<>(registryKey, id, value);
        pendingRegistrations.add(entry);
        if (activeRegisterEvent.getRegistryKey().equals(registryKey)) {
            registerPending(activeRegisterEvent, entry);
        }
        return value;
    }

    private static <R, T extends R> void registerPending(
            RegisterEvent event, PendingRegistration<R, T> entry) {
        event.register(entry.registryKey,
                helper -> helper.register(entry.id, entry.value));
        entry.registered = true;
    }

    private static final class PendingRegistration<R, T extends R> {
        private final ResourceKey<? extends Registry<R>> registryKey;
        private final ResourceLocation id;
        private final T value;
        private boolean registered;

        private PendingRegistration(
                ResourceKey<? extends Registry<R>> registryKey,
                ResourceLocation id, T value) {
            this.registryKey = registryKey;
            this.id = id;
            this.value = value;
        }
    }
}
