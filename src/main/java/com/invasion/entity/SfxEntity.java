package com.invasion.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

// TODO: What is this for?
@Deprecated(since = "unused")
public class SfxEntity extends Entity {
    private int lifespan;

    public SfxEntity(EntityType<SfxEntity> type, Level world) {
        super(type, world);
        this.lifespan = 200;
    }

    public SfxEntity(EntityType<SfxEntity> type, Level world, double x, double y, double z) {
        this(type, world);
        setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (lifespan-- <= 0) {
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte byte0) {
    }

    @Override
    protected void defineSynchedData(Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput nbt) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput nbt) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
