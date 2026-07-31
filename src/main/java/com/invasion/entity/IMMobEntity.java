package com.invasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class IMMobEntity extends EntityIMLiving {
    public IMMobEntity(EntityType<? extends IMMobEntity> type, Level world) {
        super(type, world);
    }
}