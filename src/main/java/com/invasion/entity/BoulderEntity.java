package com.invasion.entity;

import com.invasion.InvSounds;
import com.invasion.block.BlockSpecial;
import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class BoulderEntity extends AbstractArrow {

    private boolean exploded;

    public BoulderEntity(EntityType<? extends BoulderEntity> type, Level world) {
        super(type, world);
        setPickupItemStack(getDefaultPickupItem());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return Items.STONE.getDefaultInstance();
    }

    @Override
    protected boolean tryPickup(Player player) {
        return !getPickupItemStackOrigin().isEmpty() && super.tryPickup(player);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        playSound(InvSounds.ENTITY_BOULDER_LAND, 1, 0.9F / (getRandom().nextFloat() * 0.2F + 0.9F));
        level().gameEvent(this, GameEvent.HIT_GROUND, blockPosition());
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (!exploded) {
            exploded = true;
            if (state.is(InvBlocks.NEXUS_CORE) && level().getBlockEntity(hit.getBlockPos()) instanceof NexusBlockEntity nexus) {
                // TODO: Boulder damage source type
                nexus.getNexus().damage(damageSources().arrow(this, getOwner()), 2);
            } else if (state.getDestroySpeed(level(), hit.getBlockPos()) >= 0) {

                if (!state.is(BlockTags.WITHER_IMMUNE) && !state.is(BlockTags.DRAGON_IMMUNE)) {
                    level().gameEvent(this, GameEvent.HIT_GROUND, hit.getBlockPos());
                    if (BlockSpecial.of(state) == BlockSpecial.DEFLECTION && getRandom().nextInt(2) == 0) {
                        discard();
                        return;
                    }
                    if (level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                        level().explode(this, getX(), getY(), getZ(), 2, ExplosionInteraction.BLOCK);
                    }
                }
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        exploded = nbt.getBoolean("exploded");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("exploded", exploded);
    }
}