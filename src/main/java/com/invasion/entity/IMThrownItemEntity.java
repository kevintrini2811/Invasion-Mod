package com.invasion.entity;

import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Damaging mushroom, sand, and snowball projectile used by baby IM skeletons. */
public final class IMThrownItemEntity extends ThrowableItemProjectile {
    public IMThrownItemEntity(
            EntityType<? extends IMThrownItemEntity> type, Level level) {
        super(type, level);
    }

    public IMThrownItemEntity(
            Level level, LivingEntity owner, ItemStack item) {
        super(InvEntities.THROWN_ITEM, owner, level, item);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    public void tick() {
        if (getItem().isEmpty()) {
            discard();
        } else {
            super.tick();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3 && !getItem().isEmpty()) {
            ParticleOptions particle = new ItemParticleOption(
                    ParticleTypes.ITEM,
                    ItemStackTemplate.fromNonEmptyStack(getItem()));
            for (int i = 0; i < 8; i++) {
                level().addParticle(
                        particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        entity.hurt(damageSources().thrown(this, getOwner()), 0.5F);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level() instanceof ServerLevel
                && level().getBlockState(hit.getBlockPos())
                        .is(InvBlocks.NEXUS_CORE)
                && level().getBlockEntity(hit.getBlockPos())
                        instanceof NexusBlockEntity nexus) {
            nexus.getNexus().damage(
                    damageSources().thrown(this, getOwner()), 2);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel world) {
            world.broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }
}
