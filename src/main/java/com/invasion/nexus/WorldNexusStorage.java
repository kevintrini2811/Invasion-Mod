package com.invasion.nexus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.block.InvBlocks;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

public class WorldNexusStorage extends PersistentState {
    private static final Identifier ID = InvasionMod.id("nexus");

    public static Type<WorldNexusStorage> getType(ServerWorld world) {
        return new PersistentState.Type<>(() -> new WorldNexusStorage(world), (nbt, lookup) -> new WorldNexusStorage(world, nbt, lookup), DataFixTypes.LEVEL);
    }

    public static WorldNexusStorage of(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(getType(world), ID.toUnderscoreSeparatedString());
    }

    private final ServerWorld world;

    private final Map<UUID, Nexus> instances = new HashMap<>();

    private Optional<UUID> activeNexus = Optional.empty();

    private boolean resumed;
    private int cleanupTimer;

    private WorldNexusStorage(ServerWorld world) {
        this.world = world;
    }

    private WorldNexusStorage(ServerWorld world, NbtCompound nbt, WrapperLookup lookup) {
        this(world);
        resumed = true;
        if (nbt.containsUuid("activeNexus")) {
            activeNexus = Optional.of(nbt.getUuid("activeNexus"));
        }
        nbt.getList("nexuses", NbtElement.COMPOUND_TYPE).forEach(i -> {
            Nexus nexus = new Nexus(world, this, (NbtCompound)i, lookup);
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
            markDirty();
        }
    }

    private boolean tickCleanup(Nexus nexus) {
        if (cleanupTimer == 0 && !world.getBlockState(nexus.getOrigin()).isOf(InvBlocks.NEXUS_CORE)) {
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

    public synchronized boolean canActivate(Nexus nexus) {
        return activeNexus.map(instances::get).orElse(nexus) == nexus;
    }

    public synchronized boolean setActiveNexus(Nexus nexus) {
        if (!canActivate(nexus)) {
            return false;
        }
        activeNexus = Optional.ofNullable(nexus).map(Nexus::getUuid);
        markDirty();
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup lookup) {
        activeNexus.ifPresent(nexus -> {
            nbt.putUuid("activeNexus", nexus);
        });
        NbtList nexuses = new NbtList();
        instances.forEach((uuid, nexus) -> {
            nexuses.add(nexus.writeNbt(new NbtCompound(), lookup));
        });
        nbt.put("nexuses", nexuses);
        return nbt;
    }
}
