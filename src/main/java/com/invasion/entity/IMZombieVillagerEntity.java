package com.invasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * An IM zombie created when an IM mob kills a villager.
 */
public final class IMZombieVillagerEntity extends EntityIMZombie {
    public IMZombieVillagerEntity(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
    }
}
