package com.invasion.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import com.invasion.InvSounds;
import com.invasion.item.InvItems;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.IHasNexus;

public class SpiderEggEntity extends Mob implements Combatant<SpiderEggEntity> {
    private static final EntityDataAccessor<Boolean> HATCHED = SynchedEntityData.defineId(SpiderEggEntity.class, EntityDataSerializers.BOOLEAN);

    private int hatchTime;
    private int ticks;

    private List<Entity> contents;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::level);

    public SpiderEggEntity(EntityType<SpiderEggEntity> type, Level world) {
        super(type, world);
    }

    public SpiderEggEntity(Entity parent, List<Entity> contents, int hatchTime) {
        super(InvEntities.SPIDER_EGG, parent.level());
        this.contents = contents == null ? List.of() : contents;
        this.hatchTime = hatchTime;
        resetHealth();
        setPos(parent.position());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.01);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        if (getRandom().nextInt(4) == 0) {
            spawnAtLocation(InvItems.SMALL_REMNANTS);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HATCHED, false);
    }

    public boolean isHatched() {
        return entityData.get(HATCHED);
    }

    public void setHatched(boolean hatched) {
        entityData.set(HATCHED, hatched);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            ticks++;
            if (isHatched()) {
                if (ticks > hatchTime + 40) {
                    discard();
                }
            } else if (ticks > hatchTime) {
                hatch();
            }
        }
    }
    private void hatch() {
        playSound(InvSounds.ENTITY_SPIDER_EGG_HATCH, 1, 1);
        setHatched(true);
        if (!level().isClientSide()) {
            for (Entity entity : contents) {
                entity.setPos(position());
                level().addFreshEntity(entity);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("ticks", ticks);
        compound.putInt("hatchTime", hatchTime);
        net.minecraft.nbt.ListTag entities = new net.minecraft.nbt.ListTag();
        for (Entity entity : contents) {
            // EntityType.create requires the serialized type id.
            CompoundTag entityTag = new CompoundTag();
            if (entity.save(entityTag)) {
                entities.add(entityTag);
            }
        }
        compound.put("contents", entities);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        ticks = compound.getInt("ticks");
        hatchTime = compound.getInt("hatchTime");
        contents = compound.getList("contents", net.minecraft.nbt.Tag.TAG_COMPOUND).stream()
                .flatMap(entity -> EntityType.create((CompoundTag) entity, level()).stream())
                .toList();
    }

    @Override
    public String getLegacyName() {
        return "IMSpider-egg";
    }

    @Override
    public Handle getNexusHandle() {
        return nexus;
    }

    @Override
    public SpiderEggEntity asEntity() {
        return this;
    }
}
