package com.invasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** An engineer variant that builds its structures from bricks. */
public final class ZombieBuilderEntity extends PigmanEngineerEntity {
    public ZombieBuilderEntity(
            EntityType<? extends ZombieBuilderEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected BlockState getBuildingBlock() {
        return Blocks.BRICKS.defaultBlockState();
    }

    @Override
    protected boolean dropsEngineerBonusLoot() {
        return false;
    }
}
