package com.invasion.nexus;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import com.invasion.nexus.ai.AttackerAI;

public interface NexusAccess {
    long BIND_EXPIRE_TIME = 300000L;
    long TICKS_PER_DAY = 24000L;
    long SUNSET_TIME = 12000L;
    long HALF_DAY_TIME = 14000L;
    long NIGHT_TIME = 16000L;

    int WAVE_DURATION = 240;

    UUID getUuid();

    boolean isDiscarded();

    BlockPos getOrigin();

    boolean isActivating();

    Mode getMode();

    int getLevel();

    int getSpawnRadius();

    int getCurrentWave();

    default int getProgressionLevel() {
        return getCurrentWave();
    }

    default int getEngineerVariantChancePercent() {
        return Math.max(2, Math.min(100, getProgressionLevel() + 1));
    }

    default int getChargedCreeperChancePercent() {
        return Math.clamp(getProgressionLevel() - 9, 0, 100);
    }

    default int getRandomEquipmentChancePercent() {
        return Math.clamp(getProgressionLevel(), 1, 100);
    }

    default int getBabyZombieChancePercent() {
        return Math.clamp(getProgressionLevel(), 1, 20);
    }

    default int getWitherSkeletonChancePercent() {
        return Math.clamp(getProgressionLevel() - 7, 0, 100);
    }

    default int getMobsLeftInWave() {
        return 0;
    }

    default int getMobsToKillInWave() {
        return 0;
    }

	default int getWavePhaseToken() { return 0; }

    default int getMobsLeftInPhase() {
        return getMobsLeftInWave();
    }

    default long getPhaseTimerTicks() {
        return 0L;
    }

    default int getHealthPercent() {
        return 0;
    }

    Level getWorld();

    AttackerAI getAttackerAI();

    Participants getParticipants();

    boolean isActive();

    List<Component> getStatus();

    void notifyCombatantRemoved(Combatant<?> combatant, Entity.RemovalReason reason);

    void damage(DamageSource source, int amount);
}
