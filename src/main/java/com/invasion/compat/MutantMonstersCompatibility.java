package com.invasion.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.IMWitchEntity;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Optional, class-linkage-free support for Mutant Monsters mobs. */
public final class MutantMonstersCompatibility {
    public static final String MOD_ID = "mutantmonsters";
    public static final String WAVE_MOB_TAG = "invmodMutantWaveMob";
    public static final List<String> MOB_NAMES = List.of(
            "mutant_zombie", "mutant_creeper", "mutant_skeleton",
            "mutant_enderman", "mutant_snow_golem", "spider_pig");

    private static final double TARGET_RANGE = 32.0D;
    private static final Map<PathfinderMob, NexusAccess> BINDINGS =
            new WeakHashMap<>();

    private MutantMonstersCompatibility() {
    }

    public static void bootstrap() {
        if (!isLoaded()) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(MutantMonstersCompatibility::onJoin);
        NeoForge.EVENT_BUS.addListener(MutantMonstersCompatibility::tick);
        NeoForge.EVENT_BUS.addListener(MutantMonstersCompatibility::onTargetChanged);
        NeoForge.EVENT_BUS.addListener(MutantMonstersCompatibility::onIncomingDamage);
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isMutant(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && id.getNamespace().equals(MOD_ID)
                && MOB_NAMES.contains(id.getPath());
    }

    @SuppressWarnings("unchecked")
    public static List<EntityType<? extends Mob>> mobTypes() {
        if (!isLoaded()) {
            return List.of();
        }
        List<EntityType<? extends Mob>> result = new ArrayList<>();
        for (String name : MOB_NAMES) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(
                    Identifier.fromNamespaceAndPath(MOD_ID, name));
            if (type != null && isMutant(type)) {
                result.add((EntityType<? extends Mob>) type);
            }
        }
        return List.copyOf(result);
    }

    @Nullable
    public static EntityType<? extends Mob> mobType(String name) {
        return mobTypes().stream()
                .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type)
                        .getPath().equals(name))
                .findFirst().orElse(null);
    }

    public static void markWaveMob(Mob mob) {
        if (isMutant(mob.getType())) {
            mob.getPersistentData().putBoolean(WAVE_MOB_TAG, true);
        }
    }

    public static boolean isBoundMutant(Object entity) {
        return entity instanceof PathfinderMob mob && BINDINGS.containsKey(mob);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof PathfinderMob mob)
                || !isMutant(mob.getType())) {
            return;
        }
        WorldNexusStorage.of(level).getNexus()
                .filter(NexusAccess::isActive)
                .ifPresent(nexus -> bind(mob, nexus));
    }

    private static void bind(PathfinderMob mob, NexusAccess nexus) {
        if (BINDINGS.put(mob, nexus) == null) {
            mob.goalSelector.addGoal(4, new MutantNexusGoal(mob));
            if (!mob.getPersistentData().getBooleanOr(WAVE_MOB_TAG, false)) {
                double health = Math.max(1.0D, mob.getMaxHealth() * 0.3D);
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                mob.setHealth((float) health);
            }
            mob.setPersistenceRequired();
        }
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        NexusAccess activeNexus = WorldNexusStorage.of(level).getNexus()
                .filter(NexusAccess::isActive).orElse(null);
        List<PathfinderMob> mobs = List.copyOf(BINDINGS.keySet());
        for (PathfinderMob mob : mobs) {
            if (mob.isRemoved() || !mob.isAlive() || mob.level() != level) {
                BINDINGS.remove(mob);
                continue;
            }
            if (activeNexus == null) {
                mob.discard();
                BINDINGS.remove(mob);
                continue;
            }
            BINDINGS.put(mob, activeNexus);
            if (level.getGameTime() % 5L == Math.floorMod(mob.getId(), 5)) {
                selectPlayerAllyTarget(level, mob);
            }
        }
    }

    private static void selectPlayerAllyTarget(
            ServerLevel level, PathfinderMob mob) {
        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive()
                && IMWitchEntity.isPlayerAlly(current, level)) {
            return;
        }
        LivingEntity nearest = null;
        double nearestDistance = TARGET_RANGE * TARGET_RANGE;
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class, mob.getBoundingBox().inflate(TARGET_RANGE),
                candidate -> candidate.isAlive()
                        && IMWitchEntity.isPlayerAlly(candidate, level)
                        && mob.canAttack(candidate)
                        && mob.hasLineOfSight(candidate))) {
            double distance = mob.distanceToSqr(candidate);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private static void onTargetChanged(LivingChangeTargetEvent event) {
        if (isBoundMutant(event.getEntity())
                && (isBoundMutant(event.getNewAboutToBeSetTarget())
                        || event.getNewAboutToBeSetTarget()
                                instanceof com.invasion.nexus.Combatant<?>)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Object attacker = event.getSource().getEntity();
        if (isBoundMutant(event.getEntity())
                && (isBoundMutant(attacker)
                        || attacker instanceof com.invasion.nexus.Combatant<?>)) {
            event.setCanceled(true);
        }
    }

    private static final class MutantNexusGoal extends Goal {
        private final PathfinderMob mob;
        private int repathCooldown;
        private int attackCooldown;

        private MutantNexusGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            NexusAccess nexus = BINDINGS.get(mob);
            return mob.getTarget() == null && nexus != null && nexus.isActive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            NexusAccess nexus = BINDINGS.get(mob);
            if (nexus == null) {
                return;
            }
            double distance = mob.distanceToSqr(
                    Vec3.atCenterOf(nexus.getOrigin()));
            if (distance <= 16.0D) {
                mob.getNavigation().stop();
                if (--attackCooldown <= 0) {
                    mob.swing(InteractionHand.MAIN_HAND);
                    nexus.damage(mob.damageSources().mobAttack(mob), 2);
                    attackCooldown = 20;
                }
            } else if (--repathCooldown <= 0
                    || mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(
                        nexus.getOrigin().getX() + 0.5D,
                        nexus.getOrigin().getY(),
                        nexus.getOrigin().getZ() + 0.5D, 1.1D);
                repathCooldown = 20;
            }
        }
    }
}
