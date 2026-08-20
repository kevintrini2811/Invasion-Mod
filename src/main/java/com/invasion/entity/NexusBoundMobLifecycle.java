package com.invasion.entity;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;
import com.invasion.entity.ai.goal.MineBlockGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Enforces the lifetime shared by every hostile Nexus-bound IM mob. */
public final class NexusBoundMobLifecycle {
    private static final int MAX_DEATHS_PER_TICK = 10;
    private static final int MAX_STATIONARY_TICKS = 20 * 10;
    private static final Map<ServerLevel, CleanupQueue> CLEANUP_QUEUES =
            new WeakHashMap<>();
    private static final Map<LivingEntity, StationaryState> STATIONARY_STATES =
            new WeakHashMap<>();

    private NexusBoundMobLifecycle() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(NexusBoundMobLifecycle::tick);
    }

    /** Queues the loaded hostile mobs belonging to a failed Nexus. */
    public static void schedule(ServerLevel level, NexusAccess nexus) {
        for (Combatant<?> combatant : BoundIMMobRegistry.bound(level)) {
            if (combatant.getNexus() == nexus) {
                enqueue(level, combatant, nexus);
            }
        }
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Combatant<?> combatant : BoundIMMobRegistry.bound(level)) {
            LivingEntity living = combatant.asEntity();
            if (living instanceof IMWolfEntity
                    || !combatant.getNexusHandle().hasBinding()) {
                continue;
            }
            NexusAccess nexus = combatant.getNexus();
            if (nexus == null || nexus.isDiscarded() || !nexus.isActive()) {
                enqueue(level, combatant, nexus);
            } else {
                tickStationaryPathRecovery(combatant, living, nexus);
            }
        }
        drain(level);
    }

    private static void tickStationaryPathRecovery(
            Combatant<?> combatant, LivingEntity living, NexusAccess nexus) {
        if (!(living instanceof Mob mob)
                || living instanceof StationaryPathRecoveryExcluded
                || mob.getTarget() != null
                || mob instanceof PathfinderMob pathfinderMob
                        && MineBlockGoal.isMining(pathfinderMob)
                || living instanceof NexusEntity nexusMob
                        && nexusMob.getNavigatorNew().isWaitingForTask()) {
            STATIONARY_STATES.remove(living);
            return;
        }

        StationaryState state = STATIONARY_STATES.computeIfAbsent(
                living, ignored -> new StationaryState(living.blockPosition()));
        if (state.recoveryTarget != null
                && (mob.getNavigation().isDone()
                        || living.blockPosition().closerThan(
                                state.recoveryTarget, 2.0D))) {
            state.recoveryTarget = null;
            state.anchor = living.blockPosition();
            state.ticks = 0;
            return;
        }
        if (!living.blockPosition().equals(state.anchor)) {
            state.anchor = living.blockPosition();
            state.ticks = 0;
            return;
        }
        if (++state.ticks < MAX_STATIONARY_TICKS) {
            return;
        }

        state.anchor = living.blockPosition();
        state.ticks = 0;
        state.recoveryTarget = findAlternativePath(mob, nexus);
    }

    /** True while the normal Nexus goal must leave a recovery detour intact. */
    public static boolean isFollowingRecoveryPath(Mob mob) {
        StationaryState state = STATIONARY_STATES.get(mob);
        return state != null && state.recoveryTarget != null;
    }

    private static BlockPos findAlternativePath(Mob mob, NexusAccess nexus) {
        Vec3 origin = mob.position();
        Vec3 nexusDirection = Vec3.atCenterOf(nexus.getOrigin())
                .subtract(origin).multiply(1.0D, 0.0D, 1.0D).normalize();
        if (nexusDirection.lengthSqr() < 1.0E-6D) {
            nexusDirection = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideways = new Vec3(-nexusDirection.z, 0.0D, nexusDirection.x);

        mob.getNavigation().stop();
        for (int attempt = 0; attempt < 8; attempt++) {
            double sideDistance = (attempt % 2 == 0 ? 1.0D : -1.0D)
                    * (6.0D + mob.getRandom().nextDouble() * 6.0D);
            double forwardDistance = 2.0D + mob.getRandom().nextDouble() * 5.0D;
            Vec3 detour = origin.add(nexusDirection.scale(forwardDistance))
                    .add(sideways.scale(sideDistance));
            BlockPos detourPos = BlockPos.containing(detour);
            var path = mob.getNavigation().createPath(detourPos, 1);
            if (path != null && mob.getNavigation().moveTo(path, 1.0D)) {
                return detourPos;
            }
        }
        var directPath = mob.getNavigation().createPath(nexus.getOrigin(), 1);
        if (directPath != null) {
            mob.getNavigation().moveTo(directPath, 1.0D);
        }
        return null;
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
            freeze(living);
            queue.entries.addLast(new CleanupEntry(combatant, nexus));
        }
    }

    private static void freeze(LivingEntity living) {
        if (living instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setNoAi(true);
        }
        living.setDeltaMovement(Vec3.ZERO);
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
        combatant.setNexus(null);
        // A failed Nexus cleanup is not a combat death. Removing the entity
        // directly avoids loot, XP, death events, split/release behaviour and
        // twenty ticks of death animation for every remaining wave mob.
        living.discard();
    }

    private record CleanupEntry(
            Combatant<?> combatant, NexusAccess nexus) {
    }

    private static final class CleanupQueue {
        private final ArrayDeque<CleanupEntry> entries = new ArrayDeque<>();
        private final Set<Combatant<?>> pending = Collections.newSetFromMap(
                new IdentityHashMap<>());
    }

    private static final class StationaryState {
        private BlockPos anchor;
        private BlockPos recoveryTarget;
        private int ticks;

        private StationaryState(BlockPos anchor) {
            this.anchor = anchor;
        }
    }
}
