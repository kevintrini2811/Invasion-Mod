package com.invasion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import com.invasion.entity.EntityIMZombie;
import com.invasion.entity.EntityIMZombiePigman;
import com.invasion.entity.IMZoglinEntity;
import com.invasion.entity.IMCreeperEntity;
import com.invasion.entity.IMCaveSpiderEntity;
import com.invasion.entity.IMEndermanEntity;
import com.invasion.entity.IMZombifiedPiglinEntity;
import com.invasion.entity.IMSkeletonEntity;
import com.invasion.entity.ImpEnitty;
import com.invasion.entity.JumpingSpiderEntity;
import com.invasion.entity.NexusSpiderEntity;
import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.entity.QueenSpiderEntity;
import com.invasion.entity.ThrowerEntity;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.wave.EntityPattern;
import com.invasion.nexus.wave.EntityPatterns;
import com.invasion.nexus.wave.pool.Select;

public class InvasionConfig extends Config {
    private static final Map<String, Integer> DEFAULT_MOB_HEALTHS = Util.make(new HashMap<>(), m -> {
        m.put("IMCreeper-T1", 20);
        m.put("IMImp-T1", 20);
        m.put("IMPigManEngineer-T1", 20);
        m.put("IMSkeleton-T1", 20);
        m.put("IMSpider-T1-Spider", 18);
        m.put("IMSpider-T1-Baby-Spider", 3);
        m.put("IMSpider-T1-Cave-Spider", 12);
        m.put("IMSpider-T1-Baby-Cave-Spider", 3);
        m.put("IMSpider-T2-Jumping-Spider", 18);
        m.put("IMSpider-T2-Baby-Jumping-Spider", 3);
        m.put("IMSpider-T2-Mother-Spider", 23);
        m.put("IMThrower-T1", 50);
        m.put("IMThrower-T2", 70);
        m.put("IMZombie-T1", 20);
        m.put("IMZombie-T2", 30);
        m.put("IMZombie-T3", 65);
        m.put("IMZombiePigman-T1", 20);
        m.put("IMZombiePigman-T2", 30);
        m.put("IMZombiePigman-T3", 65);
        m.put("IMZoglin-T1", 65);
        m.put("IMEnderman-T1", 40);
    });
    private static final boolean DEFAULT_NIGHT_SPAWNS_ENABLED = false;
    private static final int DEFAULT_MIN_CONT_MODE_DAYS = 2;
    private static final int DEFAULT_MAX_CONT_MODE_DAYS = 3;
    private static final int DEFAULT_NIGHT_MOB_SIGHT_RANGE = 20;
    private static final int DEFAULT_NIGHT_MOB_SENSE_RANGE = 8;
    private static final int DEFAULT_NIGHT_MOB_SPAWN_CHANCE = 30;
    private static final int DEFAULT_NIGHT_MOB_MAX_GROUP_SIZE = 3;
    private static final int DEFAULT_NIGHT_MOB_LIMIT_OVERRIDE = 70;
    private static final float DEFAULT_NIGHT_MOB_STATS_SCALING = 1.0F;
    private static final boolean DEFAULT_NIGHT_MOBS_BURN = false;

    private final Map<ResourceLocation, Float> strengthOverrides = new HashMap<>();

    public boolean enableLog;
    public boolean debugMode;
    public boolean destructedBlocksDrop = true;
    public boolean updateNotifications;

    public int minContinuousModeDays = DEFAULT_MIN_CONT_MODE_DAYS;
    public int maxContinuousModeDays = DEFAULT_MAX_CONT_MODE_DAYS;

    public boolean nightSpawnsEnabled = DEFAULT_NIGHT_SPAWNS_ENABLED;
    public int nightMobSightRange = DEFAULT_NIGHT_MOB_SIGHT_RANGE;
    public int nightMobSenseRange = DEFAULT_NIGHT_MOB_SENSE_RANGE;
    public int nightMobSpawnChance = DEFAULT_NIGHT_MOB_SPAWN_CHANCE;
    public int nightMobMaxGroupSize = DEFAULT_NIGHT_MOB_MAX_GROUP_SIZE;
    public int maxNightMobs = DEFAULT_NIGHT_MOB_LIMIT_OVERRIDE;
    public boolean nightMobsBurnInDay = DEFAULT_NIGHT_MOBS_BURN;

    private final Map<String, Integer> mobHealthNightspawn = new HashMap<>();
    private final Map<String, Integer> mobHealthInvasion = new HashMap<>();

    @Nullable
    private Select<EntityPattern> spawnPool;

    public Optional<Float> getBlockStrength(Block block) {
        return Optional.ofNullable(strengthOverrides.get(BuiltInRegistries.BLOCK.getKey(block)));
    }

    public Optional<Float> getBlockCost(Block block) {
        return getBlockStrength(block).map(strength -> 1 + strength * 0.4F);
    }

    public int getHealth(String mobName, boolean nightTime) {
        return (nightTime ? mobHealthNightspawn : mobHealthInvasion).getOrDefault(mobName, DEFAULT_MOB_HEALTHS.getOrDefault(mobName, 20));
    }

    public int getHealth(Combatant<?> mob) {
        String healthKey;
        int healthMultiplier = 1;
        if (mob instanceof IMZoglinEntity) {
            healthKey = "IMZoglin-T1";
        } else if (mob instanceof IMZombifiedPiglinEntity piglin) {
            healthKey = "IMZombiePigman-T" + piglin.getTier();
        } else if (mob instanceof EntityIMZombiePigman pigman) {
            healthKey = "IMZombiePigman-T" + pigman.getTier();
        } else if (mob instanceof EntityIMZombie zombie) {
            healthKey = "IMZombie-T" + zombie.getTier();
            if (zombie.isTar()) {
                healthMultiplier = 2;
            }
        } else if (mob instanceof ThrowerEntity thrower) {
            healthKey = "IMThrower-T" + thrower.getTier();
        } else if (mob instanceof IMCaveSpiderEntity) {
            healthKey = mob.asEntity().isBaby()
                    ? "IMSpider-T1-Baby-Cave-Spider"
                    : "IMSpider-T1-Cave-Spider";
        } else if (mob instanceof QueenSpiderEntity) {
            healthKey = "IMSpider-T2-Mother-Spider";
        } else if (mob instanceof JumpingSpiderEntity) {
            healthKey = mob.asEntity().isBaby()
                    ? "IMSpider-T2-Baby-Jumping-Spider"
                    : "IMSpider-T2-Jumping-Spider";
        } else if (mob instanceof NexusSpiderEntity spider) {
            healthKey = spider.isBaby() ? "IMSpider-T1-Baby-Spider" : "IMSpider-T1-Spider";
        } else if (mob instanceof PigmanEngineerEntity) {
            healthKey = "IMPigManEngineer-T1";
        } else if (mob instanceof IMSkeletonEntity) {
            healthKey = "IMSkeleton-T1";
        } else if (mob instanceof IMCreeperEntity) {
            healthKey = "IMCreeper-T1";
        } else if (mob instanceof ImpEnitty) {
            healthKey = "IMImp-T1";
        } else if (mob instanceof IMEndermanEntity) {
            healthKey = "IMEnderman-T1";
        } else {
            healthKey = mob.getLegacyName();
        }
        return getHealth(healthKey, !mob.hasNexus()) * healthMultiplier;
    }

    public synchronized Select<EntityPattern> getSpawnPool() {
        if (spawnPool == null) {
            spawnPool = loadSpawnPool();
        }
        return spawnPool;
    }

    @Override
    public void loadConfig(File file) {
        super.loadConfig(file);
        mobHealthNightspawn.clear();
        mobHealthInvasion.clear();
        keySet().forEach(key -> {
            if (key.startsWith("block-") && key.endsWith("-strength")) {
                ResourceLocation id = ResourceLocation.tryParse(key.split("-")[1]);
                if (id != null) {
                    float strength = getPropertyValueFloat(key, 0);
                    if (strength > 0) {
                        strengthOverrides.put(id, strength);
                    }
                }
            }
            if (key.endsWith("-nightSpawn-health")) {
                String mobName = key.replace("-nightSpawn-health", "");
                int health = getPropertyValueInt(key, 0);
                if (health > 0) {
                    mobHealthNightspawn.put(mobName, health);
                }
            }
            if (key.endsWith("-invasionSpawn-health")) {
                String mobName = key.replace("-invasionSpawn-health", "");
                int health = getPropertyValueInt(key, 0);
                if (health > 0) {
                    mobHealthInvasion.put(mobName, health);
                }
            }
        });

        enableLog = getPropertyValueBoolean("enable-log-file", false);
        destructedBlocksDrop = getPropertyValueBoolean("destructed-blocks-drop", true);
        updateNotifications = getPropertyValueBoolean("update-messages-enabled", false);
        debugMode = getPropertyValueBoolean("debug", false);

        minContinuousModeDays = getPropertyValueInt("min-days-to-attack", 2);
        maxContinuousModeDays = getPropertyValueInt("max-days-to-attack", 3);

        nightSpawnsEnabled = getPropertyValueBoolean("night-spawns-enabled", false);
        nightMobSightRange = getPropertyValueInt("night-mob-sight-range", 20);
        nightMobSenseRange = getPropertyValueInt("night-mob-sense-range", 12);
        nightMobSpawnChance = getPropertyValueInt("night-mob-spawn-chance", 30);
        nightMobMaxGroupSize = getPropertyValueInt("night-mob-max-group-size", 3);
        maxNightMobs = getPropertyValueInt("mob-limit-override", 70);
        nightMobsBurnInDay = getPropertyValueBoolean("night-mobs-burn-in-day", false);
        // Entity patterns depend on registered entity types. Build this lazily
        // after NeoForge has completed the vanilla registry events.
        spawnPool = null;
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(InvasionMod.id("zombie"))) {
            saveConfig(file);
        }
    }

    private Select<EntityPattern> loadSpawnPool() {
        return Select.<EntityPattern>random().apply(builder -> {
            EntityPatterns.REGISTRY.forEach((id, pattern) -> {
                float weight = pattern.getNightMobSpawnWeight();
                if (weight > 0) {
                    builder.entry(pattern.pattern(), weight);
                }
            });
        }).build();
    }

    private void saveConfig(File saveFile) {
        try (var writer = new BufferedWriter(new FileWriter(saveFile))) {
            writeLine(writer, "# Invasion Mod config");
            writeLine(writer, "# Delete this file to restore defaults");
            writer.newLine();
            writeLine(writer, "# General settings and IDs");
            writeProperty(writer, "update-messages-enabled", "Update-messages-enabled is currently unused");
            writeProperty(writer, "destructed-blocks-drop");
            writeProperty(writer, "enable-log-file");
            writeProperty(writer, "craft-items-enabled");
            writeProperty(writer, "guiID-Nexus");
            if (debugMode) {
                writeProperty(writer, "debug");
            }

            writeLine(writer, "# Nexus Continuous Mode");
            writeProperty(writer, "min-days-to-attack");
            writeProperty(writer, "max-days-to-attack");
            writeLine(writer, "# Mob health during invasion");

            for (var pairs : mobHealthInvasion.entrySet()) {
                writeProperty(writer, pairs.getKey().toString());
            }
            writer.newLine();
            writeLine(writer, "# Mob health during invasion (at night)");
            for (var pairs : mobHealthNightspawn.entrySet()) {
                writeProperty(writer, pairs.getKey().toString());
            }
            writer.newLine();

            // Block strength options
            writeLine(writer, "# Block strengths");
            writeLine(writer, "# Add entries here for other mods' blocks");
            writeLine(writer, "# Reference values: minecraft:dirt=3.125, minecraft:gravel=2.5, minecraft:obsidian=7.7, minecraft:stone=5.5 (plus up to 50% from special)");
            writeLine(writer, "# Format:  block-<namespace>:<id>-strength=<strength>");
            if (strengthOverrides.size() == 0) {
                writeLine(writer, "# First example, reinforced stone from IC2 (remove comment symbol '#')");
                writeLine(writer, "# block231-strength=10.5");
            } else {
                for (var entry : strengthOverrides.entrySet()) {
                    writer.write(entry.getKey() + "-strength=" + entry.getValue());
                    writer.newLine();
                }
            }
            writer.newLine();

            writeLine(writer, "# Nighttime mob spawning behaviour (does not affect the nexus)");
            writeProperty(writer, "mob-limit-override", "mob-limit-override: The maximum number of randomly spawned mobs that may exist in the world. This applies to ALL of minecraft (default: 70)");
            writeProperty(writer, "night-spawns-enabled", "night-spawns-enabled: Currently does not remove any default mobs, only adds new spawns");
            writeProperty(writer, "night-mob-spawn-chance", "night-mob-spawn-chance: Higher number means mobs are more common");
            writeProperty(writer, "night-mob-max-group-size", "night-mob-group-size: The maximum number of mobs that may spawn together");
            writeProperty(writer, "night-mob-sight-range", "night-mob-sight-range: How far mobs can see a player from");
            writeProperty(writer, "night-mob-sense-range", "night-mob-sense-range: How far mobs can smell a player (trough walls)");
            writer.newLine();

            writeLine(writer, "# Nightime mob spawning tables (also does not affect the nexus)");
            writeLine(writer, "# A spawnpool contains mobs that can possibly spawn, and the probability weight of them spawning.");
            writeLine(writer, "# Explanation: zombie_t2_any_basic has all T2, zombie_t2_plain excludes tar zombies");
            EntityPatterns.REGISTRY.forEach((id, pattern) -> {
                try {
                    writeValue(writer, "nm-spawnpool1-slot" + pattern.id() + "-weight", pattern.getNightMobSpawnWeight());
                } catch (IOException e) {
                    InvasionMod.LOGGER.error("Could not save config", e);
                }
            });
            writer.flush();
        } catch (IOException e) {
            InvasionMod.LOGGER.error("Could not save config", e);
        }
    }
}
