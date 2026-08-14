package com.invasion.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // Spider eggs are anchored once laid and must not be displaced by mobs.
    }

    @Override
    public void knockback(double strength, double x, double z,
            DamageSource source, float damage, boolean force) {
        // Damage may hurt the egg, but it must not move it away from the nest.
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        if (getRandom().nextInt(4) == 0) {
            spawnAtLocation(level, InvItems.SMALL_REMNANTS);
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
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
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
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("ticks", ticks);
        compound.putInt("hatchTime", hatchTime);
        ValueOutput.ValueOutputList entities = compound.childrenList("contents");
        for (Entity entity : contents) {
			// EntityType.create requires the serialized type id. saveWithoutId
			// produced "Skipping Entity with id [invalid]" and lost hatchlings.
			entity.save(entities.addChild());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        ticks = compound.getIntOr("ticks", 0);
        hatchTime = compound.getIntOr("hatchTime", 0);
        contents = compound.childrenListOrEmpty("contents").stream()
                .flatMap(entity -> EntityType.create(entity, level(),
                        new EntitySpawnRequest(EntitySpawnReason.LOAD, false)).stream())
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
