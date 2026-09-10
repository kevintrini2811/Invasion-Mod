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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.invasion.nexus.spawns.IMWaveSpawner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class NexusLifecycleTest {
    private static final BlockPos ORIGIN = new BlockPos(10, 64, -20);

    private ServerLevel world;
    private WorldNexusStorage storage;
    private Nexus nexus;
    private MockedConstruction<IMWaveSpawner> waveSpawners;

    @BeforeEach
    void setUp() {
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

    @AfterEach
    void tearDown() {
        waveSpawners.close();
    }

    @Test
    void debugActivationPauseResumeAndStopPreserveLifecycleInvariants() {
        assertTrue(nexus.startDebugMode());
        assertTrue(nexus.isActive());
        assertEquals(Mode.DEBUG, nexus.getMode());

        assertTrue(nexus.togglePause());
        assertTrue(nexus.isPaused());
        assertFalse(nexus.togglePause());
        assertFalse(nexus.isPaused());

        nexus.stop(false);

        assertFalse(nexus.isActive());
        assertFalse(nexus.isPaused());
        assertEquals(Mode.STOPPED, nexus.getMode());
        verify(storage).clearActiveNexus(nexus);
    }

    @Test
    void stopCleansStateLoadedFromAnActiveFailureSnapshot() {
        CompoundTag saved = nexus.writeNbt(new CompoundTag(), RegistryAccess.EMPTY);
        saved.putBoolean("activated", true);
        saved.putBoolean("paused", true);
        saved.putInt("activationTimer", 200);
        saved.putInt("currentWave", 7);
        saved.putInt("mode", Mode.STARTED.ordinal());

        Nexus restored = new Nexus(world, storage, saved, RegistryAccess.EMPTY);
        restored.stop(false);

        assertFalse(restored.isActive());
        assertFalse(restored.isPaused());
        assertFalse(restored.isActivating());
        assertEquals(0, restored.getCurrentWave());
        assertEquals(Mode.STOPPED, restored.getMode());
        verify(storage).clearActiveNexus(restored);
    }

    @Test
    void radiusAndLifecycleStateSurviveReload() {
        assertTrue(nexus.setSpawnRadius(80));
        assertTrue(nexus.startDebugMode());
        assertTrue(nexus.togglePause());

        CompoundTag saved = nexus.writeNbt(new CompoundTag(), RegistryAccess.EMPTY);
        Nexus restored = new Nexus(world, storage, saved, RegistryAccess.EMPTY);

        assertEquals(nexus.getUuid(), restored.getUuid());
        assertEquals(ORIGIN, restored.getOrigin());
        assertEquals(80, restored.getSpawnRadius());
        assertTrue(restored.isActive());
        assertTrue(restored.isPaused());
        assertEquals(Mode.DEBUG, restored.getMode());
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
    void worldStorageAllowsOnlyOneActiveNexusAndReleasesItOnDestroy() throws Exception {
        Constructor<WorldNexusStorage> constructor = WorldNexusStorage.class
                .getDeclaredConstructor(ServerLevel.class);
        constructor.setAccessible(true);
        WorldNexusStorage realStorage = constructor.newInstance(world);
        Nexus first = (Nexus) realStorage.getOrCreate(UUID.randomUUID(), ORIGIN);
        Nexus second = (Nexus) realStorage.getOrCreate(UUID.randomUUID(), ORIGIN.offset(100, 0, 0));

        assertTrue(realStorage.setActiveNexus(first));
        assertFalse(realStorage.setActiveNexus(second));

        realStorage.destroyNexus(first.getUuid());

        assertTrue(realStorage.getNexus().isEmpty());
        assertTrue(realStorage.setActiveNexus(second));
        assertSame(second, realStorage.getNexus().orElseThrow());
    }
}
