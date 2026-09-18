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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
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
        ServerLevel level = mock(ServerLevel.class);
        IMSilverfishEntity silverfish = mock(IMSilverfishEntity.class);
        doReturn(InvEntities.SILVERFISH).when(silverfish).getType();
        var listener = VanillaMobSpawnReplacement.class.getDeclaredMethod(
                "queueVanillaMob", EntityJoinLevelEvent.class);
        listener.setAccessible(true);

        InvasionMod.getConfig().enableSilverfish = false;
        EntityJoinLevelEvent spawn = new EntityJoinLevelEvent(silverfish, level, false);
        listener.invoke(null, spawn);
        assertTrue(spawn.isCanceled());

        EntityJoinLevelEvent loaded = new EntityJoinLevelEvent(silverfish, level, true);
        listener.invoke(null, loaded);
        assertFalse(loaded.isCanceled());

        InvasionMod.getConfig().enableSilverfish = true;
        EntityJoinLevelEvent enabled = new EntityJoinLevelEvent(silverfish, level, false);
        listener.invoke(null, enabled);
        assertFalse(enabled.isCanceled());
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
