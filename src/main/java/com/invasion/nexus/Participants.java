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
import net.minecraft.server.level.ServerPlayer;
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
        for (Player player : nexus.getWorld().getEntitiesOfClass(
                Player.class, arena, EntitySelector.NO_SPECTATORS)) {
            entries.compute(player.getUUID(), (id, oldEntry) -> {
                if (oldEntry == null) {
                    Component message = Component.translatable("invmod.message.nexus.lifenowbound", pluralize(player.getDisplayName())).withStyle(ChatFormatting.DARK_GREEN);
                    sendMessage(message);
                    if (oldEntry == null) {
                        player.sendSystemMessage(message);
                    }
                    return new Entry(now, player.getUUID(), player.isCreative());
                }
				oldEntry.time = now;
                oldEntry.creativeAtBinding = player.isCreative();
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

    public boolean reconnect(ServerPlayer player) {
        Entry entry = entries.get(player.getUUID());
        if (entry == null) {
            return false;
        }
        entry.entity = player;
        return true;
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
                    if (!player.isCreative()) {
                        player.hurt(player.level().damageSources().magic(), 500);
                    }
                } else if (!entry.creativeAtBinding
                        && nexus.getWorld() instanceof ServerLevel sw) {
                    BountyHunter.of(sw).add(entry.id);
                }
            }
        }

        entries.clear();
    }

    public void readNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        entries.clear();
        compound.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND).forEach(el -> {
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
        private boolean creativeAtBinding;
        @Nullable
        private Player entity;

        public Entry(long time, UUID playerId, boolean creativeAtBinding) {
            this.time = time;
            id = playerId;
            this.creativeAtBinding = creativeAtBinding;
        }

        public Entry(CompoundTag compound) {
            this(compound.getLong("time"),
                    compound.getUUID("id"),
                    compound.getBoolean("creativeAtBinding"));
        }

        public Player getEntity() {
            Player currentPlayer = nexus.getWorld().getPlayerByUUID(id);
            entity = currentPlayer;
            return entity;
        }

        public CompoundTag writeNbt(CompoundTag compound, HolderLookup.Provider lookup) {
            compound.putUUID("id", id);
            compound.putLong("time", time);
            compound.putBoolean("creativeAtBinding", creativeAtBinding);
            return compound;
        }
    }
}
