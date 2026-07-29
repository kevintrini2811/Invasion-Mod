package com.invasion.nexus.wave;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import com.invasion.InvasionMod;
import com.invasion.entity.InvEntities;
import org.jetbrains.annotations.Nullable;

public interface EntityPatterns {
    Map<Identifier, PatternType> REGISTRY = new HashMap<>();

    // ================================
    // EXTERNE MOBS AUS ANDEREN MODS
    // ================================

    // ===== Mutant Monsters (Fuzs) =====

    EntityPattern Gigant = registerExternal(
            "giant",
            "minecraft",
            "giant",
            0.3F
    );

    /**
     * Registriert ein EntityPattern für einen Mob aus einer anderen Mod, falls vorhanden.
     * Gibt null zurück, wenn die Entity-ID nicht existiert.
     */

    @SuppressWarnings("unchecked")
    private static EntityPattern registerExternal(String name, String modid, String entityName, float spawnWeight) {
        Identifier entityId = Identifier.fromNamespaceAndPath(modid, entityName);

        // Versuchen, den Typ direkt zu holen
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId)
                .map(net.minecraft.core.Holder.Reference::value)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityId));

        Identifier resolvedId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        InvasionMod.LOGGER.debug("[EntityPatterns] registerExternal {} -> resolvedId={}", entityId, resolvedId);

        // Wenn der Registry-Eintrag wirklich nicht existiert (d. h. wir kriegen NICHT unsere gewünschte ID zurück)
        if (!entityId.equals(resolvedId)) {
            InvasionMod.LOGGER.warn("[EntityPatterns] Mod-Mob {} nicht gefunden (resolvedId={})), Pattern '{}' wird auf null gesetzt.",
                    entityId, resolvedId, name);
            return null;
        }

        EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) type;
        InvasionMod.LOGGER.debug("[EntityPatterns] Externen Mob {} als Pattern '{}' registriert (weight={})",
                entityId, name, spawnWeight);

        return register(name, new EntityPattern.Builder(mobType), spawnWeight);
    }

    public static boolean isExternalInvasionMob(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return false;

        // Mutant Monsters
        if (id.getNamespace().equals("mutantmonsters")) {
            return true;
        }

        // Vanilla Giant als “externer” Invasions-Mob
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("giant")) {
            return true;
        }

        // später weitere externe Mods hier ergänzen
        return false;
    }



    // ================================
    // INTERNE INVASION-MOBS (DEINE)
    // ================================

    EntityPattern ZOMBIE_T1_ANY = register("zombie_t1_any", new EntityPattern.Builder(InvEntities.ZOMBIE).addTier(1, 1).addFlavour(0, 3).addFlavour(1, 1), 1);
    EntityPattern ZOMBIE_T2_ANY_BASIC = register("zombie_t2_any_basic", new EntityPattern.Builder(InvEntities.ZOMBIE).addTier(2, 1).addFlavour(0, 2).addFlavour(1, 1).addFlavour(2, 0.4F), 1);
    EntityPattern ZOMBIE_T2_PLAIN = register("zombie_t2_plain",  new EntityPattern.Builder(InvEntities.ZOMBIE).addTier(2, 1).addFlavour(0, 1));
    EntityPattern ZOMBIE_T2_TAR = register("zombie_t2_tar", new EntityPattern.Builder(InvEntities.ZOMBIE).addTier(2, 1).addFlavour(2, 1).addTexture(5, 1));
    EntityPattern ZOMBIE_T3_ANY = register("zombie_t3_any", new EntityPattern.Builder(InvEntities.ZOMBIE).addTier(3, 1).addTexture(0, 1));

    EntityPattern ZOMBIE_PIGMAN_T1_ANY = register("zombie_pigman_t1_any", new EntityPattern.Builder(InvEntities.ZOMBIE_PIGMAN).addTier(1, 1).addFlavour(0, 1));
    EntityPattern ZOMBIE_PIGMAN_T2_ANY = register("zombie_pigman_t2_any", new EntityPattern.Builder(InvEntities.ZOMBIE_PIGMAN).addTier(2, 1).addFlavour(0, 1));
    EntityPattern ZOMBIE_PIGMAN_T3_ANY = register("zombie_pigman_t3_any", new EntityPattern.Builder(InvEntities.ZOMBIE_PIGMAN).addTier(3, 1).addFlavour(0, 1));

    EntityPattern CAVE_SPIDER_T1 = register("cave_spider_t1", new EntityPattern.Builder(InvEntities.CAVE_SPIDER), 0.35F);
    EntityPattern SPIDER_T1_ANY = register("spider_t1_any", new EntityPattern.Builder(InvEntities.SPIDER)
            .addType(InvEntities.CAVE_SPIDER, 0.35F), 0.5F);
    EntityPattern SPIDER_T2_ANY = register("spider_t2_any", new EntityPattern.Builder(InvEntities.SPIDER)
            .addType(InvEntities.JUMPING_SPIDER, 1)
            .addType(InvEntities.CAVE_SPIDER, 0.5F)
            .addType(InvEntities.QUEEN_SPIDER, 0.5F));
    EntityPattern SPIDER_T3_ANY = register("spider_t3_any", new EntityPattern.Builder(InvEntities.JUMPING_SPIDER)
            .addType(InvEntities.SPIDER, 0.5F)
            .addType(InvEntities.CAVE_SPIDER, 0.35F)
            .addType(InvEntities.QUEEN_SPIDER, 1));

    EntityPattern PIGMAN_ENGINEER_T1_ANY = register("pigman_engineer_t1_any", new EntityPattern.Builder(InvEntities.PIGMAN_ENGINEER).addTier(1, 1));

    EntityPattern SKELETON_T1_ANY = register("skeleton_t1_any", new EntityPattern.Builder(InvEntities.SKELETON).addTier(1, 1));

    EntityPattern THROWER_T1 = register("thrower_t1_any", new EntityPattern.Builder(InvEntities.THROWER).addTier(1, 1));
    EntityPattern THROWER_T2 = register("thrower_t2_any", new EntityPattern.Builder(InvEntities.THROWER).addTier(2, 1));

    EntityPattern BURROWER = register("burrower", new EntityPattern.Builder(InvEntities.BURROWER).addTier(1, 1));

    EntityPattern CREEPER_T1_BASIC = register("creeper_t1_basic", new EntityPattern.Builder(InvEntities.CREEPER).addTier(1, 1));

    EntityPattern IMP_T1 = register("imp_t1", new EntityPattern.Builder(InvEntities.IMP).addTier(1, 1));
    EntityPattern ENDERMAN_T1 = register("enderman_t1", new EntityPattern.Builder(InvEntities.ENDERMAN).addTier(1, 1));

    static EntityPattern register(String name, EntityPattern.Builder builder) {
        return register(name, builder, 0);
    }

    static EntityPattern register(String name, EntityPattern.Builder builder, float spawnWeight) {
        Identifier id = InvasionMod.id(name);
        EntityPattern pattern = builder.build();
        REGISTRY.put(id, new PatternType(id, pattern, spawnWeight));
        return pattern;
    }

    static EntityPattern getPattern(Identifier id) {
        return getKey(id).map(PatternType::pattern).orElse(EntityPatterns.ZOMBIE_T1_ANY);
    }

    static Optional<PatternType> getKey(Identifier id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    static boolean isPatternNameValid(Identifier id) {
        return REGISTRY.containsKey(id);
    }

    record PatternType(Identifier id, EntityPattern pattern, float defaultSpawnWeight) {
        public float getNightMobSpawnWeight() {
            return InvasionMod.getConfig().getPropertyValueFloat("nm-spawnpool1-slot-" + id + "-weight", defaultSpawnWeight);
        }
    }
    // ================================================
    // Lazy Getter für Mutant Monsters (sicher bei Load)
    // ================================================


    @Nullable
    static EntityPattern getMutantZombie() {
        return MutantPatterns.MUTANT_ZOMBIE;
    }

    @Nullable
    static EntityPattern getMutantCreeper() {
        return MutantPatterns.MUTANT_CREEPER;
    }

    @Nullable
    static EntityPattern getMutantSkeleton() {
        return MutantPatterns.MUTANT_SKELETON;
    }

    @Nullable
    static EntityPattern getMutantEnderman() {
        return MutantPatterns.MUTANT_ENDERMAN;
    }

    @Nullable
    static EntityPattern getSpiderPig() {
        return MutantPatterns.SPIDER_PIG;
    }

    // ================================================
    // Innere Klasse, die Mutanten erst später lädt
    // ================================================
    final class MutantPatterns {

        static final @Nullable EntityPattern MUTANT_ZOMBIE;
        static final @Nullable EntityPattern MUTANT_CREEPER;
        static final @Nullable EntityPattern MUTANT_SKELETON;
        static final @Nullable EntityPattern MUTANT_ENDERMAN;
        static final @Nullable EntityPattern SPIDER_PIG;

        static {
            MUTANT_ZOMBIE = create("mutant_zombie", "mutantmonsters", "mutant_zombie", 0.5F);
            MUTANT_CREEPER = create("mutant_creeper", "mutantmonsters", "mutant_creeper", 0.4F);
            MUTANT_SKELETON = create("mutant_skeleton", "mutantmonsters", "mutant_skeleton", 0.4F);
            MUTANT_ENDERMAN = create("mutant_enderman", "mutantmonsters", "mutant_enderman", 0.3F);
            SPIDER_PIG = create("spider_pig", "mutantmonsters", "spider_pig", 0.3F);
        }

        @SuppressWarnings("unchecked")
        private static @Nullable EntityPattern create(String name, String modid, String entityName, float spawnWeight) {
            Identifier entityId = Identifier.fromNamespaceAndPath(modid, entityName);

            var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
            if (opt.isEmpty()) {
                InvasionMod.LOGGER.warn("[EntityPatterns] Mod-Mob {} nicht gefunden, Pattern '{}' bleibt null.", entityId, name);
                return null;
            }

            EntityType<?> type = opt.get();
            EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) type;

            InvasionMod.LOGGER.debug("[EntityPatterns] Externen Mob {} als Pattern '{}' registriert (weight={})",
                    entityId, name, spawnWeight);

            return EntityPatterns.register(name, new EntityPattern.Builder(mobType), spawnWeight);
        }

        private MutantPatterns() {}
    }

}
