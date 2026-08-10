package com.invasion.entity;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Enforces the lifetime shared by every hostile Nexus-bound IM mob. */
public final class NexusBoundMobLifecycle {
    private static final int MAX_DEATHS_PER_TICK = 10;
    private static final Map<ServerLevel, CleanupQueue> CLEANUP_QUEUES =
            new WeakHashMap<>();

    private NexusBoundMobLifecycle() {
    }

    public static void bootstrap() {
        ServerTickEvents.END_LEVEL_TICK.register(NexusBoundMobLifecycle::tick);
    }

    /** Queues the loaded hostile mobs belonging to a failed Nexus. */
    public static void schedule(ServerLevel level, NexusAccess nexus) {
        for (Combatant<?> combatant : BoundIMMobRegistry.bound(level)) {
            if (combatant.getNexus() == nexus) {
                enqueue(level, combatant, nexus);
            }
        }
    }

    private static void tick(ServerLevel level) {
        for (Combatant<?> combatant : BoundIMMobRegistry.bound(level)) {
            LivingEntity living = combatant.asEntity();
            if (living instanceof IMWolfEntity
                    || !combatant.getNexusHandle().hasBinding()) {
                continue;
            }
            NexusAccess nexus = combatant.getNexus();
            if (nexus == null || nexus.isDiscarded() || !nexus.isActive()) {
                enqueue(level, combatant, nexus);
            }
        }
        drain(level);
    }

    private static void enqueue(
            ServerLevel level, Combatant<?> combatant, NexusAccess nexus) {
        LivingEntity living = combatant.asEntity();
        if (living instanceof IMWolfEntity
                || !living.isAlive()
                || living.isRemoved()) {
            return;
        }
        CleanupQueue queue = CLEANUP_QUEUES.computeIfAbsent(
                level, ignored -> new CleanupQueue());
        if (queue.pending.add(combatant)) {
            queue.entries.addLast(new CleanupEntry(combatant, nexus));
        }
    }

    private static void drain(ServerLevel level) {
        CleanupQueue queue = CLEANUP_QUEUES.get(level);
        if (queue == null) {
            return;
        }
        for (int i = 0; i < MAX_DEATHS_PER_TICK; i++) {
            CleanupEntry entry = queue.entries.pollFirst();
            if (entry == null) {
                break;
            }
            queue.pending.remove(entry.combatant);
            removeIfStillBound(level, entry);
        }
        if (queue.entries.isEmpty()) {
            CLEANUP_QUEUES.remove(level);
        }
    }

    private static void removeIfStillBound(
            ServerLevel level, CleanupEntry entry) {
        Combatant<?> combatant = entry.combatant;
        LivingEntity living = combatant.asEntity();
        if (!living.isAlive() || living.isRemoved()
                || living instanceof IMWolfEntity) {
            return;
        }

        NexusAccess currentNexus = combatant.getNexus();
        if (entry.nexus != null && currentNexus != entry.nexus
                || entry.nexus == null && currentNexus != null
                        && !currentNexus.isDiscarded()
                        && currentNexus.isActive()) {
            return;
        }
        if (living instanceof IMSlimeEntity slime) {
            slime.suppressSplitOnNexusDeath();
        } else if (living instanceof IMMagmaCubeEntity magmaCube) {
            magmaCube.suppressSplitOnNexusDeath();
        }
        combatant.setNexus(null);
        living.hurt(level.damageSources().magic(), living.getMaxHealth());
        living.kill(level);
    }

    private record CleanupEntry(
            Combatant<?> combatant, NexusAccess nexus) {
    }

    private static final class CleanupQueue {
        private final ArrayDeque<CleanupEntry> entries = new ArrayDeque<>();
        private final Set<Combatant<?>> pending = Collections.newSetFromMap(
                new IdentityHashMap<>());
    }
}
