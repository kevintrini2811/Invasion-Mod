package com.invasion.entity.ai.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import com.invasion.Notifiable;
import com.invasion.InvasionMod;
import com.invasion.block.InvBlocks;
import com.invasion.entity.pathfinding.PathingUtil;

/**
 * AI component for building structures.
 * Takes a list of block changes and steps through them to place or break the blocks when they are in the entity's range.
 */
public final class TerrainModifier implements ITerrainModify {
    private final LivingEntity theEntity;

    private final float reach;

    private Notifiable finishCallback = Notifiable.NONE;
    private Notifiable blockChangeCallback = Notifiable.NONE;

    private List<ModifyBlockEntry> modList = new ArrayList<>();

    private @Nullable ModifyBlockEntry nextEntry;
    private @Nullable ModifyBlockEntry lastEntry;

    private int entryIndex;
    private int timer;

    private Notifiable.Status lastStatus = Notifiable.Status.SUCCESS;

    public TerrainModifier(LivingEntity entity, float defaultReach) {
        this.theEntity = entity;
        this.reach = Mth.square(defaultReach);
    }

    @Override
    public boolean isReadyForTask(Notifiable asker) {
        return modList.isEmpty() || finishCallback == asker;
    }

    public boolean isBusy() {
        return timer > 0;
    }

    public boolean submitJob(BlockPos pos, Notifiable asker, Function<BlockPos, Stream<ModifyBlockEntry>> job) {
        if (!isReadyForTask(asker)) {
            return false;
        }

        return requestTask(job.apply(pos).toList(), asker, null);
    }

    @Override
    public boolean requestTask(Collection<ModifyBlockEntry> entries, @Nullable Notifiable onFinished, @Nullable Notifiable onBlockChanged) {
        if (entries.isEmpty()) {
            return false;
        }
        if (isReadyForTask(onFinished)) {
            modList.addAll(entries);
            finishCallback = onFinished == null ? Notifiable.NONE : onFinished;
            blockChangeCallback = onBlockChanged == null ? Notifiable.NONE : onBlockChanged;
            return true;
        }
        return false;
    }

    @Override
    public ModifyBlockEntry getLastBlockModified() {
        return lastEntry;
    }

    public void onUpdate() {
        if (timer > 1) {
            timer -= 1;
            return;
        }
        if (timer == 1) {
            entryIndex++;
            timer = 0;
            lastStatus = changeBlock(nextEntry);
            lastEntry = nextEntry;
            blockChangeCallback.notifyTask(lastStatus);
        }

        if (entryIndex < modList.size()) {
            nextEntry = modList.get(entryIndex);
            while (isTerrainIdentical(nextEntry)) {
                entryIndex++;
                if (entryIndex < modList.size()) {
                    nextEntry = modList.get(entryIndex);
                } else {
                    cancelTask();
                    return;
                }
            }

            timer = Math.max(nextEntry.getCost(), 1);
        } else if (!modList.isEmpty()) {
            cancelTask();
        }
    }

    public void cancelTask() {
        entryIndex = 0;
        timer = 0;
        modList.clear();
        finishCallback.notifyTask(lastStatus);
    }

    private Notifiable.Status changeBlock(ModifyBlockEntry entry) {
        if (theEntity.getEyePosition().distanceToSqr(com.invasion.util.math.PosUtils.center(entry.pos())) > reach) {
            return Notifiable.Status.OUT_OF_RANGE;
        }

        BlockState oldBlock = theEntity.level().getBlockState(entry.pos());
        entry.setOldBlock(oldBlock);
        if (oldBlock.is(InvBlocks.NEXUS_CORE)) {
            return Notifiable.Status.UNMODIFIABLE;
        }
        if (entry.newBlock().is(Blocks.LADDER)
                && !oldBlock.is(Blocks.LADDER)
                && !PathingUtil.isAirOrReplaceable(oldBlock)) {
            return Notifiable.Status.UNMODIFIABLE;
        }

        int flags = Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS;
        if (!InvasionMod.getConfig().destructedBlocksDrop) {
            flags |= Block.UPDATE_SUPPRESS_DROPS;
        }

        boolean succeeded = entry.newBlock().isAir()
                ? theEntity.level().destroyBlock(entry.pos(), InvasionMod.getConfig().destructedBlocksDrop, theEntity)
                : theEntity.level().setBlock(entry.pos(), entry.newBlock(), flags);
        if (!succeeded) {
            return Notifiable.Status.UNMODIFIABLE;
        }
        if (!entry.newBlock().isAir()) {
            theEntity.level().playSound(null, entry.pos(), entry.newBlock().getSoundType().getPlaceSound(), SoundSource.BLOCKS);
        }
        return Notifiable.Status.SUCCESS;
    }

    private boolean isTerrainIdentical(ModifyBlockEntry entry) {
        return theEntity.level().getBlockState(entry.pos()) == entry.newBlock();
    }
}
