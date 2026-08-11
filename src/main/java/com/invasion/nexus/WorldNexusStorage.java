package com.invasion.nexus;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;

public class WorldNexusStorage extends SavedData {
    private static final ResourceLocation ID = InvasionMod.id("nexus");

    public static WorldNexusStorage of(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(
                nbt -> new WorldNexusStorage(world, nbt),
                () -> new WorldNexusStorage(world), ID.toDebugFileName());
    }

    private final ServerLevel world;

    private final Map<UUID, Nexus> instances = new HashMap<>();
    private final List<LegacyNexus> legacyNexuses = new ArrayList<>();

    private Optional<UUID> activeNexus = Optional.empty();

    private boolean resumed;
    private int cleanupTimer;

    private WorldNexusStorage(ServerLevel world) {
        this.world = world;
    }

    private WorldNexusStorage(ServerLevel world, CompoundTag nbt) {
        this(world);
        resumed = true;
        Optional<UUID> savedActiveNexus = nbt.hasUUID("activeNexus")
                ? Optional.of(nbt.getUUID("activeNexus")) : Optional.empty();
        nbt.getList("nexuses", Tag.TAG_COMPOUND).forEach(i -> {
            CompoundTag compound = (CompoundTag)i;
            if (!compound.contains("phaseToken")) {
                legacyNexuses.add(new LegacyNexus(
                        BlockPos.of(compound.getLong("pos")),
                        Math.max(1, compound.getInt("currentWave")),
                        compound.getBoolean("activated")
                                && Mode.forId(compound.getInt("mode")).isActive()));
            } else {
                Nexus nexus = new Nexus(world, this, compound, world.registryAccess());
                instances.put(nexus.getUuid(), nexus);
            }
        });
        activeNexus = savedActiveNexus.filter(instances::containsKey);
    }

    public synchronized void tick() {
        migrateLegacyNexuses();
        cleanupTimer = (cleanupTimer + 1) % 40;
        int previousSize = instances.size();
        instances.values().removeIf(nexus -> {
            if (tickCleanup(nexus)) {
                return true;
            }
            nexus.tickInventory();
            nexus.getAttackerAI().tick();
            if (resumed) {
                nexus.onLoaded();
            }
            nexus.tick();
            return nexus.isDiscarded();
        });
        resumed = false;

        // Aktiven Nexus nur vergessen, wenn er wirklich nicht mehr existiert
        Optional<UUID> previousActiveNexus = activeNexus;
        activeNexus = activeNexus.filter(nexusId -> instances.containsKey(nexusId));
        if (instances.size() != previousSize
                || !activeNexus.equals(previousActiveNexus)) {
            setDirty();
        }
    }

    private void migrateLegacyNexuses() {
        Iterator<LegacyNexus> iterator = legacyNexuses.iterator();
        while (iterator.hasNext()) {
            LegacyNexus legacy = iterator.next();
            if (!world.hasChunkAt(legacy.pos())) continue;
            if (!world.getBlockState(legacy.pos()).is(InvBlocks.NEXUS_CORE)) {
                InvasionMod.LOGGER.warn("Skipping legacy Nexus migration at {} because the Nexus block is missing", legacy.pos());
                iterator.remove();
                setDirty();
                continue;
            }
            world.setBlockAndUpdate(legacy.pos(), Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(legacy.pos(), InvBlocks.NEXUS_CORE.defaultBlockState());
            if (world.getBlockEntity(legacy.pos()) instanceof NexusBlockEntity blockEntity) {
                Nexus replacement = (Nexus)blockEntity.getNexus();
                if (legacy.active() && !replacement.start(legacy.wave())) {
                    InvasionMod.LOGGER.error("Could not restart migrated Nexus at wave {} at {}", legacy.wave(), legacy.pos());
                } else {
                    InvasionMod.LOGGER.info("Replaced legacy Nexus at {}{}", legacy.pos(),
                            legacy.active() ? " and restarted wave " + legacy.wave() : "");
                }
            }
            iterator.remove();
            setDirty();
        }
    }

    private record LegacyNexus(BlockPos pos, int wave, boolean active) {
    }

    private boolean tickCleanup(Nexus nexus) {
        if (cleanupTimer == 0 && !world.getBlockState(nexus.getOrigin()).is(InvBlocks.NEXUS_CORE)) {
            nexus.stop(true);
            InvasionMod.LOGGER.warn("Stranded Nexus entity trying to delete itself...");
            return true;
        }
        return false;
    }


    public synchronized Nexus getOrCreate(UUID nexusId, BlockPos pos) {
        Nexus nexus = instances.get(nexusId);
        if (nexus == null) {
            nexus = new Nexus(world, this, nexusId, pos);
            instances.put(nexusId, nexus);
            setDirty();
        }
        return nexus;
    }

    public synchronized void destroyNexus(UUID nexusId) {
        @Nullable
        Nexus nexus = instances.remove(nexusId);
        if (nexus != null) {
            nexus.stop(true);
            setDirty();
        }
    }

    public synchronized NexusAccess getNexus(UUID nexusId) {
        return instances.get(nexusId);
    }

    public synchronized Optional<? extends ControllableNexusAccess> getNexus() {
        return activeNexus.map(instances::get);
    }

    public synchronized boolean hasStableNexus() {
        return getNexus().map(nexus -> nexus.getMode() == Mode.CONTINUOUS).orElse(false);
    }

    public synchronized void recordPlayerBlockPlacement() {
        activeNexus.map(instances::get).filter(Nexus::isActive)
                .ifPresent(Nexus::recordPlayerBlockPlacement);
    }

    public synchronized void recordPlayerMobKill(boolean ranged) {
        activeNexus.map(instances::get).filter(Nexus::isActive)
                .ifPresent(nexus -> nexus.recordPlayerMobKill(ranged));
    }

    public synchronized Optional<? extends ControllableNexusAccess> getNearestNexus(BlockPos pos) {
        return instances.values().stream()
                .min(java.util.Comparator.comparingDouble(
                        nexus -> nexus.getOrigin().distSqr(pos)));
    }

    public synchronized Optional<? extends ControllableNexusAccess> recoverNearestLoadedNexus(BlockPos pos) {
        Nexus nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int centerX = pos.getX() >> 4;
        int centerZ = pos.getZ() >> 4;
        for (int chunkX = centerX - 8; chunkX <= centerX + 8; chunkX++) {
            for (int chunkZ = centerZ - 8; chunkZ <= centerZ + 8; chunkZ++) {
                var chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof NexusBlockEntity nexusBlockEntity)) continue;
                    Nexus candidate = (Nexus)nexusBlockEntity.getNexus();
                    double distance = candidate.getOrigin().distSqr(pos);
                    if (distance < nearestDistance) {
                        nearest = candidate;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    public synchronized void onPlayerJoined(ServerPlayer player) {
        instances.values().forEach(nexus -> nexus.onPlayerJoined(player));
    }

    public synchronized boolean canActivate(Nexus nexus) {
        return activeNexus.map(instances::get).orElse(nexus) == nexus;
    }

    public synchronized boolean setActiveNexus(Nexus nexus) {
        if (!canActivate(nexus)) {
            return false;
        }
        Optional<UUID> newActiveNexus = Optional.ofNullable(nexus)
                .map(Nexus::getUuid);
        if (!newActiveNexus.equals(activeNexus)) {
            activeNexus = newActiveNexus;
            setDirty();
        }
        return true;
    }

    synchronized void clearActiveNexus(Nexus nexus) {
        if (activeNexus.filter(nexus.getUuid()::equals).isPresent()) {
            activeNexus = Optional.empty();
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        activeNexus.ifPresent(nexus -> {
            nbt.putUUID("activeNexus", nexus);
        });
        ListTag nexuses = new ListTag();
        instances.forEach((uuid, nexus) -> {
            nexuses.add(nexus.writeNbt(new CompoundTag(), world.registryAccess()));
        });
        legacyNexuses.forEach(legacy -> {
            CompoundTag pending = new CompoundTag();
            pending.putLong("pos", legacy.pos().asLong());
            pending.putInt("currentWave", legacy.wave());
            pending.putBoolean("activated", legacy.active());
            pending.putInt("mode", legacy.active() ? Mode.STARTED.ordinal() : Mode.STOPPED.ordinal());
            nexuses.add(pending);
        });
        nbt.put("nexuses", nexuses);
        return nbt;
    }
}
