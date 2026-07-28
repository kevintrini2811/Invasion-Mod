package com.invasion.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BurrowerTailEntity extends Entity {
    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(BurrowerTailEntity.class, EntityDataSerializers.INT);

    public BurrowerTailEntity(EntityType<BurrowerTailEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setParent(BurrowerEntity parent) {
        entityData.set(PARENT_ID, parent.getId());
    }

    private BurrowerEntity parent() {
        Entity entity = level().getEntity(entityData.get(PARENT_ID));
        return entity instanceof BurrowerEntity burrower ? burrower : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PARENT_ID, 0);
    }

    @Override
    public void tick() {
        super.tick();
        BurrowerEntity parent = parent();
        if (parent == null || !parent.isAlive()) {
            if (!level().isClientSide() || tickCount > 20) {
                discard();
            }
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        BurrowerEntity parent = parent();
        return parent != null && parent.hurtServer(level, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return super.is(entity) || entity == parent();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
