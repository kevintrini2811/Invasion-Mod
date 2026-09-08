package com.invasion.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.invasion.InvasionMod;
import com.invasion.entity.EquipmentUtil;
import com.invasion.entity.IMCivilianTargetHandler;
import com.invasion.mixin.PhantomAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.nexus.wave.BudgetWavePlan.Theme;
import com.invasion.nexus.wave.BudgetWavePlan;
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
import java.util.LinkedHashSet;
import java.util.UUID;
import com.invasion.nexus.Combatant;
import net.minecraftforge.event.TickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.invasion.entity.SkeletonArrowEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Configurable, loader-independent support for hostile mobs from other mods. */
public final class ConfiguredModMobs {
    private static final List<String> AVAILABLE_THEMES = List.of(
            "SWARM", "ARMORED", "RANGED", "UNDERGROUND", "SPIDER", "FLYING",
            "NETHER", "SIEGE", "FAST", "MIXED", "RANDOM", "RANDOMHELL");
    private static final List<String> DEFAULT_THEMES = List.of(
            "MIXED", "RANDOM", "RANDOMHELL");
    private static final Map<String, Entry> MOD_DEFAULTS = Map.of(
            "mutantmonsters:endersoul_clone",
            new Entry(true, 3, DEFAULT_THEMES, Abilities.NONE),
            "variantsandventures:gelid",
            new Entry(true, 3, List.of("SWARM", "MIXED", "RANDOM", "RANDOMHELL"), Abilities.EQUIPMENT),
            "variantsandventures:murk",
            new Entry(true, 3, List.of("RANGED", "MIXED", "RANDOM", "RANDOMHELL"), Abilities.EQUIPMENT),
            "variantsandventures:thicket",
            new Entry(true, 3, List.of("SWARM", "MIXED", "RANDOM", "RANDOMHELL"), Abilities.EQUIPMENT),
            "variantsandventures:verdant",
            new Entry(true, 3, List.of("RANGED", "MIXED", "RANDOM", "RANDOMHELL"), Abilities.EQUIPMENT));
    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("invasion_mod_mobs.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, Entry> ENTRIES = new LinkedHashMap<>();
    private static boolean loaded;
    private static final String NEXUS_OWNER = "invmodConfiguredNexus";

    private ConfiguredModMobs() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(ConfiguredModMobs::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(ConfiguredModMobs::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(ConfiguredModMobs::onEntityTick);
        MinecraftForge.EVENT_BUS.addListener(ConfiguredModMobs::onLevelTick);
    }

    /** Includes natural and egg spawns, not just mobs purchased by a wave. */
    public static void cleanupNexusMobs(ServerLevel level, NexusAccess nexus) {
        String owner = nexus.getUuid().toString();
        List<Mob> removals = new ArrayList<>();
        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || mob instanceof Combatant<?>) continue;
            String binding = mob.getPersistentData().getString(NEXUS_OWNER);
            if (owner.equals(binding) || binding.isEmpty() && isActive(mob.getType())) {
                removals.add(mob);
            }
        }
        // Snapshot first: removing entities may mutate the level's entity index.
        removals.forEach(Mob::discard);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        List<Mob> removals = new ArrayList<>();
        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || mob instanceof Combatant<?>) continue;
            String binding = mob.getPersistentData().getString(NEXUS_OWNER);
            if (!binding.isEmpty()) {
                NexusAccess owner = null;
                try {
                    owner = WorldNexusStorage.of(level).getNexus(UUID.fromString(binding));
                } catch (IllegalArgumentException ignored) {
                    // Invalid saved bindings must not leave invasion mobs orphaned.
                }
                if (owner == null || owner.isDiscarded() || !owner.isActive()) {
                    removals.add(mob);
                    continue;
                }
            } else if (isActive(mob.getType())) {
                activeNexus(mob);
            }
        }
        removals.forEach(Mob::discard);
    }

    /** Reloads user choices, then adds newly installed hostile entity types. */
    public static synchronized void refresh() {
        refresh(false);
    }

    public static synchronized void regenerate() {
        refresh(true);
    }

    private static void refresh(boolean reset) {
        Map<ResourceLocation, Entry> result = new LinkedHashMap<>();
        if (!reset && Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> jsonEntry : root.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(jsonEntry.getKey());
                    if (id == null || !jsonEntry.getValue().isJsonObject()) continue;
                    if (isSpecializedOriginal(id) || isUnavailableSpecializedImMob(id)) continue;
                    JsonObject value = jsonEntry.getValue().getAsJsonObject();
                    boolean active = value.has("active") && value.get("active").getAsBoolean();
                    int cost = value.has("cost") ? Math.max(1, value.get("cost").getAsInt()) : 5;
                    Entry defaults = defaultEntry(id);
                    result.put(id, new Entry(active, cost, readThemes(value),
                            readAbilities(value, defaults.abilities())));
                }
            } catch (Exception exception) {
                InvasionMod.LOGGER.error("Could not read {}; leaving it unchanged", FILE, exception);
                loaded = true;
                ENTRIES.clear();
                return;
            }
        }

        BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .filter(registryEntry -> isExternalMonster(registryEntry.getKey().location(), registryEntry.getValue()))
                .filter(registryEntry -> !isSpecializedOriginal(registryEntry.getKey().location()))
                .sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
                .forEach(registryEntry -> result.putIfAbsent(
                        registryEntry.getKey().location(), defaultEntry(registryEntry.getKey().location())));
        BudgetWavePlan.configMobDefaults().forEach(mob -> result.putIfAbsent(
                mob.id(), new Entry(true, mob.cost(), mob.themes(), new Abilities(
                        mob.canUseWeapons(), mob.canWearArmor(), mob.canMine(),
                        mob.canStair(), mob.canBridge(), mob.canTower()))));
        ENTRIES.clear();
        ENTRIES.putAll(result);
        loaded = true;
        write();
    }

    @SuppressWarnings("unchecked")
    public static synchronized List<WaveMob> activeWaveMobs(Theme theme) {
        if (!loaded) refresh();
        List<WaveMob> result = new ArrayList<>();
        ENTRIES.forEach((id, entry) -> {
            if (!entry.active() || !entry.themes().contains(theme.name())) return;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type != null && isExternalMonster(id, type)) {
                result.add(new WaveMob((EntityType<? extends Mob>) type, entry.cost()));
            }
        });
        return List.copyOf(result);
    }

    private static boolean isExternalMonster(ResourceLocation id, EntityType<?> type) {
        return !id.getNamespace().equals("minecraft")
                && !id.getNamespace().equals(InvasionMod.MOD_ID)
                && type.getCategory() == MobCategory.MONSTER;
    }

    private static boolean isSpecializedOriginal(ResourceLocation id) {
        String namespace = id.getNamespace();
        String path = id.getPath();
        return namespace.equals(MutantMonstersCompatibility.MOD_ID)
                        && MutantMonstersCompatibility.MOB_NAMES.contains(path)
                || namespace.equals(FriendsAndFoesCompatibility.MOD_ID)
                        && path.equals("wildfire")
                || namespace.equals("tinyskeletons") && List.of(
                        "baby_skeleton", "baby_bogged", "baby_parched",
                        "baby_stray", "baby_wither_skeleton").contains(path);
    }

    private static boolean isUnavailableSpecializedImMob(ResourceLocation id) {
        if (!id.getNamespace().equals(InvasionMod.MOD_ID)) return false;
        return MutantMonstersCompatibility.MOB_NAMES.contains(id.getPath())
                        && !MutantMonstersCompatibility.isLoaded()
                || id.getPath().equals("wildfire")
                        && !FriendsAndFoesCompatibility.isLoaded();
    }

    private static void write() {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "Available themes: "
                + String.join(", ", AVAILABLE_THEMES));
        ENTRIES.forEach((id, entry) -> {
            JsonObject value = new JsonObject();
            value.addProperty("active", entry.active());
            value.addProperty("cost", entry.cost());
            JsonObject abilities = new JsonObject();
            abilities.addProperty("weapons", entry.abilities().weapons());
            abilities.addProperty("armor", entry.abilities().armor());
            abilities.addProperty("mining", entry.abilities().mining());
            abilities.addProperty("stairing", entry.abilities().stairing());
            abilities.addProperty("bridging", entry.abilities().bridging());
            abilities.addProperty("towering", entry.abilities().towering());
            value.add("abilities", abilities);
            com.google.gson.JsonArray themes = new com.google.gson.JsonArray();
            entry.themes().forEach(themes::add);
            value.add("themes", themes);
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

    private static List<String> readThemes(JsonObject value) {
        if (!value.has("themes") || !value.get("themes").isJsonArray()) return DEFAULT_THEMES;
        LinkedHashSet<String> themes = new LinkedHashSet<>();
        value.getAsJsonArray("themes").forEach(element -> {
            if (!element.isJsonPrimitive()) return;
            String name = element.getAsString().toUpperCase(java.util.Locale.ROOT);
            if (AVAILABLE_THEMES.contains(name)) {
                themes.add(name);
            } else {
                InvasionMod.LOGGER.warn("Ignoring unknown mod mob theme {}", name);
            }
        });
        return List.copyOf(themes);
    }

    private static Entry defaultEntry(ResourceLocation id) {
        return MOD_DEFAULTS.getOrDefault(id.toString(),
                new Entry(false, 5, DEFAULT_THEMES, Abilities.NONE));
    }

    private static Abilities readAbilities(JsonObject value, Abilities defaults) {
        JsonObject abilities = value.has("abilities") && value.get("abilities").isJsonObject()
                ? value.getAsJsonObject("abilities") : new JsonObject();
        return new Abilities(
                ability(abilities, "weapons", value, "canUseWeapons", defaults.weapons()),
                ability(abilities, "armor", value, "canWearArmor", defaults.armor()),
                ability(abilities, "mining", value, null, defaults.mining()),
                ability(abilities, "stairing", value, null, defaults.stairing()),
                ability(abilities, "bridging", value, null, defaults.bridging()),
                ability(abilities, "towering", value, null, defaults.towering()));
    }

    private static boolean ability(JsonObject abilities, String key,
            JsonObject legacy, String legacyKey, boolean fallback) {
        if (abilities.has(key)) return abilities.get(key).getAsBoolean();
        return legacyKey != null && legacy.has(legacyKey)
                ? legacy.get(legacyKey).getAsBoolean() : fallback;
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)
                || !(event.getEntity() instanceof Mob mob)) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        Entry entry;
        synchronized (ConfiguredModMobs.class) {
            entry = ENTRIES.get(id);
        }
        if (entry == null || !entry.active()
                || !isExternalMonster(id, mob.getType())) return;
        activeNexus(mob);
        mob.targetSelector.addGoal(0, new InvasionTargetGoal(mob));
        mob.goalSelector.addGoal(0, new SpecialMovementNexusGoal(mob));
        mob.goalSelector.addGoal(1, new AttackNexusGoal(mob));
        mob.goalSelector.addGoal(2, new RangedAttackNexusGoal(mob));
        mob.goalSelector.addGoal(3, new ClimbNexusLadderGoal(mob));
        mob.goalSelector.addGoal(4, new GoToNexusGoal(mob));
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !mob.getPersistentData().contains("invmodWaveNumber")) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        synchronized (ConfiguredModMobs.class) {
            Entry entry = ENTRIES.get(id);
            if (entry == null || !entry.active() || !isExternalMonster(id, mob.getType())) return;
        }
        WorldNexusStorage.of(level).getNexus().ifPresent(
                nexus -> nexus.notifyExternalWaveMobKilled(mob));
    }

    private static void onEntityTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !isActive(mob.getType())) return;

        if (level.getSkyDarken() < 4 && mob.isOnFire()
                && activeNexus(mob) != null) {
            mob.clearFire();
        }
        if (mob.tickCount % 15 == 0) {
            pickUpAllowedEquipment(mob, level);
        }
    }

    private static void pickUpAllowedEquipment(Mob mob, ServerLevel level) {
        boolean armorAllowed = allowsArmor(mob.getType(), false);
        boolean weaponsAllowed = allowsWeapons(mob.getType(), false);
        if (!armorAllowed && !weaponsAllowed) return;

        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class, mob.getBoundingBox().inflate(1.25D),
                candidate -> !candidate.hasPickUpDelay()
                        && isAllowedEquipment(mob, candidate.getItem(),
                                armorAllowed, weaponsAllowed))) {
            AsyncCompatibility.pickUpEquipment(mob, level, item);
        }
    }

    private static boolean isAllowedEquipment(Mob mob, ItemStack stack,
            boolean armorAllowed, boolean weaponsAllowed) {
        EquipmentSlot slot = mob.getEquipmentSlotForItem(stack);
        return armorAllowed && slot.isArmor()
                || weaponsAllowed && EquipmentUtil.isWeapon(stack);
    }

    public static synchronized boolean isActive(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Entry entry = ENTRIES.get(id);
        return entry != null && entry.active() && isExternalMonster(id, type);
    }

    public static synchronized boolean allowsArmor(EntityType<?> type, boolean fallback) {
        Entry entry = ENTRIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry == null ? fallback : entry.abilities().armor();
    }

    public static synchronized boolean allowsWeapons(EntityType<?> type, boolean fallback) {
        Entry entry = ENTRIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry == null ? fallback : entry.abilities().weapons();
    }

    public static synchronized boolean allowsMining(EntityType<?> type, boolean fallback) {
        Entry entry = ENTRIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry == null ? fallback : entry.abilities().mining();
    }

    public static synchronized boolean allowsStairing(EntityType<?> type, boolean fallback) {
        Entry entry = ENTRIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry == null ? fallback : entry.abilities().stairing();
    }

    public static synchronized boolean allowsBridging(EntityType<?> type, boolean fallback) {
        Entry entry = ENTRIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry == null ? fallback : entry.abilities().bridging();
    }

    public static synchronized boolean allowsTowering(EntityType<?> type, boolean fallback) {
        Entry entry = ENTRIES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entry == null ? fallback : entry.abilities().towering();
    }

    /** Recognizes configured invasion allies without changing their Nexus binding. */
    public static boolean isInvasionAlly(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return false;
        if (!mob.getPersistentData().getString(NEXUS_OWNER).isEmpty()) return true;
        return isActive(mob.getType()) && WorldNexusStorage.of(level).getNexus()
                .filter(nexus -> nexus.isActive() && !nexus.isDiscarded()).isPresent();
    }

    /** Checks the saved owner without binding mobs as a side effect of debugging. */
    public static boolean isBoundToNexus(Mob mob, NexusAccess nexus) {
        return nexus.getUuid().toString().equals(
                mob.getPersistentData().getString(NEXUS_OWNER));
    }

    public static NexusAccess activeNexus(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        NexusAccess nexus = WorldNexusStorage.of(level).getNexus()
                .filter(candidate -> candidate.isActive() && !candidate.isDiscarded())
                .orElse(null);
        if (nexus != null && !(mob instanceof Combatant<?>)
                && !mob.getPersistentData().contains(NEXUS_OWNER)) {
            mob.getPersistentData().putString(NEXUS_OWNER, nexus.getUuid().toString());
        }
        return nexus;
    }

    /** Owns the target through the selector so native target goals cannot clear it each tick. */
    private static final class InvasionTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private InvasionTargetGoal(Mob mob) {
            super(mob, LivingEntity.class, 10, true, false,
                    target -> IMCivilianTargetHandler.isSharedInvasionTarget(target));
        }

        @Override
        public boolean canUse() {
            return isActive(mob.getType()) && activeNexus(mob) != null
                    && (mob.getTarget() == null || !mob.getTarget().isAlive())
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return isActive(mob.getType()) && activeNexus(mob) != null
                    && mob.getTarget() != null
                    && IMCivilianTargetHandler.isSharedInvasionTarget(mob.getTarget())
                    && super.canContinueToUse();
        }

        @Override
        protected double getFollowDistance() {
            return 32.0D;
        }
    }

    /** Steers native jumping/flight controls instead of asking them for ground paths. */
    private static final class SpecialMovementNexusGoal extends Goal {
        private final Mob mob;
        private NexusAccess nexus;
        private int attackCooldown;

        private SpecialMovementNexusGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean usesSpecialMovement() {
            return LegacyNexusJumpControl.of(mob) != null
                    || mob.getMoveControl() instanceof FlyingMoveControl
                    || mob.getNavigation() instanceof FlyingPathNavigation
                    || mob instanceof FlyingMob
                    || mob instanceof Ghast || mob instanceof Phantom
                    || mob instanceof Vex || mob instanceof Blaze
                    || mob.isNoGravity();
        }

        @Override public boolean canUse() {
            nexus = activeNexus(mob);
            return nexus != null && usesSpecialMovement()
                    && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            mob.getNavigation().stop();
            attackCooldown = 20;
        }

        @Override public void tick() {
            var target = PosUtils.center(nexus.getOrigin());
            mob.getLookControl().setLookAt(target);
            NexusJumpControl control = LegacyNexusJumpControl.of(mob);
            if (control != null) {
                float yaw = (float)(Mth.atan2(target.z - mob.getZ(),
                        target.x - mob.getX()) * 180.0D / Math.PI) - 90.0F;
                control.invasion$setDirection(yaw, true);
                control.invasion$setWantedMovement(1.0D);
            } else if (mob instanceof Phantom) {
                // Phantom's move control reads this field, not setWantedPosition.
                ((PhantomAccessor)mob).invasion$setMoveTargetPoint(target);
            } else {
                if (mob.getNavigation() instanceof FlyingPathNavigation) {
                    if (mob.getNavigation().isDone()) {
                        mob.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
                    }
                }
                if (mob.getNavigation().isDone()) {
                    mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.0D);
                }
                if (mob instanceof Blaze) {
                    // Blazes use ordinary move control and supply vertical lift themselves.
                    double lift = Mth.clamp(target.y - mob.getY(), -1.0D, 1.0D) * 0.3D;
                    var velocity = mob.getDeltaMovement();
                    mob.setDeltaMovement(velocity.add(0, (lift - velocity.y) * 0.3D, 0));
                }
            }
            if (attackCooldown > 0) attackCooldown--;
            double range = Math.max(4.0D, mob.getBbWidth() * 0.5D + 1.0D);
            if (mob.distanceToSqr(target) <= range * range && attackCooldown == 0) {
                mob.swing(InteractionHand.MAIN_HAND);
                nexus.damage(mob.damageSources().mobAttack(mob), 2);
                attackCooldown = 20;
            }
        }

        @Override public void stop() {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0);
            NexusJumpControl control = LegacyNexusJumpControl.of(mob);
            if (control != null) {
                control.invasion$setWantedMovement(0);
            }
            if (mob instanceof Phantom) {
                ((PhantomAccessor)mob).invasion$setMoveTargetPoint(mob.position());
            }
        }
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

    /** Uses the same locked-column climb used by IM ground-mob navigation. */
    private static final class ClimbNexusLadderGoal extends Goal {
        private final Mob mob;
        private int columnX;
        private int columnZ;
        private int exitY;
        private boolean previousNoGravity;
        private boolean completedExit;
        private int completedColumnX;
        private int completedColumnZ;
        private int completedExitY;

        private ClimbNexusLadderGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (activeNexus(mob) == null || mob.getTarget() != null && mob.getTarget().isAlive()) return false;
            BlockPos ladder = targetedLadder();
            if (ladder == null) return false;
            if (completedExit
                    && ladder.getX() == completedColumnX
                    && ladder.getZ() == completedColumnZ
                    && mob.getY() >= completedExitY - 0.5D) {
                return false;
            }
            if (completedExit && (Math.abs(mob.getX() - (completedColumnX + 0.5D)) > 0.8D
                    || Math.abs(mob.getZ() - (completedColumnZ + 0.5D)) > 0.8D
                    || mob.getY() < completedExitY - 0.5D)) {
                completedExit = false;
            }
            columnX = ladder.getX();
            columnZ = ladder.getZ();
            exitY = ladder.getY();
            BlockPos.MutableBlockPos scan = ladder.mutable();
            for (int offset = 1; offset <= 32; offset++) {
                scan.set(ladder).move(net.minecraft.core.Direction.UP, offset);
                if (!mob.level().getBlockState(scan).is(Blocks.LADDER)) break;
                exitY = scan.getY();
            }
            exitY++;
            return true;
        }

        @Override public boolean canContinueToUse() {
            return activeNexus(mob) != null
                    && (mob.getTarget() == null || !mob.getTarget().isAlive())
                    && mob.getY() < exitY - 0.05D;
        }

        @Override public void start() {
            previousNoGravity = mob.isNoGravity();
            mob.setNoGravity(true);
        }

        @Override public void tick() {
            double targetX = columnX + 0.5D;
            double targetZ = columnZ + 0.5D;
            mob.setPos(targetX, mob.getY(), targetZ);
            mob.setDeltaMovement(0, 0.2D, 0);
            mob.setXxa(0);
            mob.setZza(0);
            mob.fallDistance = 0;
            mob.setShiftKeyDown(false);
            mob.setJumping(false);
            if (mob.getY() >= exitY - 0.05D) {
                mob.setPos(targetX, exitY - 0.05D, targetZ);
                mob.setDeltaMovement(0, 0, 0);
            }
        }

        @Override public void stop() {
            if (mob.getY() >= exitY - 0.3D) {
                BlockPos exit = findSafeExit();
                mob.setPos(exit.getX() + 0.5D, exit.getY(), exit.getZ() + 0.5D);
                mob.setDeltaMovement(0, 0, 0);
                completedExit = true;
                completedColumnX = columnX;
                completedColumnZ = columnZ;
                completedExitY = exitY;
                mob.getNavigation().stop();
                NexusAccess nexus = activeNexus(mob);
                if (nexus != null) {
                    mob.getNavigation().moveTo(nexus.getOrigin().getX() + 0.5D,
                            nexus.getOrigin().getY(), nexus.getOrigin().getZ() + 0.5D, 1.0D);
                }
            }
            mob.setNoGravity(previousNoGravity);
            mob.fallDistance = 0;
        }

        private BlockPos findSafeExit() {
            BlockPos ladderTop = new BlockPos(columnX, exitY - 1, columnZ);
            NexusAccess nexus = activeNexus(mob);
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = ladderTop.relative(direction).above();
                BlockPos support = candidate.below();
                if (!mob.level().getBlockState(support)
                        .isFaceSturdy(mob.level(), support, Direction.UP)) continue;
                double dx = candidate.getX() + 0.5D - mob.getX();
                double dy = candidate.getY() - mob.getY();
                double dz = candidate.getZ() + 0.5D - mob.getZ();
                if (!mob.level().noCollision(mob, mob.getBoundingBox().move(dx, dy, dz))) continue;
                double distance = nexus == null ? 0.0D
                        : candidate.distToCenterSqr(nexus.getOrigin().getX() + 0.5D,
                                nexus.getOrigin().getY(), nexus.getOrigin().getZ() + 0.5D);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
            return best != null ? best : ladderTop.above();
        }

        private BlockPos targetedLadder() {
            BlockPos current = mob.blockPosition();
            if (mob.level().getBlockState(current).is(Blocks.LADDER)) return current;
            net.minecraft.world.level.pathfinder.Path path = mob.getNavigation().getPath();
            if (path == null || path.isDone()) return null;
            BlockPos next = path.getNextNodePos();
            if (mob.level().getBlockState(next).is(Blocks.LADDER)) return next;
            return null;
        }
    }

    private static final class RangedAttackNexusGoal extends Goal {
        private final Mob mob;
        private NexusAccess nexus;
        private int cooldown;

        private RangedAttackNexusGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override public boolean canUse() {
            nexus = activeNexus(mob);
            double distance = nexus == null ? 0 : mob.distanceToSqr(PosUtils.center(nexus.getOrigin()));
            return nexus != null && (mob.getTarget() == null || !mob.getTarget().isAlive())
                    && allowsWeapons(mob.getType(), false) && hasRangedWeapon()
                    && distance > 16 && distance <= 256 && hasClearShot();
        }

        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { cooldown = 0; mob.getNavigation().stop(); }

        @Override public void tick() {
            var target = PosUtils.center(nexus.getOrigin());
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(target);
            if (--cooldown <= 0) {
                SkeletonArrowEntity arrow = new SkeletonArrowEntity(
                        mob.level(), mob, mob.getMainHandItem());
                double dx = target.x - mob.getX();
                double dy = target.y - arrow.getY();
                double dz = target.z - mob.getZ();
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                arrow.shoot(dx, dy + horizontal * 0.2D, dz, 1.1F, 12.0F);
                mob.level().addFreshEntity(arrow);
                cooldown = 65;
            }
        }

        private boolean hasRangedWeapon() {
            return mob.getMainHandItem().is(Items.BOW)
                    || mob.getMainHandItem().is(Items.CROSSBOW)
                    || mob.getMainHandItem().is(Items.TRIDENT);
        }

        private boolean hasClearShot() {
            var target = PosUtils.center(nexus.getOrigin());
            var hit = mob.level().clip(new net.minecraft.world.level.ClipContext(
                    mob.getEyePosition(), target,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, mob));
            return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                    && hit.getBlockPos().equals(nexus.getOrigin());
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

    private record Entry(boolean active, int cost, List<String> themes, Abilities abilities) {}
    private record Abilities(boolean weapons, boolean armor, boolean mining,
            boolean stairing, boolean bridging, boolean towering) {
        private static final Abilities NONE = new Abilities(false, false, false, false, false, false);
        private static final Abilities EQUIPMENT = new Abilities(true, true, false, false, false, false);
    }
    public record WaveMob(EntityType<? extends Mob> type, int cost) {}
}
