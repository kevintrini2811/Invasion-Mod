package com.invasion.nexus;

import java.util.List;
import java.util.UUID;

import com.invasion.nexus.ai.AttackerAI;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface NexusAccess {
    long BIND_EXPIRE_TIME = 300000L;
    long TICKS_PER_DAY = World.field_30969;//24000
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

    World getWorld();

    AttackerAI getAttackerAI();

    Participants getParticipants();

    boolean isActive();

    List<Text> getStatus();

    void notifyCombatantRemoved(Combatant<?> combatant, Entity.RemovalReason reason);

    void damage(DamageSource source, int amount);
}
