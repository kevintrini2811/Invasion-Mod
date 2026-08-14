package com.invasion.compat;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.ModList;

/** Safe entry point that keeps Mutant Monsters classes behind a mod check. */
public final class MutantMonstersCompatibility {
    public static final String MOD_ID = "mutantmonsters";
    public static final List<String> MOB_NAMES = List.of(
            "mutant_zombie", "mutant_creeper", "mutant_skeleton",
            "mutant_enderman", "spider_pig");

    private MutantMonstersCompatibility() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void bootstrapEntities() {
        if (isLoaded()) {
            MutantMonstersEntities.bootstrap();
        }
    }

    public static List<EntityType<? extends Mob>> mobTypes() {
        return isLoaded() ? MutantMonstersEntities.mobTypes() : List.of();
    }

    @Nullable
    public static EntityType<? extends Mob> mobType(String name) {
        if (!isLoaded()) {
            return null;
        }
        return switch (name) {
            case "mutant_zombie" -> MutantMonstersEntities.MUTANT_ZOMBIE;
            case "mutant_creeper" -> MutantMonstersEntities.MUTANT_CREEPER;
            case "mutant_skeleton" -> MutantMonstersEntities.MUTANT_SKELETON;
            case "mutant_enderman" -> MutantMonstersEntities.MUTANT_ENDERMAN;
            case "spider_pig" -> MutantMonstersEntities.SPIDER_PIG;
            default -> null;
        };
    }

    public static boolean isMutant(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && id.getNamespace().equals("invmod")
                && MOB_NAMES.contains(id.getPath());
    }

    @Nullable
    public static EntityType<? extends Mob> replacementFor(
            EntityType<?> originalType) {
        if (!isLoaded()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(originalType);
        return id != null && id.getNamespace().equals(MOD_ID)
                ? mobType(id.getPath()) : null;
    }

    public static void registerAttributes(
            net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        if (isLoaded()) {
            MutantMonstersEntities.registerAttributes(event);
        }
    }
}
