package com.invasion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Cleans up players that somehow get away
 *
 * If the player is not in the world at the time the nexus is destroyed/closed
 * they get sent to the hunter to be killed once they return.
 */
public class BountyHunter extends SavedData {
    private static final int TICK_RATE = 3500;
    private static final Codec<List<UUID>> DEATH_LIST_CODEC = UUIDUtil.AUTHLIB_CODEC.listOf();
    private static final ResourceLocation ID = InvasionMod.id("nexus_bounty_hunter");

    public static BountyHunter of(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(
                nbt -> new BountyHunter(world, nbt),
                () -> new BountyHunter(world), ID.toDebugFileName());
    }

    private final List<UUID> players = new ArrayList<>();
    private long time;

    private final ServerLevel world;

    private BountyHunter(ServerLevel world) {
        this.world = world;
    }

    private BountyHunter(ServerLevel world, CompoundTag nbt) {
        this(world);
        DEATH_LIST_CODEC.decode(NbtOps.INSTANCE, nbt.get("players"))
                .result().map(Pair::getFirst).ifPresent(players::addAll);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        DEATH_LIST_CODEC.encodeStart(NbtOps.INSTANCE, players)
                .result().ifPresent(data -> nbt.put("players", data));
        return nbt;
    }

    public void tick() {
        if (players.isEmpty()) {
            return;
        }

        setDirty();

        if (++time % TICK_RATE == 0) {
            for (UUID id : players) {
                Player player = world.getPlayerByUUID(id);
                if (player != null) {
                    players.remove(id);
                    if (!player.isCreative()) {
                        player.hurt(world.damageSources().magic(), 500);
                        player.setHealth(1);
                        world.getServer().getPlayerList().broadcastAll(
                                new ClientboundSystemChatPacket(
                                        Component.literal("Nexus energies caught up to ")
                                                .append(player.getDisplayName()),
                                        false));
                    }
                    setDirty();
                }
            }
        }
    }

    public void add(UUID playerId) {
        players.add(playerId);
        setDirty();
    }

}
