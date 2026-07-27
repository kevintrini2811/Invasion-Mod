package com.invasion.nexus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import com.invasion.BountyHunter;
import com.invasion.InvasionMod;

public class Participants {
    private static final Component POSSESSIVE_SUFFEX_A = Component.literal("'");
    private static final Component POSSESSIVE_SUFFEX_B = Component.literal("'s");

    private final Map<UUID, Entry> entries = new HashMap<>();

    private final Nexus nexus;

    public Participants(Nexus nexus) {
        this.nexus = nexus;
    }

    public void bindPlayers(AABB arena) {
        final long now = System.currentTimeMillis();
        for (Player player : nexus.getWorld().getEntitiesOfClass(Player.class, arena, EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
            entries.compute(player.getUUID(), (id, oldEntry) -> {
                if (oldEntry == null || now - oldEntry.time > NexusAccess.BIND_EXPIRE_TIME) {
                    Component message = Component.translatable("invmod.message.nexus.lifenowbound", pluralize(player.getDisplayName())).withStyle(ChatFormatting.DARK_GREEN);
                    sendMessage(message);
                    if (oldEntry == null) {
                        player.sendSystemMessage(message);
                    }
                    return new Entry(now, player.getUUID());
                }
                return oldEntry;
            });
        }
    }


    private Component pluralize(Component text) {
        return text.copy().append(text.getString().toLowerCase(Locale.ROOT).endsWith("s") ? POSSESSIVE_SUFFEX_A : POSSESSIVE_SUFFEX_B).withStyle(ChatFormatting.GREEN);
    }

    public void sendWarning(String translationKey, Object...params) {
        sendMessage(ChatFormatting.RED, translationKey, params);
    }

    public void sendNotice(String translationKey, Object...params) {
        sendMessage(ChatFormatting.DARK_GREEN, translationKey, params);
    }

    public void sendMessage(ChatFormatting color, String translationKey, Object...params) {
        sendMessage(Component.translatable(translationKey, params).withStyle(color));
    }

    public void sendMessage(Component message) {
        for (Entry entry : entries.values()) {
            Player player = entry.getEntity();
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }

    public void sendMessageIncludingNearby(Component message, AABB area) {
        Set<UUID> notifiedPlayers = new HashSet<>();
        for (Entry entry : entries.values()) {
            Player player = entry.getEntity();
            if (player != null) {
                player.sendSystemMessage(message);
                notifiedPlayers.add(player.getUUID());
            }
        }

        for (Player player : nexus.getWorld().getEntitiesOfClass(Player.class, area)) {
            if (notifiedPlayers.add(player.getUUID())) {
                player.sendSystemMessage(message);
            }
        }
    }

    public void playSoundForBoundPlayers(SoundEvent sound) {
        playSoundForBoundPlayers(sound, 1, 1);
    }

    public void playSoundForBoundPlayers(SoundEvent sound, float volume, float pitch) {
        for (Entry entry : entries.values()) {
            try {
                Player player = entry.getEntity();
                if (player != null) {
                    player.level().playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, volume, pitch);
                }
            } catch (Exception e) {
                InvasionMod.LOGGER.error("Problem while trying to play sound " + sound + " at player " + entry.id, e);
            }
        }
    }

    public void release() {
        long time = System.currentTimeMillis();
        for (Entry entry : entries.values()) {
            if (time - entry.time < NexusAccess.BIND_EXPIRE_TIME) {
                Player player = entry.getEntity();
                if (player != null) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.AMBIENT, 4, 1);
                    player.hurt(player.level().damageSources().magic(), 500);
                } else if (nexus.getWorld() instanceof ServerLevel sw) {
                    BountyHunter.of(sw).add(entry.id);
                }
            }
        }

        entries.clear();
    }

    public void readNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        entries.clear();
        compound.getListOrEmpty("entries").forEach(el -> {
            Entry entry = new Entry((CompoundTag)el);
            entries.put(entry.id, entry);
        });
    }

    public CompoundTag writeNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        ListTag entries = new ListTag();
        for (Entry entry : this.entries.values()) {
            entries.add(entry.writeNbt(new CompoundTag(), lookup));
        }
        compound.put("entries", entries);
        return compound;
    }

    public Component getParticipantsList() {
        boolean first = true;
        MutableComponent result = Component.empty();
        for (Entry entry : entries.values()) {
            Player player = entry.getEntity();
            if (player != null) {
                if (!first) {
                    result = result.append(Component.literal(", ").withStyle(ChatFormatting.DARK_AQUA));
                }
                result = result.append(player.getDisplayName().copy().withStyle(ChatFormatting.AQUA));
                first = false;
            }
        }
        return Component.translatable("invmod.message.nexus.listboundplayers", result).withStyle(ChatFormatting.DARK_AQUA);
    }

    private class Entry {
        long time;
        private final UUID id;
        @Nullable
        private Player entity;

        public Entry(long time, UUID playerId) {
            this.time = time;
            id = playerId;
        }

        public Entry(CompoundTag compound) {
            this(compound.getLongOr("time", 0L), compound.read("id", net.minecraft.core.UUIDUtil.CODEC).orElseThrow());
        }

        public Player getEntity() {
            if (entity == null) {
                entity = nexus.getWorld().getPlayerByUUID(id);
            }
            return entity;
        }

        public CompoundTag writeNbt(CompoundTag compound, HolderLookup.Provider lookup) {
            compound.store("id", net.minecraft.core.UUIDUtil.CODEC, id);
            compound.putLong("time", time);
            return compound;
        }
    }
}
