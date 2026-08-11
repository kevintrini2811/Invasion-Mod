package com.invasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A faster zombie engineer variant equipped for mining. */
public final class ZombieMinerEntity extends ZombieBuilderEntity {
    public ZombieMinerEntity(
            EntityType<? extends ZombieMinerEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected Item getMiningTool() {
        return Items.DIAMOND_PICKAXE;
    }

    @Override
    public float getDiggingSpeedMultiplier() {
        return 2.0F;
    }
}
