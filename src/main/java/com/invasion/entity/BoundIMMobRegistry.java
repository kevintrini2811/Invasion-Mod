package com.invasion.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;

/** Index of loaded IM combatants, avoiding repeated full-level entity scans. */
public final class BoundIMMobRegistry {
    private static final Map<ServerLevel, LevelEntries> LEVELS =
            new WeakHashMap<>();

    private BoundIMMobRegistry() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(BoundIMMobRegistry::onJoin);
        MinecraftForge.EVENT_BUS.addListener(BoundIMMobRegistry::onLeave);
    }

    public static synchronized void update(
            Entity entity, @Nullable NexusAccess nexus) {
        if (!(entity.level() instanceof ServerLevel level)
                || !(entity instanceof Combatant<?> combatant)) {
            return;
        }
        LevelEntries entries = LEVELS.computeIfAbsent(
                level, ignored -> new LevelEntries());
        entries.loaded.add(combatant);
        if (nexus == null) {
            entries.bound.remove(combatant);
        } else {
            entries.bound.add(combatant);
        }
    }

    public static synchronized List<Combatant<?>> loaded(ServerLevel level) {
        LevelEntries entries = LEVELS.get(level);
        return entries == null ? List.of() : List.copyOf(entries.loaded);
    }

    public static synchronized List<Combatant<?>> bound(ServerLevel level) {
        LevelEntries entries = LEVELS.get(level);
        return entries == null ? List.of() : List.copyOf(entries.bound);
    }

    public static List<Combatant<?>> activeBound(ServerLevel level) {
        List<Combatant<?>> result = new ArrayList<>();
        for (Combatant<?> combatant : bound(level)) {
            Entity entity = combatant.asEntity();
            NexusAccess nexus = combatant.getNexus();
            if (entity.isAlive() && !entity.isRemoved()
                    && nexus != null && !nexus.isDiscarded()
                    && nexus.isActive()) {
                result.add(combatant);
            }
        }
        return result;
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Combatant<?> combatant)) {
            return;
        }
        NexusAccess nexus = combatant.getNexus();
        if (nexus == null) {
            nexus = WorldNexusStorage.of(level).getNexus()
                    .filter(candidate -> candidate.isActive()
                            && !candidate.isDiscarded())
                    .orElse(null);
            if (nexus != null) {
                combatant.setNexus(nexus);
                return;
            }
        }
        update(event.getEntity(), nexus);
    }

    private static synchronized void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Combatant<?> combatant)) {
            return;
        }
        LevelEntries entries = LEVELS.get(level);
        if (entries != null) {
            entries.loaded.remove(combatant);
            entries.bound.remove(combatant);
            if (entries.loaded.isEmpty()) {
                LEVELS.remove(level);
            }
        }
    }

    private static final class LevelEntries {
        private final Set<Combatant<?>> loaded = Collections.newSetFromMap(
                new IdentityHashMap<>());
        private final Set<Combatant<?>> bound = Collections.newSetFromMap(
                new IdentityHashMap<>());
    }
}
