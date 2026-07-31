package com.invasion.entity;

import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Arrow fired by an IM skeleton. A nexus is damaged only when the arrow
 * actually collides with its core block.
 */
public class SkeletonArrowEntity extends Arrow {
    public SkeletonArrowEntity(EntityType<? extends SkeletonArrowEntity> type, Level level) {
        super(type, level);
    }

    public SkeletonArrowEntity(Level level, LivingEntity owner, ItemStack weapon) {
        this(InvEntities.SKELETON_ARROW, level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setPickupItemStack(Items.ARROW.getDefaultInstance());
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level() instanceof ServerLevel
                && level().getBlockState(hit.getBlockPos()).is(InvBlocks.NEXUS_CORE)
                && level().getBlockEntity(hit.getBlockPos()) instanceof NexusBlockEntity nexus) {
            nexus.getNexus().damage(damageSources().arrow(this, getOwner()), 2);
            discard();
        }
    }
}
