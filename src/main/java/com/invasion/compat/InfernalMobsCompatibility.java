package com.invasion.compat;

import java.lang.reflect.Method;
import java.util.List;

import com.invasion.InvasionMod;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Optional integration for AtomicStryker's Infernal Mobs. */
public final class InfernalMobsCompatibility {
    private static final String MOD_ID = "infernalmobs";
    private static final String ROLL_TAG = "invmodInfernalMobsRolled";
    private static final List<String> MODIFIERS = List.of(
            "1UP", "Alchemist", "Berserk", "Blastoff", "Bulwark",
            "Choke", "Cloaking", "Darkness", "Ender", "Exhaust",
            "Fiery", "Ghastly", "Gravity", "Lifesteal", "Ninja",
            "Poisonous", "Quicksand", "Regen", "Rust", "Sapper",
            "Sprint", "Sticky", "Storm", "Unyielding", "Vengeance",
            "Weakness", "Webber", "Wither");

    private static Method getMobModifiers;
    private static Method addEntityModifiersByString;
    private static Method getNbtTag;
    private static Method setMobWasSpawnedBefore;
    private static Object infernalMobs;
    private static boolean available;

    private InfernalMobsCompatibility() {
    }

    public static void bootstrap() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> core = Class.forName(
                    "atomicstryker.infernalmobs.common.InfernalMobsCore");
            infernalMobs = core.getMethod("instance").invoke(null);
            getMobModifiers = core.getMethod(
                    "getMobModifiers",
                    net.minecraft.world.entity.LivingEntity.class);
            addEntityModifiersByString = core.getMethod(
                    "addEntityModifiersByString",
                    net.minecraft.world.entity.LivingEntity.class,
                    String.class);
            getNbtTag = core.getMethod("getNBTTag");
            setMobWasSpawnedBefore = core.getMethod(
                    "setMobWasSpawnedBefore",
                    net.minecraft.world.entity.LivingEntity.class);
            available = true;
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                    InfernalMobsCompatibility::entityJoined);
        } catch (ReflectiveOperationException | LinkageError exception) {
            InvasionMod.LOGGER.error(
                    "Infernal Mobs is installed, but its compatibility API "
                            + "could not be initialized",
                    exception);
        }
    }

    private static void entityJoined(EntityJoinLevelEvent event) {
        if (!available || event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Mob mob)
                || !(mob instanceof Combatant<?> combatant)
                || !BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())
                        .getNamespace().equals(InvasionMod.MOD_ID)) {
            return;
        }

        NexusAccess nexus = combatant.getNexus();
        if (nexus == null || !nexus.isActive()
                || mob.getPersistentData().getBooleanOr(ROLL_TAG, false)) {
            return;
        }
        mob.getPersistentData().putBoolean(ROLL_TAG, true);

        try {
            String nbtTag = (String) getNbtTag.invoke(infernalMobs);
            if (mob.getPersistentData().contains(nbtTag)) {
                // Preserve modifiers (or the non-infernal marker) restored
                // from disk instead of treating a loaded mob as a new spawn.
                return;
            }

            // Run before Infernal Mobs' regular spawn hook and mark the mob as
            // handled. This leaves only the invasion wave roll while its Nexus
            // is active.
            setMobWasSpawnedBefore.invoke(null, mob);
            int chancePercent = Math.clamp(nexus.getCurrentWave(), 0, 100);
            if (mob.getRandom().nextInt(100) >= chancePercent) {
                return;
            }

            int start = mob.getRandom().nextInt(MODIFIERS.size());
            for (int offset = 0; offset < MODIFIERS.size(); offset++) {
                String modifier = MODIFIERS.get(
                        (start + offset) % MODIFIERS.size());
                addEntityModifiersByString.invoke(
                        infernalMobs, mob, modifier);
                if (getMobModifiers.invoke(null, mob) != null) {
                    return;
                }
            }
            InvasionMod.LOGGER.warn(
                    "Infernal Mobs did not accept any modifier for {}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
        } catch (ReflectiveOperationException | LinkageError exception) {
            available = false;
            InvasionMod.LOGGER.error(
                    "Disabling Infernal Mobs compatibility after an API error",
                    exception);
        }
    }
}
