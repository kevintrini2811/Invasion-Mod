package com.invasion.nexus;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.invasion.BountyHunter;
import com.invasion.InvasionMod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

public class Participants {
    private static final Text POSSESSIVE_SUFFEX_A = Text.literal("'");
    private static final Text POSSESSIVE_SUFFEX_B = Text.literal("'s");

    private final Map<UUID, Entry> entries = new HashMap<>();

    private final Nexus nexus;

    public Participants(Nexus nexus) {
        this.nexus = nexus;
    }

    public void bindPlayers(Box arena) {
        final long now = System.currentTimeMillis();
        for (PlayerEntity player : nexus.getWorld().getEntitiesByClass(PlayerEntity.class, arena, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR)) {
            entries.compute(player.getUuid(), (id, oldEntry) -> {
                if (oldEntry == null || now - oldEntry.time > NexusAccess.BIND_EXPIRE_TIME) {
                    Text message = Text.translatable("invmod.message.nexus.lifenowbound", pluralize(player.getDisplayName())).formatted(Formatting.DARK_GREEN);
                    sendMessage(message);
                    if (oldEntry == null) {
                        player.sendMessage(message);
                    }
                    return new Entry(now, player.getUuid());
                }
                return oldEntry;
            });
        }
    }


    private Text pluralize(Text text) {
        return text.copy().append(text.getString().toLowerCase(Locale.ROOT).endsWith("s") ? POSSESSIVE_SUFFEX_A : POSSESSIVE_SUFFEX_B).formatted(Formatting.GREEN);
    }

    public void sendWarning(String translationKey, Object...params) {
        sendMessage(Formatting.RED, translationKey, params);
    }

    public void sendNotice(String translationKey, Object...params) {
        sendMessage(Formatting.DARK_GREEN, translationKey, params);
    }

    public void sendMessage(Formatting color, String translationKey, Object...params) {
        sendMessage(Text.translatable(translationKey, params).formatted(color));
    }

    public void sendMessage(Text message) {
        for (Entry entry : entries.values()) {
            PlayerEntity player = entry.getEntity();
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public void playSoundForBoundPlayers(SoundEvent sound) {
        playSoundForBoundPlayers(sound, 1, 1);
    }

    public void playSoundForBoundPlayers(SoundEvent sound, float volume, float pitch) {
        for (Entry entry : entries.values()) {
            try {
                PlayerEntity player = entry.getEntity();
                if (player != null) {
                    player.getWorld().playSound(null, player.getBlockPos(), sound, SoundCategory.AMBIENT, volume, pitch);
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
                PlayerEntity player = entry.getEntity();
                if (player != null) {
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.AMBIENT, 4, 1);
                    player.damage(player.getWorld().getDamageSources().magic(), 500);
                } else if (nexus.getWorld() instanceof ServerWorld sw) {
                    BountyHunter.of(sw).add(entry.id);
                }
            }
        }

        entries.clear();
    }

    public void readNbt(NbtCompound compound, RegistryWrapper.WrapperLookup lookup) {
        entries.clear();
        compound.getList("entries", NbtElement.COMPOUND_TYPE).forEach(el -> {
            Entry entry = new Entry((NbtCompound)el);
            entries.put(entry.id, entry);
        });
    }

    public NbtCompound writeNbt(NbtCompound compound, RegistryWrapper.WrapperLookup lookup) {
        NbtList entries = new NbtList();
        for (Entry entry : this.entries.values()) {
            entries.add(entry.writeNbt(new NbtCompound(), lookup));
        }
        compound.put("entries", entries);
        return compound;
    }

    public Text getParticipantsList() {
        boolean first = true;
        MutableText result = Text.empty();
        for (Entry entry : entries.values()) {
            PlayerEntity player = entry.getEntity();
            if (player != null) {
                if (!first) {
                    result = result.append(Text.literal(", ").formatted(Formatting.DARK_AQUA));
                }
                result = result.append(player.getDisplayName().copy().formatted(Formatting.AQUA));
                first = false;
            }
        }
        return Text.translatable("invmod.message.nexus.listboundplayers", result).formatted(Formatting.DARK_AQUA);
    }

    private class Entry {
        long time;
        private final UUID id;
        @Nullable
        private PlayerEntity entity;

        public Entry(long time, UUID playerId) {
            this.time = time;
            id = playerId;
        }

        public Entry(NbtCompound compound) {
            this(compound.getLong("time"), compound.getUuid("id"));
        }

        public PlayerEntity getEntity() {
            if (entity == null) {
                entity = nexus.getWorld().getPlayerByUuid(id);
            }
            return entity;
        }

        public NbtCompound writeNbt(NbtCompound compound, RegistryWrapper.WrapperLookup lookup) {
            compound.putUuid("id", id);
            compound.putLong("time", time);
            return compound;
        }
    }
}
