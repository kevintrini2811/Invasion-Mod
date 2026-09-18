package com.invasion.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.invasion.InvasionConfig;
import com.invasion.InvasionMod;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.wave.BudgetWavePlan;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.core.BlockPos;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SilverfishConfigTest {
    private final boolean originalEnabled = InvasionMod.getConfig().enableSilverfish;

    @AfterEach
    void restoreConfig() {
        InvasionMod.getConfig().enableSilverfish = originalEnabled;
    }

    @Test
    void defaultsToEnabledAndReloadsPersistedToggle(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("invasion_config.cfg");
        InvasionConfig config = new InvasionConfig();
        config.loadConfig(file.toFile());
        assertTrue(config.enableSilverfish);
        assertEquals("true", config.getProperty("enable-silverfish", "missing"));

        Files.writeString(file, "enable-silverfish=false\n");
        config.loadConfig(file.toFile());
        assertFalse(config.enableSilverfish);
        config.loadConfig(file.toFile());
        assertFalse(config.enableSilverfish);

        Files.writeString(file, "enable-silverfish=true\n");
        config.loadConfig(file.toFile());
        assertTrue(config.enableSilverfish);
    }

    @Test
    void blocksNewSilverfishButPreservesSavedSilverfish() throws Exception {
        PersistentEntitySectionManager<Entity> manager = new PersistentEntitySectionManager<>(
                Entity.class, mock(LevelCallback.class), mock(EntityPersistentStorage.class));
        IMSilverfishEntity silverfish = mock(IMSilverfishEntity.class);
        UUID id = UUID.randomUUID();
        when(silverfish.getUUID()).thenReturn(id);
        when(silverfish.blockPosition()).thenReturn(BlockPos.ZERO);

        InvasionMod.getConfig().enableSilverfish = false;
        assertFalse(manager.addNewEntity(silverfish));
        assertFalse(manager.isLoaded(id));
        manager.addLegacyChunkEntities(Stream.of(silverfish));
        assertTrue(manager.isLoaded(id));

        InvasionMod.getConfig().enableSilverfish = true;
        IMSilverfishEntity enabled = mock(IMSilverfishEntity.class);
        when(enabled.getUUID()).thenReturn(UUID.randomUUID());
        when(enabled.blockPosition()).thenReturn(BlockPos.ZERO);
        assertTrue(manager.addNewEntity(enabled));
    }

    @Test
    void disablesInfectionEvenAtMaximumProgressionWithBonus() throws Exception {
        Mob host = mock(Mob.class);
        NexusAccess nexus = mock(NexusAccess.class);
        RandomSource random = mock(RandomSource.class);
        when(nexus.getProgressionLevel()).thenReturn(100);
        when(host.getRandom()).thenReturn(random);
        var infection = EntityConstruct.class.getDeclaredMethod(
                "applyWaveInfection", Mob.class, NexusAccess.class, int.class);
        infection.setAccessible(true);

        InvasionMod.getConfig().enableSilverfish = false;
        infection.invoke(null, host, nexus, BudgetWavePlan.RULE_INFECTED_BONUS);
        verify(host, never()).addTag(IMSilverfishEntity.INFECTED_TAG);
        verifyNoInteractions(random);

        InvasionMod.getConfig().enableSilverfish = true;
        infection.invoke(null, host, nexus, BudgetWavePlan.RULE_INFECTED_BONUS);
        verify(host).addTag(IMSilverfishEntity.INFECTED_TAG);
    }

    @Test
    void excludesSilverfishFromWaveAndRandomSelections() {
        InvasionMod.getConfig().enableSilverfish = false;
        assertFalse(BudgetWavePlan.isWaveSpawnAllowed(InvEntities.SILVERFISH));
        RandomSource random = RandomSource.create(1234L);
        for (int i = 0; i < 1000; i++) {
            assertNotEquals(InvEntities.SILVERFISH,
                    BudgetWavePlan.randomMobConstruct(random).entityType());
        }
        BudgetWavePlan plan = BudgetWavePlan.generate(100, 12, random);
        do {
            assertTrue(plan.currentPhase().purchases().stream()
                    .noneMatch(purchase -> purchase.type() == InvEntities.SILVERFISH));
        } while (plan.advance());

        InvasionMod.getConfig().enableSilverfish = true;
        assertTrue(BudgetWavePlan.isWaveSpawnAllowed(InvEntities.SILVERFISH));
    }
}
