package com.invasion.entity;

import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.NexusAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class TieredIMMobEntity extends IMMobEntity {
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(TieredIMMobEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAVOUR = SynchedEntityData.defineId(TieredIMMobEntity.class, EntityDataSerializers.INT);

    private boolean updatingAttributes;

    public TieredIMMobEntity(EntityType<? extends IMMobEntity> type, Level world) {
        super(type, world);
        initTieredAttributes();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TIER, 1);
        builder.define(FLAVOUR, 0);
    }

    @Override
    public void onSpawned(NexusAccess nexus, EntityConstruct spawnConditions) {
        super.onSpawned(nexus, spawnConditions);
        setAppearance(spawnConditions.tier(), spawnConditions.flavour());
    }

    public final int getTier() {
        return entityData.get(TIER);
    }

    public final void setTier(int tier) {
        if (tier != getTier()) {
            entityData.set(TIER, tier);
            onAttributesChanged();
        }
    }

    public final int getFlavour() {
        return entityData.get(FLAVOUR);
    }

    public final void setFlavour(int flavour) {
        if (flavour != getFlavour()) {
            entityData.set(FLAVOUR, flavour);
            onAttributesChanged();
        }
    }

    private void setAppearance(int tier, int flavour) {
        if (tier != getTier() || flavour != getFlavour()) {
            entityData.set(TIER, tier);
            entityData.set(FLAVOUR, flavour);
            onAttributesChanged();
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
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
            if (!level().isClientSide()) {
                resetHealth();
            }
        } finally {
            updatingAttributes = false;
        }
    }

    protected abstract void initTieredAttributes();

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("tier", getTier());
        compound.putInt("flavour", getFlavour());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        EquipmentSlot[] slots = EquipmentSlot.values();
        ItemStack[] equipment = new ItemStack[slots.length];
        float[] dropChances = new float[slots.length];
        for (int i = 0; i < slots.length; i++) {
            equipment[i] = getItemBySlot(slots[i]).copy();
            dropChances[i] = getEquipmentDropChance(slots[i]);
        }

        setAppearance(compound.getInt("tier"), compound.getInt("flavour"));

        // Applying the saved tier rebuilds attributes and default loadout.
        // Restore the equipment that Mob already decoded from this save.
        for (int i = 0; i < slots.length; i++) {
            setItemSlot(slots[i], equipment[i]);
            setDropChance(slots[i], dropChances[i]);
        }
    }

    @Override
    @Deprecated
    public String getLegacyName() {
        return String.format("%s-T1", getClass().getName().replace("Entity", ""), getTier());
    }
}
