package com.invasion.entity;

import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Trident fired by an IM mob that can damage a Nexus core on impact. */
public final class IMThrownTridentEntity extends ThrownTrident {
    public IMThrownTridentEntity(
            Level level, LivingEntity owner, ItemStack weapon) {
        super(level, owner, weapon);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level() instanceof ServerLevel
                && level().getBlockState(hit.getBlockPos()).is(InvBlocks.NEXUS_CORE)
                && level().getBlockEntity(hit.getBlockPos()) instanceof NexusBlockEntity nexus) {
            nexus.getNexus().damage(damageSources().trident(this, getOwner()), 2);
            discard();
        }
    }
}
