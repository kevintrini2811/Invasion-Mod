package com.invasion.entity;

import com.invasion.InvSounds;
import com.invasion.block.BlockSpecial;
import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.server.level.ServerLevel;

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
    public void tick() {
        super.tick();
        if (tickCount > 60) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel serverLevel) {
            float damage = Math.min(14, Math.max(tickCount / 20.0F, 1) * 6);
            if (hit.getEntity().hurtServer(serverLevel, damageSources().arrow(this, getOwner()), damage)) {
                playSound(InvSounds.ENTITY_BOULDER_LAND, 1, 0.9F / (getRandom().nextFloat() * 0.2F + 0.9F));
                level().gameEvent(this, GameEvent.PROJECTILE_LAND, blockPosition());
                discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

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
                    if (serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
                        level().explode(this, getX(), getY(), getZ(), 2, ExplosionInteraction.BLOCK);
                    }
                }
            }
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        exploded = nbt.getBooleanOr("exploded", false);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("exploded", exploded);
    }
}
