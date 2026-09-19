package com.invasion.nexus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Constructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.invasion.nexus.spawns.IMWaveSpawner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class NexusLifecycleTest {
    private static final BlockPos ORIGIN = new BlockPos(10, 64, -20);

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private ServerLevel world;
    private WorldNexusStorage storage;
    private Nexus nexus;
    private MockedConstruction<IMWaveSpawner> waveSpawners;
    private MockedStatic<NexusChunkLoader> chunkLoader;

    @BeforeEach
    void setUp() {
        chunkLoader = org.mockito.Mockito.mockStatic(NexusChunkLoader.class);
        waveSpawners = mockConstruction(IMWaveSpawner.class, (spawner, context) -> {
            AtomicInteger radius = new AtomicInteger((Integer) context.arguments().get(1));
            when(spawner.getRadius()).thenAnswer(invocation -> radius.get());
            when(spawner.setRadius(any(Integer.class))).thenAnswer(invocation -> {
                int requested = Math.max(IMWaveSpawner.MIN_SPAWN_RADIUS, invocation.getArgument(0));
                return radius.getAndSet(requested) != requested;
            });
            when(spawner.writeNbt(any(CompoundTag.class), any())).thenAnswer(invocation -> {
                CompoundTag tag = invocation.getArgument(0);
                tag.putInt("spawnRadius", radius.get());
                return tag;
            });
            org.mockito.Mockito.doAnswer(invocation -> {
                radius.set(invocation.<CompoundTag>getArgument(0).getInt("spawnRadius"));
                return null;
            }).when(spawner).readNbt(any(CompoundTag.class), any());
        });
        world = mock(ServerLevel.class);
        storage = mock(WorldNexusStorage.class);
        when(storage.setActiveNexus(any())).thenReturn(true);
        when(world.getMinBuildHeight()).thenReturn(-64);
        when(world.getMaxBuildHeight()).thenReturn(320);
        when(world.getBlockState(any(BlockPos.class))).thenReturn(mock(BlockState.class));
        when(world.getEntitiesOfClass(eq(Player.class), any(AABB.class), any())).thenReturn(List.of());
        when(world.getEntitiesOfClass(eq(Mob.class), any(AABB.class), any())).thenReturn(List.of());
        nexus = new Nexus(world, storage, UUID.randomUUID(), ORIGIN);
    }

    @Test
    void skippedSpawnsReduceOutstandingTargetsWithoutAwardingKills() throws Exception {
        for (String name : List.of("phaseMobsLeft", "mobsLeftInWave", "mobsToKillInWave")) {
            var field = Nexus.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(nexus, 5);
        }
        nexus.notifySpawnsSkipped(2);
        assertEquals(3, nexus.getMobsLeftInPhase());
        assertEquals(3, nexus.getMobsLeftInWave());
        assertEquals(3, nexus.getMobsToKillInWave());
        for (String name : List.of("phaseKills", "nexusKills")) {
            var field = Nexus.class.getDeclaredField(name);
            field.setAccessible(true);
            assertEquals(0, field.getInt(nexus));
        }
        nexus.notifySpawnsSkipped(10);
        assertEquals(0, nexus.getMobsLeftInPhase());
        assertEquals(0, nexus.getMobsLeftInWave());
    }

    @AfterEach
    void tearDown() {
        waveSpawners.close();
        chunkLoader.close();
    }

    @Test
    void debugActivationPauseResumeAndStopPreserveLifecycleInvariants() {
        CompoundTag activeState = nexus.writeNbt(new CompoundTag(), RegistryAccess.EMPTY);
        activeState.putBoolean("activated", true);
        activeState.putInt("mode", Mode.DEBUG.ordinal());
        Nexus restored = new Nexus(world, storage, activeState, RegistryAccess.EMPTY);

        assertTrue(restored.isActive());
        assertEquals(Mode.DEBUG, restored.getMode());

        assertTrue(restored.togglePause());
        assertTrue(restored.isPaused());
        assertFalse(restored.togglePause());
        assertFalse(restored.isPaused());
    }

    @Test
    void stopCleansStateLoadedFromAnActiveFailureSnapshot() {
        CompoundTag saved = nexus.writeNbt(new CompoundTag(), RegistryAccess.EMPTY);
        saved.putBoolean("activated", true);
        saved.putBoolean("paused", true);
        saved.putInt("activationTimer", 200);
        saved.putInt("currentWave", 7);
        saved.putInt("mode", Mode.STOPPED.ordinal());

        Nexus restored = new Nexus(world, storage, saved, RegistryAccess.EMPTY);
        int spawnRadius = restored.getSpawnRadius();
        restored.stop(false);

        assertFalse(restored.isActive());
        assertFalse(restored.isPaused());
        assertFalse(restored.isActivating());
        assertEquals(0, restored.getCurrentWave());
        assertEquals(Mode.STOPPED, restored.getMode());
        verify(storage).clearActiveNexus(restored);
        chunkLoader.verify(() -> NexusChunkLoader.force(
                world, restored.getUuid(), ORIGIN, spawnRadius, false));
    }

    @Test
    void radiusAndLifecycleStateSurviveReload() {
        assertTrue(nexus.setSpawnRadius(80));

        CompoundTag saved = nexus.writeNbt(new CompoundTag(), RegistryAccess.EMPTY);
        Nexus restored = new Nexus(world, storage, saved, RegistryAccess.EMPTY);

        assertEquals(nexus.getUuid(), restored.getUuid());
        assertEquals(ORIGIN, restored.getOrigin());
        assertEquals(80, restored.getSpawnRadius());
        assertFalse(restored.isActive());
        assertFalse(restored.isPaused());
        assertEquals(Mode.STOPPED, restored.getMode());
    }

    @Test
    void participantIdentitySurvivesReloadAndReconnects() {
        UUID playerId = UUID.randomUUID();
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(playerId);
        when(player.isCreative()).thenReturn(false);
        when(player.getDisplayName()).thenReturn(Component.literal("Player"));
        when(world.getEntitiesOfClass(eq(Player.class), any(AABB.class), any())).thenReturn(List.of(player));

        Participants participants = nexus.getParticipants();
        participants.bindPlayers(new AABB(ORIGIN));
        CompoundTag saved = participants.writeNbt(new CompoundTag(), null);

        Participants restored = new Participants(nexus);
        restored.readNbt(saved, null);

        assertTrue(restored.reconnect(player));
        when(world.getPlayerByUUID(playerId)).thenReturn(player);
        clearInvocations(player);
        restored.sendMessage(net.minecraft.network.chat.Component.literal("test"));
        verify(player).sendSystemMessage(any());
    }

    @Test
    void participantsRecoverValidEntriesFromMixedPersistedEntries() {
        UUID playerId = UUID.randomUUID();
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(playerId);
        when(player.isCreative()).thenReturn(false);
        when(player.getDisplayName()).thenReturn(Component.literal("Player"));
        when(world.getEntitiesOfClass(eq(Player.class), any(AABB.class), any())).thenReturn(List.of(player));

        Participants participants = nexus.getParticipants();
        participants.bindPlayers(new AABB(ORIGIN));
        CompoundTag valid = (CompoundTag) participants.writeNbt(new CompoundTag(), null)
                .getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND).get(0);
        ListTag entries = new ListTag();
        entries.add(new CompoundTag());
        entries.add(valid);
        CompoundTag saved = new CompoundTag();
        saved.put("entries", entries);

        Participants restored = new Participants(nexus);
        restored.readNbt(saved, null);

        assertTrue(restored.reconnect(player));
    }

    @Test
    void worldStorageAllowsOnlyOneActiveNexusAndReleasesItOnDestroy() throws Exception {
        Constructor<WorldNexusStorage> constructor = WorldNexusStorage.class
                .getDeclaredConstructor(ServerLevel.class);
        constructor.setAccessible(true);
        WorldNexusStorage realStorage = constructor.newInstance(world);
        Nexus first = (Nexus) realStorage.getOrCreate(UUID.randomUUID(), ORIGIN);
        Nexus second = (Nexus) realStorage.getOrCreate(UUID.randomUUID(), ORIGIN.offset(100, 0, 0));

        assertTrue(realStorage.setActiveNexus(first));
        assertFalse(realStorage.setActiveNexus(second));

        realStorage.clearActiveNexus(first);

        assertTrue(realStorage.getNexus().isEmpty());
        assertTrue(realStorage.setActiveNexus(second));
        assertSame(second, realStorage.getNexus().orElseThrow());
    }

    @Test
    void worldStorageRecoversValidNexusesFromMixedPersistedEntries() throws Exception {
        UUID validId = nexus.getUuid();
        CompoundTag valid = nexus.writeNbt(new CompoundTag(), RegistryAccess.EMPTY);
        CompoundTag invalid = valid.copy();
        invalid.remove("uuid");

        ListTag entries = new ListTag();
        entries.add(invalid);
        entries.add(valid);
        CompoundTag saved = new CompoundTag();
        saved.put("nexuses", entries);
        saved.putUUID("activeNexus", validId);

        WorldNexusStorage restored = loadStorage(saved);

        assertEquals(validId, restored.getNexus(validId).getUuid());
        assertEquals(validId, restored.getNexus().orElseThrow().getUuid());
    }

    @Test
    void worldStorageDropsPersistedActiveIdentityWithoutMatchingNexus() throws Exception {
        CompoundTag saved = new CompoundTag();
        saved.put("nexuses", new ListTag());
        saved.putUUID("activeNexus", UUID.randomUUID());

        WorldNexusStorage restored = loadStorage(saved);

        assertTrue(restored.getNexus().isEmpty());
    }

    private WorldNexusStorage loadStorage(CompoundTag saved) throws Exception {
        Constructor<WorldNexusStorage> constructor = WorldNexusStorage.class
                .getDeclaredConstructor(ServerLevel.class, CompoundTag.class);
        constructor.setAccessible(true);
        return constructor.newInstance(world, saved);
    }
}
