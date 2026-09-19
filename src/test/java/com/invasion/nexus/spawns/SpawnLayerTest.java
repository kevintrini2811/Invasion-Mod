package com.invasion.nexus.spawns;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.invasion.compat.ConfiguredModMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.Test;

class SpawnLayerTest {
    @Test
    void roofAboveHeadSeparatesSurfaceAndUnderground() {
        ServerLevel world = mock(ServerLevel.class);
        Mob mob = mock(Mob.class);
        when(mob.getBbHeight()).thenReturn(1.8F);
        BlockPos pos = new BlockPos(1, 60, 2);
        // Roof at y=63, including transparent blocks in WORLD_SURFACE.
        when(world.getHeight(Heightmap.Types.WORLD_SURFACE, 1, 2)).thenReturn(64);
        assertTrue(SpawnLayer.BOTH.allows(world, pos, mob));
        assertTrue(SpawnLayer.UNDERGROUND.allows(world, pos, mob));
        assertFalse(SpawnLayer.SURFACE.allows(world, pos, mob));
        when(world.getHeight(Heightmap.Types.WORLD_SURFACE, 1, 2)).thenReturn(60);
        assertTrue(SpawnLayer.BOTH.allows(world, pos, mob));
        assertFalse(SpawnLayer.UNDERGROUND.allows(world, pos, mob));
        assertTrue(SpawnLayer.SURFACE.allows(world, pos, mob));
        assertTrue(SpawnLayer.MINERS_ONLY.allows(world, pos, mob));
    }

    @Test
    void undergroundMinersRespectConfiguredAbilityAndMobGriefing() {
        ServerLevel world = mock(ServerLevel.class, RETURNS_DEEP_STUBS);
        Mob mob = mock(Mob.class);
        when(world.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0)).thenReturn(80);
        when(world.getGameRules().get(GameRules.MOB_GRIEFING)).thenReturn(true);
        try (var configured = mockStatic(ConfiguredModMobs.class)) {
            configured.when(() -> ConfiguredModMobs.allowsMining(mob.getType(), false)).thenReturn(false);
            assertFalse(SpawnLayer.MINERS_ONLY.allows(world, BlockPos.ZERO, mob));
            configured.when(() -> ConfiguredModMobs.allowsMining(mob.getType(), false)).thenReturn(true);
            assertTrue(SpawnLayer.MINERS_ONLY.allows(world, BlockPos.ZERO, mob));
            when(world.getGameRules().get(GameRules.MOB_GRIEFING)).thenReturn(false);
            assertFalse(SpawnLayer.MINERS_ONLY.allows(world, BlockPos.ZERO, mob));
        }
    }

    @Test
    void restrictedContainerPreservesCaveAndSurfaceInSameColumnAfterSorting() {
        SpawnPoint cave = new SpawnPoint(new BlockPos(1, 30, 2), 0, SpawnType.HUMANOID);
        SpawnPoint surface = new SpawnPoint(new BlockPos(1, 70, 2), 0, SpawnType.HUMANOID);
        SpawnPointContainer points = new SpawnPointContainer(true);
        points.addSpawnPointXZ(cave);
        points.addSpawnPointXZ(surface);
        points.getRandomSpawnPoints(SpawnType.HUMANOID,
                net.minecraft.advancements.predicates.MinMaxBounds.Ints.between(-10, 10), 2);
        points.addSpawnPointXZ(surface);
        assertEquals(2, points.getNumberOfSpawnPoints(SpawnType.HUMANOID));
        SpawnPointContainer legacy = new SpawnPointContainer();
        legacy.addSpawnPointXZ(surface);
        legacy.addSpawnPointXZ(cave);
        assertEquals(1, legacy.getNumberOfSpawnPoints(SpawnType.HUMANOID));
        assertEquals(cave, legacy.getRandomSpawnPoint(SpawnType.HUMANOID));
    }

    @Test
    void configValuesRoundTripAndInvalidValuesUseBoth() {
        for (SpawnLayer layer : SpawnLayer.values()) assertEquals(layer, SpawnLayer.parse(layer.value()));
        assertEquals(SpawnLayer.BOTH, SpawnLayer.parse("unknown"));
        assertEquals(SpawnLayer.MINERS_ONLY, SpawnLayer.parse(" MINERS ONLY "));
    }
}
