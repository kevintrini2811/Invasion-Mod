package com.invasion.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

public final class ZombieBuilderEntity extends PigmanEngineerEntity {
    public ZombieBuilderEntity(
            EntityType<? extends ZombieBuilderEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected BlockState getBuildingBlock() {
        return Blocks.BRICKS.getDefaultState();
    }
}
