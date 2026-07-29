package com.invasion.nexus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.block.InvBlocks;

public class WorldNexusStorage extends SavedData {
    private static final Identifier ID = InvasionMod.id("nexus");

    public static SavedDataType<WorldNexusStorage> getType(ServerLevel world) {
        return new SavedDataType<>(
                ID,
                () -> new WorldNexusStorage(world),
                CompoundTag.CODEC.xmap(
                        tag -> new WorldNexusStorage(world, tag, world.registryAccess()),
                        storage -> storage.write(new CompoundTag(), world.registryAccess())
                ),
                DataFixTypes.LEVEL
        );
    }

    public static WorldNexusStorage of(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(getType(world));
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
        activeNexus = nbt.read("activeNexus", net.minecraft.core.UUIDUtil.CODEC);
        nbt.getListOrEmpty("nexuses").forEach(i -> {
            Nexus nexus = new Nexus(world, this, (CompoundTag)i, lookup);
            instances.put(nexus.getUuid(), nexus);
        });
    }

    public synchronized void tick() {
        cleanupTimer = (cleanupTimer + 1) % 40;
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
        activeNexus = activeNexus.filter(nexusId -> instances.containsKey(nexusId));


        if (!instances.isEmpty()) {
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
        return instances.computeIfAbsent(nexusId, id -> new Nexus(world, this, nexusId, pos));
    }

    public synchronized void destroyNexus(UUID nexusId) {
        @Nullable
        Nexus nexus = instances.remove(nexusId);
        if (nexus != null) {
            nexus.stop(true);
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
        activeNexus = Optional.ofNullable(nexus).map(Nexus::getUuid);
        setDirty();
        return true;
    }

    private CompoundTag write(CompoundTag nbt, Provider lookup) {
        activeNexus.ifPresent(nexus -> {
            nbt.store("activeNexus", net.minecraft.core.UUIDUtil.CODEC, nexus);
        });
        ListTag nexuses = new ListTag();
        instances.forEach((uuid, nexus) -> {
            nexuses.add(nexus.writeNbt(new CompoundTag(), lookup));
        });
        nbt.put("nexuses", nexuses);
        return nbt;
    }
}
