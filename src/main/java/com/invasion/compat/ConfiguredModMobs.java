package com.invasion.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.invasion.InvasionMod;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.util.math.PosUtils;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Configurable, loader-independent support for hostile mobs from other mods. */
public final class ConfiguredModMobs {
    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("invasion_mod_mobs.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Identifier, Entry> ENTRIES = new LinkedHashMap<>();
    private static boolean loaded;

    private ConfiguredModMobs() {
    }

    public static void bootstrap() {
        NeoForge.EVENT_BUS.addListener(ConfiguredModMobs::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(ConfiguredModMobs::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(ConfiguredModMobs::onEntityTick);
    }

    /** Reloads user choices, then adds newly installed hostile entity types. */
    public static synchronized void refresh() {
        Map<Identifier, Entry> result = new LinkedHashMap<>();
        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> jsonEntry : root.entrySet()) {
                    Identifier id = Identifier.tryParse(jsonEntry.getKey());
                    if (id == null || !jsonEntry.getValue().isJsonObject()) continue;
                    JsonObject value = jsonEntry.getValue().getAsJsonObject();
                    boolean active = value.has("active") && value.get("active").getAsBoolean();
                    int cost = value.has("cost") ? Math.max(1, value.get("cost").getAsInt()) : 5;
                    result.put(id, new Entry(active, cost));
                }
            } catch (Exception exception) {
                InvasionMod.LOGGER.error("Could not read {}; leaving it unchanged", FILE, exception);
                loaded = true;
                ENTRIES.clear();
                return;
            }
        }

        BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .filter(registryEntry -> isExternalMonster(registryEntry.getKey().identifier(), registryEntry.getValue()))
                .sorted(Comparator.comparing(entry -> entry.getKey().identifier().toString()))
                .forEach(registryEntry -> result.putIfAbsent(
                        registryEntry.getKey().identifier(), new Entry(false, 5)));
        ENTRIES.clear();
        ENTRIES.putAll(result);
        loaded = true;
        write();
    }

    @SuppressWarnings("unchecked")
    public static synchronized List<WaveMob> activeWaveMobs() {
        if (!loaded) refresh();
        List<WaveMob> result = new ArrayList<>();
        ENTRIES.forEach((id, entry) -> {
            if (!entry.active()) return;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type != null && isExternalMonster(id, type)) {
                result.add(new WaveMob((EntityType<? extends Mob>) type, entry.cost()));
            }
        });
        return List.copyOf(result);
    }

    private static boolean isExternalMonster(Identifier id, EntityType<?> type) {
        return !id.getNamespace().equals("minecraft")
                && !id.getNamespace().equals(InvasionMod.MOD_ID)
                && type.getCategory() == MobCategory.MONSTER;
    }

    private static void write() {
        JsonObject root = new JsonObject();
        ENTRIES.forEach((id, entry) -> {
            JsonObject value = new JsonObject();
            value.addProperty("active", entry.active());
            value.addProperty("cost", entry.cost());
            root.add(id.toString(), value);
        });
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            InvasionMod.LOGGER.error("Could not update {}", FILE, exception);
        }
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)
                || !(event.getEntity() instanceof Mob mob)) return;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        Entry entry;
        synchronized (ConfiguredModMobs.class) {
            entry = ENTRIES.get(id);
        }
        if (entry == null || !entry.active()) return;
        mob.goalSelector.addGoal(1, new AttackNexusGoal(mob));
        mob.goalSelector.addGoal(4, new GoToNexusGoal(mob));
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !mob.getPersistentData().contains("invmodWaveNumber")) return;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        synchronized (ConfiguredModMobs.class) {
            Entry entry = ENTRIES.get(id);
            if (entry == null || !entry.active()) return;
        }
        WorldNexusStorage.of(level).getNexus().ifPresent(
                nexus -> nexus.notifyExternalWaveMobKilled(mob));
    }

    private static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || level.getSkyDarken() >= 4
                || !mob.isOnFire() || !isActive(mob.getType())) return;
        if (activeNexus(mob) != null) mob.clearFire();
    }

    private static synchronized boolean isActive(EntityType<?> type) {
        Entry entry = ENTRIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry != null && entry.active();
    }

    private static NexusAccess activeNexus(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        return WorldNexusStorage.of(level).getNexus()
                .filter(nexus -> nexus.isActive() && !nexus.isDiscarded())
                .orElse(null);
    }

    private static final class GoToNexusGoal extends Goal {
        private final Mob mob;
        private NexusAccess nexus;

        private GoToNexusGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            nexus = activeNexus(mob);
            return nexus != null && (mob.getTarget() == null || !mob.getTarget().isAlive())
                    && mob.distanceToSqr(PosUtils.center(nexus.getOrigin())) > 9.0;
        }

        @Override public boolean canContinueToUse() {
            return nexus != null && nexus.isActive()
                    && (mob.getTarget() == null || !mob.getTarget().isAlive())
                    && mob.distanceToSqr(PosUtils.center(nexus.getOrigin())) > 9.0;
        }

        @Override public void start() { move(); }
        @Override public void tick() {
            if (mob.getNavigation().isDone()) move();
        }
        private void move() {
            mob.getNavigation().moveTo(nexus.getOrigin().getX() + 0.5,
                    nexus.getOrigin().getY(), nexus.getOrigin().getZ() + 0.5, 1.0);
        }
    }

    private static final class AttackNexusGoal extends Goal {
        private final Mob mob;
        private NexusAccess nexus;
        private int cooldown;

        private AttackNexusGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override public boolean canUse() {
            nexus = activeNexus(mob);
            return nexus != null && (mob.getTarget() == null || !mob.getTarget().isAlive())
                    && mob.distanceToSqr(PosUtils.center(nexus.getOrigin())) <= 16.0;
        }

        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { cooldown = 20; }
        @Override public void tick() {
            mob.getLookControl().setLookAt(PosUtils.center(nexus.getOrigin()));
            if (--cooldown <= 0) {
                mob.swing(InteractionHand.MAIN_HAND);
                nexus.damage(mob.damageSources().mobAttack(mob), 2);
                cooldown = 20;
            }
        }
    }

    private record Entry(boolean active, int cost) {}
    public record WaveMob(EntityType<? extends Mob> type, int cost) {}
}
