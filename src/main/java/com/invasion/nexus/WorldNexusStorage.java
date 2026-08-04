package com.invasion.nexus;

import java.util.HashMap;
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
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.block.InvBlocks;

public class WorldNexusStorage extends SavedData {
    private static final ResourceLocation ID = InvasionMod.id("nexus");

    public static Factory<WorldNexusStorage> getType(ServerLevel world) {
        return new SavedData.Factory<>(
                () -> new WorldNexusStorage(world),
                (nbt, lookup) -> new WorldNexusStorage(world, nbt, lookup),
                DataFixTypes.LEVEL);
    }

    public static WorldNexusStorage of(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(getType(world), ID.toDebugFileName());
    }

    private final ServerLevel world;

    private final Map<UUID, Nexus> instances = new HashMap<>();

    private Optional<UUID> activeNexus = Optional.empty();

    private boolean resumed;
    private int cleanupTimer;

    private WorldNexusStorage(ServerLevel world) {
        this.world = world;
    }

    private WorldNexusStorage(ServerLevel world, CompoundTag nbt, Provider lookup) {
        this(world);
        resumed = true;
        if (nbt.hasUUID("activeNexus")) {
            activeNexus = Optional.of(nbt.getUUID("activeNexus"));
        }
        nbt.getList("nexuses", Tag.TAG_COMPOUND).forEach(i -> {
            Nexus nexus = new Nexus(world, this, (CompoundTag)i, lookup);
            instances.put(nexus.getUuid(), nexus);
        });
    }

    public synchronized void tick() {
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

    public synchronized Optional<? extends ControllableNexusAccess> getNearestNexus(BlockPos pos) {
        return instances.values().stream()
                .min(java.util.Comparator.comparingDouble(
                        nexus -> nexus.getOrigin().distSqr(pos)));
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

    @Override
    public CompoundTag save(CompoundTag nbt, Provider lookup) {
        activeNexus.ifPresent(nexus -> {
            nbt.putUUID("activeNexus", nexus);
        });
        ListTag nexuses = new ListTag();
        instances.forEach((uuid, nexus) -> {
            nexuses.add(nexus.writeNbt(new CompoundTag(), lookup));
        });
        nbt.put("nexuses", nexuses);
        return nbt;
    }
}
