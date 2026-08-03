package com.invasion.nexus;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public interface IHasNexus {

    Handle getNexusHandle();

    @Nullable
    default NexusAccess getNexus() {
        return getNexusHandle().get();
    }

    default void setNexus(@Nullable NexusAccess nexus) {
        getNexusHandle().set(nexus);
    }

    default boolean hasNexus() {
        return getNexus() != null;
    }

    double findDistanceToNexus();

    @Nullable
    static NexusAccess findNexus(Level world, BlockPos center) {
        for (BlockPos pos : BlockPos.withinManhattan(center, 8, 5, 8)) {
            if (world.getBlockState(pos).is(InvBlocks.NEXUS_CORE)) {
                if (world.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
                    return nexus.getNexus();
                }
            }
        }
        return null;
    }

    public final class Handle {
        @Nullable
        private UUID nexusId;
        @Nullable
        private GlobalPos globalPos;
        @Nullable
        private NexusAccess nexus;

        private final Supplier<Level> worldGetter;

        public Handle(Supplier<Level> worldGetter) {
            this.worldGetter = worldGetter;
        }

        public @Nullable NexusAccess get() {
            if (nexusId != null
                    && globalPos != null
                    && nexus == null
                    && worldGetter.get() instanceof ServerLevel sw
                    && sw.getServer().getLevel(globalPos.dimension()) instanceof ServerLevel world) {
                nexus = WorldNexusStorage.of(world).getNexus(nexusId);
                if (nexus == null) {
                    set(null);
                }
            }
            return nexus;
        }

        /** True while saved Nexus identity still exists, even if it cannot resolve. */
        public boolean hasBinding() {
            return nexusId != null;
        }

        public void set(@Nullable NexusAccess nexus) {
            nexusId = nexus == null ? null : nexus.getUuid();
            globalPos = nexus == null ? null : GlobalPos.of(nexus.getWorld().dimension(), nexus.getOrigin());
            this.nexus = nexus;
        }

        public void readNbt(CompoundTag nbt) {
            nexus = null;
            globalPos = nbt.contains("globalPos")
                    ? GlobalPos.CODEC.decode(NbtOps.INSTANCE, nbt.get("globalPos"))
                            .result().map(Pair::getFirst).orElse(null)
                    : null;
            nexusId = nbt.hasUUID("nexusId") ? nbt.getUUID("nexusId") : null;
        }

        public Optional<GlobalPos> getPos() {
            return Optional.ofNullable(globalPos);
        }

        public void writeNbt(CompoundTag nbt) {
            if (nexusId != null) {
                nbt.putUUID("nexusId", nexusId);
            }
            if (globalPos != null) {
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, globalPos)
                        .result().ifPresent(value -> nbt.put("globalPos", value));
            }
        }
    }
}
