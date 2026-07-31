package com.invasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class EntityIMPrimedTNT extends BoulderEntity {
    public EntityIMPrimedTNT(EntityType<EntityIMPrimedTNT> type, Level world) {
        super(type, world);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        convertIntoExplosive();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        convertIntoExplosive();
    }

    private void convertIntoExplosive() {
        discard();
        PrimedTnt tnt = new PrimedTnt(level(), getX(), getY(), getZ(), getOwner() instanceof LivingEntity l ? l : null);
        tnt.setDeltaMovement(getDeltaMovement());
        level().addFreshEntity(tnt);
    }
}
