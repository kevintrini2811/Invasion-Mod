package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** A standard IM zombie with twice the movement and mining speed. */
public final class EntityIMSpeedyZombie extends EntityIMZombie {
    public EntityIMSpeedyZombie(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
    }

    @Override
    protected void initTieredAttributes() {
        super.initTieredAttributes();
        setBaseMovementSpeed(
                getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * 2.0D);
    }

    @Override
    public float getDiggingSpeedMultiplier() {
        return 2.0F;
    }

    private boolean isInSoulFire() {
        return level().getBlockState(blockPosition()).is(Blocks.SOUL_FIRE);
    }

    @Override
    public boolean hurtServer(
            ServerLevel world, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.IS_FIRE) && isInSoulFire()) {
            return false;
        }
        return super.hurtServer(world, source, damage);
    }

    @Override
    public void tick() {
        super.tick();
        if (isInSoulFire()) {
            setRemainingFireTicks(0);
        }
    }

    public static void convertFrom(EntityIMZombie source) {
        if (!(source.level() instanceof ServerLevel world)
                || source instanceof EntityIMSpeedyZombie) {
            return;
        }
        EntityIMSpeedyZombie speedy = InvEntities.SPEEDY_ZOMBIE.create(
                world, EntitySpawnReason.CONVERSION);
        if (speedy == null) {
            return;
        }

        speedy.setTier(source.getTier());
        speedy.setFlavour(source.getFlavour());
        speedy.setBaby(source.isBaby());
        speedy.setNexus(source.getNexus());
        speedy.setCountsTowardMobCap(source.countsTowardMobCap());
        speedy.snapTo(source.getX(), source.getY(), source.getZ(),
                source.getYRot(), source.getXRot());
        speedy.setDeltaMovement(source.getDeltaMovement());
        speedy.setCustomName(source.getCustomName());
        speedy.setCustomNameVisible(source.isCustomNameVisible());
        speedy.setNoAi(source.isNoAi());
        speedy.setCanPickUpLoot(source.canPickUpLoot());
        speedy.setTarget(source.getTarget());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            speedy.setItemSlot(slot, source.getItemBySlot(slot).copy());
        }
        speedy.setHealth(Math.min(speedy.getMaxHealth(),
                source.getHealth() / source.getMaxHealth()
                        * speedy.getMaxHealth()));
        if (source.isPersistenceRequired()) {
            speedy.setPersistenceRequired();
        }

        Entity vehicle = source.getVehicle();
        source.stopRiding();
        source.discard();
        if (world.addFreshEntity(speedy)
                && vehicle != null && !vehicle.isRemoved()) {
            speedy.startRiding(vehicle);
        }
    }
}
