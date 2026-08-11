package com.invasion.nexus.wave;

import com.invasion.entity.InvEntities;
import com.invasion.nexus.EntityConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.fabricmc.loader.api.FabricLoader;

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

    private record Option(EntityType<? extends Mob> type, int tier, int flavour, int cost) {
        Option(EntityType<? extends Mob> type, int tier, int cost) {
            this(type, tier, 0, cost);
        }
    }
	private enum Team { ZOMBIE_RUSH, WITHER_GANG, SPIDER_GANG, SPIDER_FAMILY, SKELETON_FAMILY, ENDER_SWARM }
    private static Option o(EntityType<? extends Mob> type, int tier, int cost) { return new Option(type, tier, cost); }
    private static Option o(EntityType<? extends Mob> type, int tier, int flavour, int cost) { return new Option(type, tier, flavour, cost); }

    private static final List<Option> ALL = List.of(
            o(InvEntities.ZOMBIE,1,1), o(InvEntities.ZOMBIE,2,3), o(InvEntities.ZOMBIE,3,6),
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
            o(InvEntities.ZOMBIE_BUILDER,1,7), o(InvEntities.SILVERFISH,1,3), o(InvEntities.ENDERMITE,1,4),
            o(InvEntities.WITCH,1,5), o(InvEntities.THROWER,1,5), o(InvEntities.THROWER,2,10), o(InvEntities.IMP,1,5),
            o(InvEntities.BLAZE,1,8), o(InvEntities.BREEZE,1,6), o(InvEntities.PHANTOM,1,5), o(InvEntities.ZOGLIN,1,11),
            o(InvEntities.CREEPER,1,5), o(InvEntities.CREEPER,2,10), o(InvEntities.SLIME,1,4), o(InvEntities.MAGMA_CUBE,1,6),
            o(InvEntities.WITHER,1,110), o(InvEntities.WARDEN,1,100), o(InvEntities.GHAST,1,10),
            o(InvEntities.BURROWER,1,8), o(InvEntities.ENDERMAN,1,5));

    private static final Map<Theme, List<Option>> POOLS = makePools();
    private static Map<Theme, List<Option>> makePools() {
        Map<Theme, List<Option>> pools = new EnumMap<>(Theme.class);
        pools.put(Theme.SPIDER, filter(InvEntities.SPIDER, InvEntities.CAVE_SPIDER, InvEntities.JUMPING_SPIDER, InvEntities.QUEEN_SPIDER));
        pools.put(Theme.FLYING, filter(InvEntities.PHANTOM, InvEntities.GHAST, InvEntities.BREEZE, InvEntities.BLAZE, InvEntities.WITHER));
        pools.put(Theme.NETHER, filter(InvEntities.ZOMBIE_PIGMAN, InvEntities.ZOMBIFIED_PIGLIN, InvEntities.PIGMAN_ENGINEER, InvEntities.BLAZE, InvEntities.IMP, InvEntities.GHAST, InvEntities.ZOGLIN, InvEntities.MAGMA_CUBE, InvEntities.WITHER_SKELETON, InvEntities.WITHER));
        pools.put(Theme.UNDERGROUND, filter(InvEntities.BURROWER, InvEntities.ZOMBIE, InvEntities.ZOMBIE_BUILDER, InvEntities.SKELETON, InvEntities.SPIDER, InvEntities.CAVE_SPIDER, InvEntities.JUMPING_SPIDER, InvEntities.QUEEN_SPIDER, InvEntities.WARDEN, InvEntities.SILVERFISH, InvEntities.SLIME).stream().filter(option -> option.flavour != 2).toList());
        pools.put(Theme.FAST, filter(InvEntities.SILVERFISH, InvEntities.BLAZE, InvEntities.BREEZE, InvEntities.JUMPING_SPIDER, InvEntities.SPEEDY_ZOMBIE, InvEntities.SKELETON, InvEntities.STRAY, InvEntities.BOGGED, InvEntities.PARCHED, InvEntities.PHANTOM));
        pools.put(Theme.SIEGE, filter(InvEntities.THROWER, InvEntities.GHAST, InvEntities.PIGMAN_ENGINEER, InvEntities.CREEPER, InvEntities.ZOMBIE_BUILDER, InvEntities.ENDERMAN, InvEntities.ZOGLIN, InvEntities.ZOMBIE, InvEntities.BURROWER, InvEntities.ENDERMITE));
        pools.put(Theme.RANGED, filter(InvEntities.ZOMBIE, InvEntities.HUSK, InvEntities.DROWNED, InvEntities.ZOMBIE_VILLAGER, InvEntities.SKELETON, InvEntities.STRAY, InvEntities.BOGGED, InvEntities.PARCHED, InvEntities.WITHER_SKELETON, InvEntities.ZOMBIE_PIGMAN, InvEntities.IMP, InvEntities.THROWER, InvEntities.GHAST, InvEntities.BLAZE, InvEntities.BREEZE, InvEntities.WITHER, InvEntities.WITCH));
        pools.put(Theme.ARMORED, ALL.stream().filter(option -> option.type != InvEntities.WARDEN).toList());
        pools.put(Theme.SWARM, ALL.stream().filter(option -> option.cost <= 5 && option.type != InvEntities.ENDERMITE && option.flavour != 2).toList());
        pools.put(Theme.MIXED, ALL.stream().filter(option -> option.type != InvEntities.WITHER && option.type != InvEntities.WARDEN).toList());
        pools.put(Theme.RANDOM, pools.get(Theme.MIXED));
        pools.put(Theme.RANDOMHELL, ALL);
        return pools;
    }

    @SafeVarargs private static List<Option> filter(EntityType<? extends Mob>... types) {
        return ALL.stream().filter(o -> java.util.Arrays.asList(types).contains(o.type)).toList();
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
        List<Option> pool = POOLS.get(theme);
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
		if (index == 0 && hasFreeEngineer(theme)) {
			purchases.add(new Purchase(InvEntities.PIGMAN_ENGINEER, 1, 0, rollRules(theme, wave, random)));
		}
        if (wave >= 15 && wave % 5 == 0 && index == phaseCount - 1) {
            EntityType<? extends Mob> boss = random.nextBoolean() ? InvEntities.WITHER : InvEntities.WARDEN;
            purchases.add(new Purchase(boss, 1, 0, rollRules(theme, wave, random)));
        }
        return new Phase(theme, List.copyOf(purchases));
    }

	private static boolean hasFreeEngineer(Theme theme) {
		return switch (theme) {
			case SWARM, ARMORED, UNDERGROUND, NETHER, SIEGE, FAST, MIXED, RANDOM, RANDOMHELL -> true;
			default -> false;
		};
	}

    private static int effectiveCost(Theme theme, Option option) {
        if (theme == Theme.RANDOMHELL) return option.cost >= 100 ? 20 : 5;
        return theme == Theme.RANDOM ? 5 : option.cost;
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
		if (!FabricLoader.getInstance().isModLoaded("tinyskeletons")) return result.stream().filter(team -> team != Team.SKELETON_FAMILY).toList();
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
        int variantChance = Math.min(50, 3 + wave);
        if (type == InvEntities.ZOMBIE && base.flavour == 0 && random.nextInt(100) < variantChance) {
            List<EntityType<? extends Mob>> variants = List.of(InvEntities.HUSK, InvEntities.DROWNED, InvEntities.ZOMBIE_VILLAGER, InvEntities.SPEEDY_ZOMBIE);
            type = variants.get(random.nextInt(variants.size()));
        } else if (type == InvEntities.SKELETON && random.nextInt(100) < variantChance) {
            List<EntityType<? extends Mob>> variants = List.of(InvEntities.STRAY, InvEntities.BOGGED, InvEntities.PARCHED, InvEntities.WITHER_SKELETON);
            type = variants.get(random.nextInt(variants.size()));
        } else if (type == InvEntities.CREEPER && base.tier == 1 && random.nextInt(100) < Math.min(100, wave)) {
            return new Purchase(type, 2, cost, rollRules(theme, wave, random));
        } else if (type == InvEntities.ZOMBIE_PIGMAN && random.nextInt(5) == 0) {
            type = InvEntities.ZOMBIFIED_PIGLIN;
        }
        // Tiny Skeletons are deliberately only considered when the optional mod is present.
        if (type == InvEntities.SKELETON && FabricLoader.getInstance().isModLoaded("tinyskeletons")) {
            // The compatibility entity is selected by its own integration; preserving this roll in the plan is future-proof.
            random.nextInt(100);
        }
        return new Purchase(type, base.tier, base.flavour, cost, rollRules(theme, wave, random));
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
