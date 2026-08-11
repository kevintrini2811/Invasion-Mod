package com.invasion.entity;

import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.NexusAccess;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public abstract class TieredIMMobEntity extends IMMobEntity {
    private static final TrackedData<Integer> TIER = DataTracker.registerData(TieredIMMobEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FLAVOUR = DataTracker.registerData(TieredIMMobEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private boolean updatingAttributes;

    public TieredIMMobEntity(EntityType<? extends IMMobEntity> type, World world) {
        super(type, world);
        initTieredAttributes();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TIER, 1);
        builder.add(FLAVOUR, 0);
    }

    @Override
    public void onSpawned(NexusAccess nexus, EntityConstruct spawnConditions) {
        super.onSpawned(nexus, spawnConditions);
        setAppearance(spawnConditions.tier(), spawnConditions.flavour());
    }

    public final int getTier() {
        return dataTracker.get(TIER);
    }

    public final void setTier(int tier) {
        if (tier != getTier()) {
            dataTracker.set(TIER, tier);
            onAttributesChanged();
        }
    }

    public final int getFlavour() {
        return dataTracker.get(FLAVOUR);
    }

    public final void setFlavour(int flavour) {
        if (flavour != getFlavour()) {
            dataTracker.set(FLAVOUR, flavour);
            onAttributesChanged();
        }
    }

    private void setAppearance(int tier, int flavour) {
        if (tier != getTier() || flavour != getFlavour()) {
            dataTracker.set(TIER, tier);
            dataTracker.set(FLAVOUR, flavour);
            onAttributesChanged();
        }
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (data == TIER || data == FLAVOUR) {
            onAttributesChanged();
        }
    }

    protected void onAttributesChanged() {
        if (updatingAttributes) {
            return;
        }
        updatingAttributes = true;
        try {
            initTieredAttributes();
            if (!getWorld().isClient) {
                resetHealth();
            }
        } finally {
            updatingAttributes = false;
        }
    }

    protected abstract void initTieredAttributes();

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("tier", getTier());
        compound.putInt("flavour", getFlavour());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        setAppearance(compound.getInt("tier"), compound.getInt("flavour"));
    }

    @Override
    @Deprecated
    public String getLegacyName() {
        return String.format("%s-T1", getClass().getName().replace("Entity", ""), getTier());
    }
}
