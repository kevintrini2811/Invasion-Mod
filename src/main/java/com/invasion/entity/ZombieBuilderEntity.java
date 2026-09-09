package com.invasion.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** An engineer variant that builds its structures from bricks. */
public class ZombieBuilderEntity extends PigmanEngineerEntity {
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

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }
}
