package com.invasion.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.invasion.nexus.Combatant;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Gives every hostile IM mob the shared civilian and raider target set. */
public final class IMCivilianTargetHandler {
    private static final double TARGET_RANGE = 32.0D;
    private static final int SEARCH_INTERVAL = 20;
    private static final int FAILED_SEARCH_BACKOFF = 40;
    private static final Map<ServerLevel, CivilianIndex> LEVELS =
            new WeakHashMap<>();
    private static final Map<Mob, Long> NEXT_SEARCH = new WeakHashMap<>();

    private IMCivilianTargetHandler() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(IMCivilianTargetHandler::onJoin);
        NeoForge.EVENT_BUS.addListener(IMCivilianTargetHandler::onLeave);
        NeoForge.EVENT_BUS.addListener(IMCivilianTargetHandler::tick);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getEntity() instanceof LivingEntity living
                && isCivilian(living)) {
            LEVELS.computeIfAbsent(level, ignored -> new CivilianIndex())
                    .add(living);
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        CivilianIndex index = LEVELS.get(level);
        if (index != null) {
            index.remove(living);
            if (index.isEmpty()) {
                LEVELS.remove(level);
            }
        }
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        CivilianIndex index = LEVELS.get(level);
        if (index == null || index.isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime % SEARCH_INTERVAL == 0L) {
            index.rebuild();
            if (index.isEmpty()) {
                LEVELS.remove(level);
                return;
            }
        }
        List<Combatant<?>> attackers = BoundIMMobRegistry.loaded(level);
        if (attackers.isEmpty()) {
            return;
        }
        for (Combatant<?> combatant : attackers) {
            LivingEntity entity = combatant.asEntity();
            if (!(entity instanceof Mob mob)
                    || entity instanceof IMWolfEntity
                    || !mob.isAlive()
                    || mob.getTarget() != null && mob.getTarget().isAlive()
                    || Math.floorMod(gameTime + mob.getId(),
                            SEARCH_INTERVAL) != 0L
                    || NEXT_SEARCH.getOrDefault(mob, 0L) > gameTime) {
                continue;
            }
            LivingEntity target = index.nearestAttackable(mob);
            if (target == null) {
                NEXT_SEARCH.put(mob, gameTime + FAILED_SEARCH_BACKOFF);
            } else {
                NEXT_SEARCH.remove(mob);
                mob.setTarget(target);
            }
        }
    }

    private static boolean isCivilian(LivingEntity entity) {
        return entity instanceof AbstractVillager
                || entity instanceof AbstractPiglin
                || entity instanceof Pig
                || entity instanceof Hoglin
                || entity instanceof Pillager
                || entity instanceof Vindicator
                || entity instanceof Evoker
                || entity instanceof Ravager
                || entity instanceof Vex;
    }

    private static long chunkKey(int x, int z) {
        return ((long)x << 32) ^ (z & 0xffffffffL);
    }

    private static final class CivilianIndex {
        private final Set<LivingEntity> loaded = Collections.newSetFromMap(
                new IdentityHashMap<>());
        private final Map<Long, List<LivingEntity>> byChunk = new HashMap<>();

        private void add(LivingEntity entity) {
            loaded.add(entity);
            byChunk.computeIfAbsent(chunkKey(
                    entity.chunkPosition().x,
                    entity.chunkPosition().z),
                    ignored -> new ArrayList<>()).add(entity);
        }

        private void remove(LivingEntity entity) {
            loaded.remove(entity);
        }

        private boolean isEmpty() {
            return loaded.isEmpty();
        }

        private void rebuild() {
            loaded.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
            byChunk.clear();
            for (LivingEntity entity : loaded) {
                byChunk.computeIfAbsent(chunkKey(
                        entity.chunkPosition().x,
                        entity.chunkPosition().z),
                        ignored -> new ArrayList<>()).add(entity);
            }
        }

        private LivingEntity nearestAttackable(Mob mob) {
            int minChunkX = Mth.floor((mob.getX() - TARGET_RANGE) / 16.0D);
            int maxChunkX = Mth.floor((mob.getX() + TARGET_RANGE) / 16.0D);
            int minChunkZ = Mth.floor((mob.getZ() - TARGET_RANGE) / 16.0D);
            int maxChunkZ = Mth.floor((mob.getZ() + TARGET_RANGE) / 16.0D);
            double maxDistance = TARGET_RANGE * TARGET_RANGE;
            LivingEntity nearest = null;
            double nearestDistance = maxDistance;
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    for (LivingEntity candidate : byChunk.getOrDefault(
                            chunkKey(x, z), List.of())) {
                        double distance = mob.distanceToSqr(candidate);
                        if (distance <= nearestDistance
                                && candidate.isAlive()
                                && !candidate.isRemoved()
                                && mob.canAttack(candidate)) {
                            nearest = candidate;
                            nearestDistance = distance;
                        }
                    }
                }
            }
            return nearest;
        }
    }
}
