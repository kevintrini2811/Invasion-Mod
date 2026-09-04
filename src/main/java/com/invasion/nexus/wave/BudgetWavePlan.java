package com.invasion.nexus.wave;

import com.invasion.entity.InvEntities;
import com.invasion.nexus.EntityConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.ModList;
import com.invasion.compat.MutantMonstersCompatibility;
import com.invasion.compat.FriendsAndFoesCompatibility;
import com.invasion.compat.ConfiguredModMobs;

/** A complete, persistent purchase plan for one invasion wave. */
public final class BudgetWavePlan {
    public static final int RULE_ARMORED = 1;
    public static final int RULE_RANGED = 2;
    public static final int RULE_INFECTED_BONUS = 4;
    public static final int RULE_PLANNED = 8;
    public static final int RULE_WEAPON = 16;
    public static final int RULE_ARMOR = 32;
    public static final int RULE_BABY = 64;

    public enum Theme { SWARM, ARMORED, RANGED, UNDERGROUND, SPIDER, FLYING, NETHER, SIEGE, FAST, MIXED, RANDOM, RANDOMHELL }
	public record ThemeBias(boolean siege, boolean nether, boolean ranged, boolean swarm) {
		public static final ThemeBias NONE = new ThemeBias(false, false, false, false);
	}

    public record Purchase(EntityType<? extends Mob> type, int tier, int flavour, int cost, int rules) {
        public Purchase(EntityType<? extends Mob> type, int tier, int cost, int rules) {
            this(type, tier, 0, cost, rules);
        }

        EntityPattern pattern() {
            return new EntityPattern.Builder(type).addTier(tier, 1)
                    .addFlavour(flavour, 1).rules(rules).build();
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            tag.putInt("tier", tier);
            tag.putInt("flavour", flavour);
            tag.putInt("cost", cost);
            tag.putInt("rules", rules);
            return tag;
        }

        @SuppressWarnings("unchecked")
        static Purchase load(CompoundTag tag) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(tag.getStringOr("type", "invmod:zombie")));
            if (!isWaveSpawnAllowed(type)) type = InvEntities.ZOMBIE;
            return new Purchase((EntityType<? extends Mob>)type, tag.getIntOr("tier", 1),
                    tag.getIntOr("flavour", 0),
                    tag.getIntOr("cost", 1), tag.getIntOr("rules", 0));
        }
    }

    public static boolean isWaveSpawnAllowed(EntityType<?> type) {
        return type != null
                // Guardians are only post-purchase environmental mobs and
                // must never be persisted or submitted as a
                // direct round-budget purchase.
                && type != InvEntities.GUARDIAN
                && type != InvEntities.ELDER_GUARDIAN
                && type != InvEntities.SPIDER_EGG
                && type != InvEntities.WITHER_SKULL
                && type != InvEntities.WITCH_POTION
                && type != InvEntities.TRAP
                && type != InvEntities.SFX
                && type != InvEntities.SPAWN_PROXY
                && type != InvEntities.BOLT
                && type != InvEntities.BOULDER
                && type != InvEntities.SKELETON_ARROW
                && type != InvEntities.THROWN_ITEM
                && type != InvEntities.TNT;
    }

    public record Phase(Theme theme, List<Purchase> purchases) {
        public Wave asWave() {
            var entry = WaveEntry.finite();
            for (Purchase purchase : purchases) entry.entry(purchase.pattern(), 1);
            int duration = Math.max(5_000, purchases.size() * 750);
            return Wave.builder(duration, 0).entry(entry.end(duration - 500)
                    .amount(purchases.size()).granularity(250)).build();
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("theme", theme.name());
            ListTag list = new ListTag();
            purchases.forEach(p -> list.add(p.save()));
            tag.put("purchases", list);
            return tag;
        }

        static Phase load(CompoundTag tag) {
            Theme theme;
            try { theme = Theme.valueOf(tag.getStringOr("theme", "MIXED")); }
            catch (IllegalArgumentException ignored) { theme = Theme.MIXED; }
            List<Purchase> purchases = new ArrayList<>();
            tag.getListOrEmpty("purchases").forEach(value -> purchases.add(Purchase.load((CompoundTag)value)));
            return new Phase(theme, List.copyOf(purchases));
        }
    }

    private record Option(EntityType<? extends Mob> type, int tier, int flavour, int cost, int rules) {
        Option(EntityType<? extends Mob> type, int tier, int cost) {
            this(type, tier, 0, cost, 0);
        }
        Option(EntityType<? extends Mob> type, int tier, int flavour, int cost) {
            this(type, tier, flavour, cost, 0);
        }
    }
	private enum Team { ZOMBIE_RUSH, WITHER_GANG, SPIDER_GANG, SPIDER_FAMILY, SKELETON_FAMILY, ENDER_SWARM }
    private static Option o(EntityType<? extends Mob> type, int tier, int cost) { return new Option(type, tier, cost); }
    private static Option o(EntityType<? extends Mob> type, int tier, int flavour, int cost) { return new Option(type, tier, flavour, cost); }
    private static Option baby(EntityType<? extends Mob> type, int tier, int cost) {
        return new Option(type, tier, 0, cost, RULE_BABY);
    }

    private static final List<Option> ALL = makeAll();

    private static List<Option> makeAll() {
        List<Option> options = new ArrayList<>(List.of(
            o(InvEntities.ZOMBIE,1,1), o(InvEntities.ZOMBIE,2,3), o(InvEntities.ZOMBIE,3,6),
            o(InvEntities.FAT_ZOMBIE,3,30),
            o(InvEntities.ZOMBIE,2,2,5),
            o(InvEntities.HUSK,1,2), o(InvEntities.HUSK,2,4), o(InvEntities.HUSK,3,7),
            o(InvEntities.DROWNED,1,1), o(InvEntities.DROWNED,2,3), o(InvEntities.DROWNED,3,6),
            o(InvEntities.ZOMBIE_VILLAGER,1,1), o(InvEntities.ZOMBIE_VILLAGER,2,3), o(InvEntities.ZOMBIE_VILLAGER,3,6),
            o(InvEntities.SPEEDY_ZOMBIE,1,3), o(InvEntities.SPEEDY_ZOMBIE,2,5), o(InvEntities.SPEEDY_ZOMBIE,3,9),
            o(InvEntities.ZOMBIE_PIGMAN,1,2), o(InvEntities.ZOMBIE_PIGMAN,2,4), o(InvEntities.ZOMBIE_PIGMAN,3,7),
            o(InvEntities.ZOMBIFIED_PIGLIN,1,2), o(InvEntities.ZOMBIFIED_PIGLIN,2,4), o(InvEntities.ZOMBIFIED_PIGLIN,3,7),
            o(InvEntities.SKELETON,1,2), o(InvEntities.STRAY,1,3), o(InvEntities.BOGGED,1,3), o(InvEntities.PARCHED,1,3),
            o(InvEntities.WITHER_SKELETON,1,5), o(InvEntities.SPIDER,1,2), o(InvEntities.CAVE_SPIDER,1,3),
            o(InvEntities.JUMPING_SPIDER,1,3), o(InvEntities.QUEEN_SPIDER,1,8), o(InvEntities.PIGMAN_ENGINEER,1,5),
            o(InvEntities.ZOMBIE_BUILDER,1,7), o(InvEntities.ZOMBIE_MINER,1,9), o(InvEntities.SILVERFISH,1,3), o(InvEntities.ENDERMITE,1,4),
            o(InvEntities.WITCH,1,5), o(InvEntities.THROWER,1,5), o(InvEntities.THROWER,2,10), o(InvEntities.IMP,1,5),
            o(InvEntities.BLAZE,1,8), o(InvEntities.BREEZE,1,6), o(InvEntities.PHANTOM,1,5), o(InvEntities.ZOGLIN,1,15),
            o(InvEntities.CREEPER,1,5), o(InvEntities.CREEPER,2,10), o(InvEntities.SLIME,1,4), o(InvEntities.MAGMA_CUBE,1,6),
            o(InvEntities.WITHER,1,110), o(InvEntities.WARDEN,1,100), o(InvEntities.GHAST,1,20),
            o(InvEntities.BURROWER,1,8), o(InvEntities.ENDERMAN,1,5)));
        options.addAll(List.of(
                baby(InvEntities.ZOMBIE,1,3), baby(InvEntities.ZOMBIE,2,5), baby(InvEntities.ZOMBIE,3,9),
                baby(InvEntities.SPEEDY_ZOMBIE,1,4), baby(InvEntities.SPEEDY_ZOMBIE,2,6), baby(InvEntities.SPEEDY_ZOMBIE,3,10),
                baby(InvEntities.HUSK,1,3), baby(InvEntities.HUSK,2,5), baby(InvEntities.HUSK,3,9),
                baby(InvEntities.DROWNED,1,2), baby(InvEntities.DROWNED,2,4), baby(InvEntities.DROWNED,3,7)));
        if (ModList.get().isLoaded("tinyskeletons")) {
            options.addAll(List.of(
                    baby(InvEntities.SKELETON,1,3), baby(InvEntities.STRAY,1,4),
                    baby(InvEntities.BOGGED,1,4), baby(InvEntities.PARCHED,1,4),
                    baby(InvEntities.WITHER_SKELETON,1,6)));
        }
        MutantMonstersCompatibility.freeBossTypes().forEach(
                type -> options.add(o(type, 1, 200)));
        EntityType<? extends Mob> wildfire =
                FriendsAndFoesCompatibility.imWildfireType();
        if (wildfire != null) options.add(o(wildfire, 1, 200));
        EntityType<? extends Mob> spiderPig =
                MutantMonstersCompatibility.mobType("spider_pig");
        EntityType<? extends Mob> creeperMinion =
                MutantMonstersCompatibility.mobType("creeper_minion");
        if (spiderPig != null) options.add(o(spiderPig, 1, 25));
        if (creeperMinion != null) options.add(o(creeperMinion, 1, 5));
        return List.copyOf(options);
    }

    /** Selects any purchasable IM combat mob without exposing it as a purchase. */
    public static EntityConstruct randomMobConstruct(RandomSource random) {
        Option option = ALL.get(random.nextInt(ALL.size()));
        return new EntityConstruct(
                option.type(), 0, option.tier(), option.flavour(),
                1.0F, option.rules(), 360);
    }

    private static final Map<Theme, List<Option>> POOLS = makePools();

    public record ConfigMobDefault(Identifier id, int cost, List<String> themes) {}

    /** Canonical JSON defaults for every directly purchasable IM mob type. */
    public static List<ConfigMobDefault> configMobDefaults() {
        Map<EntityType<? extends Mob>, Integer> costs = new IdentityHashMap<>();
        ALL.forEach(option -> costs.merge(option.type, option.cost, Math::min));
        Map<EntityType<? extends Mob>, LinkedHashSet<String>> themes = new IdentityHashMap<>();
        POOLS.forEach((theme, options) -> options.forEach(option -> themes
                .computeIfAbsent(option.type, ignored -> new LinkedHashSet<>())
                .add(theme.name())));
        addConfigAlias(costs, themes, InvEntities.MYSTERY_ZOMBIE, InvEntities.SPEEDY_ZOMBIE);
        addConfigAlias(costs, themes, InvEntities.GUARDIAN, InvEntities.DROWNED);
        addConfigAlias(costs, themes, InvEntities.ELDER_GUARDIAN, InvEntities.DROWNED);
        return costs.entrySet().stream()
                .map(entry -> new ConfigMobDefault(
                        BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()), entry.getValue(),
                        List.copyOf(themes.getOrDefault(entry.getKey(), new LinkedHashSet<>()))))
                .filter(entry -> entry.id() != null)
                .sorted(Comparator.comparing(entry -> entry.id().toString()))
                .toList();
    }

    private static void addConfigAlias(
            Map<EntityType<? extends Mob>, Integer> costs,
            Map<EntityType<? extends Mob>, LinkedHashSet<String>> themes,
            EntityType<? extends Mob> type, EntityType<? extends Mob> source) {
        Integer cost = costs.get(source);
        if (cost == null) return;
        costs.putIfAbsent(type, cost);
        themes.putIfAbsent(type, new LinkedHashSet<>(
                themes.getOrDefault(source, new LinkedHashSet<>())));
    }
    private static Map<Theme, List<Option>> makePools() {
        Map<Theme, List<Option>> pools = new EnumMap<>(Theme.class);
        pools.put(Theme.SPIDER, select(o -> isType(o, InvEntities.SPIDER, InvEntities.CAVE_SPIDER,
                InvEntities.JUMPING_SPIDER, InvEntities.QUEEN_SPIDER)));
        pools.put(Theme.FLYING, select(o -> isType(o, InvEntities.PHANTOM, InvEntities.GHAST,
                InvEntities.BREEZE, InvEntities.BLAZE, InvEntities.WITHER)));
        pools.put(Theme.NETHER, select(o -> isType(o, InvEntities.ZOMBIE_PIGMAN,
                InvEntities.ZOMBIFIED_PIGLIN, InvEntities.PIGMAN_ENGINEER, InvEntities.BLAZE,
                InvEntities.IMP, InvEntities.GHAST, InvEntities.ZOGLIN, InvEntities.MAGMA_CUBE,
                InvEntities.WITHER_SKELETON, InvEntities.WITHER) && !isBaby(o)));
        pools.put(Theme.UNDERGROUND, select(o ->
                isType(o, InvEntities.BURROWER, InvEntities.ZOMBIE_BUILDER, InvEntities.ZOMBIE_MINER,
                        InvEntities.SPIDER, InvEntities.CAVE_SPIDER, InvEntities.JUMPING_SPIDER,
                        InvEntities.QUEEN_SPIDER, InvEntities.WARDEN, InvEntities.SILVERFISH, InvEntities.SLIME)
                || o.type == InvEntities.ZOMBIE && o.flavour == 0
                || o.type == InvEntities.SKELETON));
        pools.put(Theme.FAST, select(o ->
                isType(o, InvEntities.SILVERFISH, InvEntities.BLAZE, InvEntities.BREEZE,
                        InvEntities.JUMPING_SPIDER, InvEntities.PHANTOM)
                || o.type == InvEntities.SPEEDY_ZOMBIE && o.tier <= 2
                || isBaby(o) && o.tier <= 2 && isType(o, InvEntities.ZOMBIE,
                        InvEntities.HUSK, InvEntities.DROWNED, InvEntities.SPEEDY_ZOMBIE)
                || isBaby(o) && isType(o, InvEntities.SKELETON, InvEntities.STRAY,
                        InvEntities.BOGGED, InvEntities.PARCHED)));
        pools.put(Theme.SIEGE, select(o ->
                isType(o, InvEntities.THROWER, InvEntities.GHAST, InvEntities.PIGMAN_ENGINEER,
                        InvEntities.CREEPER, InvEntities.ZOMBIE_BUILDER, InvEntities.ENDERMAN,
                        InvEntities.ZOGLIN, InvEntities.BURROWER)
                || !isBaby(o) && o.type == InvEntities.ZOMBIE
                        && (o.tier == 1 && o.flavour == 0 || o.flavour == 2)));
        pools.put(Theme.RANGED, select(o ->
                isType(o, InvEntities.HUSK, InvEntities.DROWNED, InvEntities.ZOMBIE_VILLAGER,
                        InvEntities.ZOMBIE_PIGMAN) && !isBaby(o)
                || o.type == InvEntities.ZOMBIE && o.flavour != 2
                || isType(o, InvEntities.SKELETON, InvEntities.STRAY, InvEntities.BOGGED,
                        InvEntities.PARCHED, InvEntities.WITHER_SKELETON)
                || isType(o, InvEntities.IMP, InvEntities.THROWER, InvEntities.GHAST,
                        InvEntities.BLAZE, InvEntities.BREEZE, InvEntities.WITHER, InvEntities.WITCH)
                || o.type == InvEntities.ZOMBIE && o.flavour == 2));
        EntityType<? extends Mob> spiderPig = MutantMonstersCompatibility.mobType("spider_pig");
        EntityType<? extends Mob> mutantZombie = MutantMonstersCompatibility.mobType("mutant_zombie");
        EntityType<? extends Mob> mutantCreeper = MutantMonstersCompatibility.mobType("mutant_creeper");
        EntityType<? extends Mob> mutantSkeleton = MutantMonstersCompatibility.mobType("mutant_skeleton");
        EntityType<? extends Mob> mutantEnderman = MutantMonstersCompatibility.mobType("mutant_enderman");
        EntityType<? extends Mob> wildfire =
                FriendsAndFoesCompatibility.imWildfireType();
        pools.put(Theme.ARMORED, select(o -> !isBaby(o) && (
                isType(o, InvEntities.ZOMBIE, InvEntities.HUSK, InvEntities.DROWNED,
                        InvEntities.ZOMBIE_VILLAGER, InvEntities.SKELETON, InvEntities.STRAY,
                        InvEntities.BOGGED, InvEntities.PARCHED, InvEntities.WITHER_SKELETON,
                        InvEntities.ZOMBIE_PIGMAN, InvEntities.PIGMAN_ENGINEER,
                        InvEntities.ZOMBIE_BUILDER, InvEntities.GHAST, InvEntities.BLAZE,
                        InvEntities.CREEPER, InvEntities.ENDERMAN, InvEntities.SPIDER,
                        InvEntities.CAVE_SPIDER, InvEntities.JUMPING_SPIDER, InvEntities.QUEEN_SPIDER))));
        pools.put(Theme.SWARM, select(o ->
                !isBaby(o) && (o.tier == 1 && isType(o, InvEntities.ZOMBIE, InvEntities.HUSK,
                        InvEntities.DROWNED, InvEntities.ZOMBIE_VILLAGER, InvEntities.ZOMBIE_PIGMAN,
                        InvEntities.ZOMBIFIED_PIGLIN)
                        || isType(o, InvEntities.SKELETON, InvEntities.STRAY, InvEntities.BOGGED,
                                InvEntities.PARCHED, InvEntities.SPIDER, InvEntities.PIGMAN_ENGINEER,
                                InvEntities.ZOMBIE_BUILDER, InvEntities.SILVERFISH,
                                InvEntities.WITCH, InvEntities.FAT_ZOMBIE))
                || isBaby(o) && o.tier == 1 && isType(o, InvEntities.ZOMBIE, InvEntities.HUSK,
                        InvEntities.DROWNED, InvEntities.SKELETON, InvEntities.STRAY,
                        InvEntities.BOGGED, InvEntities.PARCHED)));
        pools.put(Theme.MIXED, ALL);
        pools.put(Theme.RANDOM, ALL);
        pools.put(Theme.RANDOMHELL, ALL);
        addToPool(pools, Theme.FAST, spiderPig);
        addToPool(pools, Theme.SPIDER, spiderPig);
        addToPool(pools, Theme.UNDERGROUND, mutantZombie);
        addToPool(pools, Theme.UNDERGROUND, mutantCreeper);
        addToPool(pools, Theme.UNDERGROUND, mutantSkeleton);
        addToPool(pools, Theme.UNDERGROUND, mutantEnderman);
        addToPool(pools, Theme.RANGED, mutantSkeleton);
        addToPool(pools, Theme.FLYING, wildfire);
        addToPool(pools, Theme.NETHER, wildfire);
        return pools;
    }

    private static void addToPool(Map<Theme, List<Option>> pools,
            Theme theme, EntityType<? extends Mob> type) {
        if (type == null) return;
        List<Option> updated = new ArrayList<>(pools.get(theme));
        ALL.stream().filter(option -> option.type == type).findFirst()
                .ifPresent(updated::add);
        pools.put(theme, List.copyOf(updated));
    }

    private static List<Option> select(Predicate<Option> predicate) {
        return ALL.stream().filter(predicate).toList();
    }

    @SafeVarargs
    private static boolean isType(Option option, EntityType<? extends Mob>... types) {
        for (EntityType<? extends Mob> type : types) {
            if (option.type == type) return true;
        }
        return false;
    }

    private static boolean isBaby(Option option) {
        return (option.rules & RULE_BABY) != 0;
    }

    private final int wave;
    private final List<Phase> phases;
    private int phaseIndex;

    private BudgetWavePlan(int wave, List<Phase> phases, int phaseIndex) {
        this.wave = wave; this.phases = phases; this.phaseIndex = phaseIndex;
    }

    public static BudgetWavePlan generate(int wave, int phaseCount, RandomSource random) {
		return generate(wave, phaseCount, random, ThemeBias.NONE);
	}

	public static BudgetWavePlan generate(int wave, int phaseCount, RandomSource random, ThemeBias bias) {
        List<Phase> phases = new ArrayList<>();
		for (int i = 0; i < phaseCount; i++) phases.add(generatePhase(wave, i, phaseCount, random, bias));
        return new BudgetWavePlan(wave, List.copyOf(phases), 0);
    }

    private static Phase generatePhase(int wave, int index, int phaseCount, RandomSource random, ThemeBias bias) {
        List<Theme> choices = new ArrayList<>(List.of(Theme.values()));
        choices.remove(Theme.RANDOMHELL);
		if (bias.siege) choices.add(Theme.SIEGE);
		if (bias.nether) choices.add(Theme.NETHER);
		if (bias.ranged) choices.add(Theme.RANGED);
		if (bias.swarm) choices.add(Theme.SWARM);
        if (wave >= 20 && wave % 5 == 0 && random.nextBoolean()) choices.add(Theme.RANDOMHELL);
        Theme theme = choices.get(random.nextInt(choices.size()));
        List<Option> pool = poolFor(theme);
        int budget = wave * 10;
        int remaining = budget;
        List<Purchase> purchases = new ArrayList<>();
        while (remaining > 0) {
			int teamBudget = remaining;
			List<Team> teams = teams(theme).stream().filter(team -> teamCost(theme, team) <= teamBudget).toList();
			if (!teams.isEmpty() && random.nextInt(5) == 0) {
				Team team = teams.get(random.nextInt(teams.size()));
				remaining -= teamCost(theme, team);
				addTeam(purchases, team, wave, theme, random);
				continue;
			}
            int availableBudget = remaining;
            List<Option> affordable = pool.stream().filter(o -> effectiveCost(theme, o) <= availableBudget).toList();
            if (affordable.isEmpty()) break;
            Option option = affordable.get(random.nextInt(affordable.size()));
            int cost = effectiveCost(theme, option);
            purchases.add(rollVariant(option, wave, theme, random, cost));
            remaining -= cost;
        }
        while (remaining-- > 0) purchases.add(new Purchase(InvEntities.ZOMBIE, 1, 1, rollRules(theme, wave, random)));
        while (purchases.size() < wave * 5) purchases.add(new Purchase(InvEntities.ZOMBIE, 1, 0, rollRules(theme, wave, random)));
        purchases.add(rollVariant(o(InvEntities.PIGMAN_ENGINEER, 1, 5),
                wave, theme, random, 0));
        if (wave >= 15 && wave % 5 == 0 && index == phaseCount - 1) {
            List<EntityType<? extends Mob>> bosses = new ArrayList<>(
                    List.of(InvEntities.WITHER, InvEntities.WARDEN));
            bosses.addAll(MutantMonstersCompatibility.freeBossTypes());
            EntityType<? extends Mob> wildfire = FriendsAndFoesCompatibility.imWildfireType();
            if (wildfire != null) bosses.add(wildfire);
            EntityType<? extends Mob> boss = bosses.get(random.nextInt(bosses.size()));
            purchases.add(new Purchase(boss, 1, 0, rollRules(theme, wave, random)));
        }
        return new Phase(theme, List.copyOf(purchases));
    }

    private static List<Option> poolFor(Theme theme) {
        List<Option> pool = POOLS.get(theme);
        List<Option> expanded = new ArrayList<>(pool);
        ConfiguredModMobs.activeWaveMobs(theme).forEach(
                mob -> expanded.add(o(mob.type(), 1, mob.cost())));
        return List.copyOf(expanded);
    }

    private static int effectiveCost(Theme theme, Option option) {
        if (theme == Theme.RANDOM && isBoss(option.type)) {
            return option.cost;
        }
        if (theme == Theme.RANDOMHELL) return isBoss(option.type) ? 20 : 5;
        return theme == Theme.RANDOM ? 5 : option.cost;
    }

    private static boolean isBoss(EntityType<? extends Mob> type) {
        return type == InvEntities.WITHER || type == InvEntities.WARDEN
                || MutantMonstersCompatibility.freeBossTypes().contains(type)
                || type == FriendsAndFoesCompatibility.imWildfireType();
    }

	private static List<Team> teams(Theme theme) {
		List<Team> result = switch (theme) {
			case SWARM -> List.of(Team.ZOMBIE_RUSH, Team.ENDER_SWARM);
			case ARMORED -> List.of(Team.ZOMBIE_RUSH, Team.WITHER_GANG, Team.SPIDER_GANG, Team.SPIDER_FAMILY, Team.SKELETON_FAMILY);
			case RANGED -> List.of(Team.ZOMBIE_RUSH, Team.WITHER_GANG, Team.SKELETON_FAMILY);
			case UNDERGROUND -> List.of(Team.ZOMBIE_RUSH);
			case SPIDER -> List.of(Team.SPIDER_GANG, Team.SPIDER_FAMILY);
			case NETHER -> List.of(Team.WITHER_GANG);
			case SIEGE -> List.of(Team.ENDER_SWARM);
			case MIXED, RANDOM, RANDOMHELL -> List.of(Team.values());
			default -> List.of();
		};
		if (!ModList.get().isLoaded("tinyskeletons")) return result.stream().filter(team -> team != Team.SKELETON_FAMILY).toList();
		return result;
	}

	private static int teamCost(Theme theme, Team team) {
		if (theme == Theme.RANDOM || theme == Theme.RANDOMHELL) return 5;
		return switch (team) {
			case ZOMBIE_RUSH, SPIDER_FAMILY, SKELETON_FAMILY -> 10;
			case WITHER_GANG -> 12;
			case SPIDER_GANG -> 14;
			case ENDER_SWARM -> 15;
		};
	}

	private static void addTeam(List<Purchase> out, Team team, int wave, Theme theme, RandomSource random) {
		java.util.function.BiConsumer<Option, Integer> add = (option, count) -> {
			for (int i = 0; i < count; i++) out.add(rollVariant(option, wave, theme, random, 0));
		};
		switch (team) {
			case ZOMBIE_RUSH -> { add.accept(o(InvEntities.ZOMBIE, 1, 1), 8); add.accept(o(InvEntities.ZOMBIE, 2, 3), 2); }
			case WITHER_GANG -> add.accept(o(InvEntities.WITHER_SKELETON, 1, 5), 3);
			case SPIDER_GANG -> { add.accept(o(InvEntities.SPIDER,1,2),1); add.accept(o(InvEntities.CAVE_SPIDER,1,3),1); add.accept(o(InvEntities.JUMPING_SPIDER,1,3),1); add.accept(o(InvEntities.QUEEN_SPIDER,1,8),1); }
			case SPIDER_FAMILY -> { add.accept(o(InvEntities.SPIDER,1,2),1); add.accept(o(InvEntities.JUMPING_SPIDER,1,3),1); add.accept(o(InvEntities.CAVE_SPIDER,1,3),2); }
			case SKELETON_FAMILY -> {
				add.accept(o(InvEntities.SKELETON,1,2),1);
				add.accept(o(InvEntities.PARCHED,1,3),1);
				for (int i = 0; i < 2; i++) {
					Purchase baby = rollVariant(o(InvEntities.SKELETON,1,3), wave, theme, random, 0);
					out.add(new Purchase(baby.type, baby.tier, baby.cost, baby.rules | RULE_BABY));
				}
			}
			case ENDER_SWARM -> { add.accept(o(InvEntities.ENDERMAN,1,5),2); add.accept(o(InvEntities.ENDERMITE,1,4),3); }
		}
	}

    private static int rollRules(Theme theme, int wave, RandomSource random) {
        int rules = RULE_PLANNED | (theme == Theme.ARMORED ? RULE_ARMORED : 0)
                | (theme == Theme.RANGED ? RULE_RANGED : 0)
                | (theme == Theme.SWARM || theme == Theme.SIEGE ? RULE_INFECTED_BONUS : 0);
		if (random.nextInt(100) < Math.min(100, wave)) rules |= RULE_WEAPON;
		if (random.nextInt(100) < Math.min(100, wave)) rules |= RULE_ARMOR;
		if (random.nextInt(100) < Math.min(20, wave)) rules |= RULE_BABY;
		return rules;
    }

    private static Purchase rollVariant(Option base, int wave, Theme theme, RandomSource random, int cost) {
        EntityType<? extends Mob> type = base.type;
        int variantChance = Math.min(50, 5 + wave);
        int rules = rollRules(theme, wave, random) | base.rules;
        if (type == InvEntities.ZOMBIE && base.flavour == 0
                && random.nextInt(100) < variantChance) {
            List<EntityType<? extends Mob>> variants = List.of(InvEntities.HUSK, InvEntities.DROWNED, InvEntities.ZOMBIE_VILLAGER, InvEntities.SPEEDY_ZOMBIE);
            type = variants.get(random.nextInt(variants.size()));
        } else if (type == InvEntities.SKELETON && random.nextInt(100) < variantChance) {
            List<EntityType<? extends Mob>> variants = List.of(InvEntities.STRAY, InvEntities.BOGGED, InvEntities.PARCHED, InvEntities.WITHER_SKELETON);
            type = variants.get(random.nextInt(variants.size()));
        } else if (type == InvEntities.CREEPER && base.tier == 1 && random.nextInt(100) < Math.min(100, wave)) {
            return new Purchase(type, 2, base.flavour, cost, rules);
        } else if (type == InvEntities.ZOMBIE_PIGMAN && random.nextInt(5) == 0) {
            type = InvEntities.ZOMBIFIED_PIGLIN;
        } else if (type == InvEntities.PIGMAN_ENGINEER
                && random.nextInt(100) < Math.min(100, 1 + wave)) {
            type = random.nextBoolean() ? InvEntities.ZOMBIE_MINER : InvEntities.ZOMBIE_BUILDER;
        }
        return new Purchase(type, base.tier, base.flavour, cost, rules);
    }

    public int wave() { return wave; }
    public int phaseIndex() { return phaseIndex; }
    public int phaseCount() { return phases.size(); }
    public Phase currentPhase() { return phases.get(phaseIndex); }
    public boolean advance() { if (phaseIndex + 1 >= phases.size()) return false; phaseIndex++; return true; }
    public int totalMobs() { return phases.stream().mapToInt(p -> p.purchases.size()).sum(); }

    public CompoundTag save(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag(); tag.putInt("wave", wave); tag.putInt("phase", phaseIndex);
        ListTag list = new ListTag(); phases.forEach(p -> list.add(p.save())); tag.put("phases", list); return tag;
    }

    public static BudgetWavePlan load(CompoundTag tag, HolderLookup.Provider lookup) {
        List<Phase> phases = new ArrayList<>();
        tag.getListOrEmpty("phases").forEach(value -> phases.add(Phase.load((CompoundTag)value)));
        return new BudgetWavePlan(tag.getIntOr("wave", 1), List.copyOf(phases),
                Math.clamp(tag.getIntOr("phase", 0), 0, Math.max(0, phases.size() - 1)));
    }
}
