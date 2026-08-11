package com.invasion.entity;

import java.util.List;

import com.invasion.InvSounds;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.IHasNexus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;

public class SpiderEggEntity extends MobEntity implements Combatant<SpiderEggEntity> {
    private static final TrackedData<Boolean> HATCHED = DataTracker.registerData(SpiderEggEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int hatchTime;
    private int ticks;

    private List<Entity> contents;

    private final IHasNexus.Handle nexus = new IHasNexus.Handle(this::getWorld);

    public SpiderEggEntity(EntityType<SpiderEggEntity> type, World world) {
        super(type, world);
    }

    public SpiderEggEntity(Entity parent, List<Entity> contents, int hatchTime) {
        super(InvEntities.SPIDER_EGG, parent.getWorld());
        this.contents = contents == null ? List.of() : contents;
        this.hatchTime = hatchTime;
        resetHealth();
        setPosition(parent.getPos());
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.01);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HATCHED, false);
    }

    public boolean isHatched() {
        return dataTracker.get(HATCHED);
    }

    public void setHatched(boolean hatched) {
        dataTracker.set(HATCHED, hatched);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient) {
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
    @Override
    public void tickMovement() {
        if (isAlive() && !hasNexus() && isAffectedByDaylight()) {
            setOnFireFor(8);
        }

        super.tickMovement();
    }


    private void hatch() {
        playSound(InvSounds.ENTITY_SPIDER_EGG_HATCH, 1, 1);
        setHatched(true);
        if (!getWorld().isClient) {
            for (Entity entity : contents) {
                entity.setPosition(getPos());
                getWorld().spawnEntity(entity);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putInt("ticks", ticks);
        compound.putInt("hatchTime", hatchTime);
        NbtList entities = new NbtList();
        for (Entity entity : contents) {
            entities.add(entity.writeNbt(new NbtCompound()));
        }
        compound.put("contents", entities);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        ticks = compound.getInt("ticks");
        hatchTime = compound.getInt("hatchTime");
        contents = compound.getList("contents", NbtElement.COMPOUND_TYPE).stream()
                .flatMap(entity -> EntityType.getEntityFromNbt((NbtCompound)entity, getWorld()).stream())
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
