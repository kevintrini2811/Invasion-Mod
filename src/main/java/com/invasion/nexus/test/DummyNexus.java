package com.invasion.nexus.test;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.ControllableNexusAccess;
import com.invasion.nexus.Mode;
import com.invasion.nexus.Participants;
import com.invasion.nexus.ai.AttackerAI;

public class DummyNexus implements ControllableNexusAccess {
    private Level world;

    private final UUID uuid = UUID.randomUUID();

    @Override
    public UUID getUuid() {
        return uuid;
    }

    public void setWorld(Level world) {
        this.world = world;
    }

    @Override
    public void notifyCombatantRemoved(Combatant<?> combatant, RemovalReason reason) {
    }

    @Override
    public void damage(DamageSource source, int amount) {
    }

    @Override
    public boolean isActivating() {
        return false;
    }

    @Override
    public Mode getMode() {
        return Mode.STOPPED;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public int getSpawnRadius() {
        return 45;
    }

    @Override
    public int getCurrentWave() {
        return 1;
    }

    @Override
    public BlockPos getOrigin() {
        return BlockPos.ZERO;
    }

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public AttackerAI getAttackerAI() {
        return null;
    }

    @Override
    public Participants getParticipants() {
        return null;
    }

    @Override
    public boolean start(int wave) {
        return true;
    }

    @Override
    public void stop(boolean killEnemies) {
    }

    @Override
    public boolean setSpawnRadius(int radius) {
        return true;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public List<Component> getStatus() {
        return List.of();
    }

    @Override
    public boolean isDiscarded() {
        return false;
    }
}