package com.invasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public class SpawnProxyEntity extends Mob {
    public SpawnProxyEntity(EntityType<SpawnProxyEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

}
