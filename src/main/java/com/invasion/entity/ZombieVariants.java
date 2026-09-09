package com.invasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Resolves legacy zombie tiers to their independently configurable entity types. */
public final class ZombieVariants {
    private ZombieVariants() {}

    @SuppressWarnings("unchecked")
    public static <T extends Mob> EntityType<T> resolve(EntityType<T> type, int tier, int flavour) {
        // Each pair registers the same entity class, preserving the caller's type.
        EntityType<?> resolved = type;
        if (tier == 3) {
            if (type == InvEntities.ZOMBIE) resolved = InvEntities.ZOMBIE_BRUTE;
            else if (type == InvEntities.SPEEDY_ZOMBIE) resolved = InvEntities.SPEEDY_ZOMBIE_BRUTE;
            else if (type == InvEntities.HUSK) resolved = InvEntities.HUSK_BRUTE;
            else if (type == InvEntities.DROWNED) resolved = InvEntities.DROWNED_BRUTE;
            else if (type == InvEntities.ZOMBIE_VILLAGER) resolved = InvEntities.ZOMBIE_VILLAGER_BRUTE;
            else if (type == InvEntities.ZOMBIE_PIGMAN) resolved = InvEntities.ZOMBIE_PIGMAN_BRUTE;
        } else if (type == InvEntities.ZOMBIE && tier == 2 && flavour == 2) {
            resolved = InvEntities.TAR_ZOMBIE;
        }
        return (EntityType<T>) resolved;
    }

    public static EntityType<?> baseType(EntityType<?> type) {
        if (type == InvEntities.ZOMBIE_BRUTE || type == InvEntities.TAR_ZOMBIE) return InvEntities.ZOMBIE;
        if (type == InvEntities.SPEEDY_ZOMBIE_BRUTE) return InvEntities.SPEEDY_ZOMBIE;
        if (type == InvEntities.HUSK_BRUTE) return InvEntities.HUSK;
        if (type == InvEntities.DROWNED_BRUTE) return InvEntities.DROWNED;
        if (type == InvEntities.ZOMBIE_VILLAGER_BRUTE) return InvEntities.ZOMBIE_VILLAGER;
        if (type == InvEntities.ZOMBIE_PIGMAN_BRUTE) return InvEntities.ZOMBIE_PIGMAN;
        return type;
    }
}
