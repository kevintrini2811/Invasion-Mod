package com.invasion.entity.ai.goal;

import java.util.EnumSet;
import java.util.Stack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult.Type;
import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.block.BlockMetadata;
import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import com.invasion.entity.Miner;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;

public class MineBlockGoal extends Goal {
    private final PathfinderMob mob;
    private final PathNavigation navigation;

    private float breakProgress;
    private int prevBreakProgress;
    private Stack<BreakEntry> breakingBlockPos = new Stack<>();
    @Nullable
    private BreakEntry currentEntry;

    public MineBlockGoal(PathfinderMob mob) {
        this.mob = mob;
        navigation = mob.getNavigation();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return ((net.minecraft.server.level.ServerLevel) mob.level()).getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && !navigation.isDone()
                && mob.tickCount % 5 == 0
                && mob.pick(1, 1, false).getType() == Type.BLOCK;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        breakProgress = 0;
        breakingBlockPos.clear();
        if (canUse()) {
            mob.playSound(InvSounds.ENTITY_SCRAPE,
                    (float)mob.getRandom().triangle(0.5F, 0.5F),
                    (float)mob.getRandom().triangle(mob.getVoicePitch(), 0.2F)
            );
            addClearRegion(navigation.getPath().getNextNodePos());
            navigation.stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (currentEntry != null) {
            return true;
        }
        for (BreakEntry entry : breakingBlockPos) {
            if (!mob.level().isEmptyBlock(entry.pos())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (currentEntry == null) {
            if (breakingBlockPos.isEmpty()) {
                return;
            }
            currentEntry = breakingBlockPos.pop();
        }
        BlockPos pos = currentEntry.pos();
        BlockState breakingState = mob.level().getBlockState(pos);
        if (breakingState != currentEntry.lastKnownState()) {
            currentEntry = null;
            return;
        }
        mob.swing(InteractionHand.MAIN_HAND);
        mob.getLookControl().setLookAt(com.invasion.util.math.PosUtils.center(pos));

        float speedMultiplier = mob instanceof Miner miner
                ? miner.getDiggingSpeedMultiplier()
                : 1.0F;
        if (mob.isInWater()) {
            speedMultiplier *= 0.5F;
        }
        float speed = getDiggingSpeed(mob, breakingState, pos)
                * speedMultiplier * 10;
        breakProgress += speed;
        if (breakProgress >= 10) {
            mob.level().destroyBlockProgress(mob.getId(), pos, -1);
            if (breakingState.is(InvBlocks.NEXUS_CORE)) {
                damageNexus(pos);
                breakProgress = 0;
                return;
            }
            boolean removed = mob.level().destroyBlock(pos,
                    InvasionMod.getConfig().destructedBlocksDrop);
            if (removed && mob instanceof Miner miner) {
                miner.onBlockRemoved(pos, breakingState);
            }
            breakProgress = 0;
            currentEntry = null;
            return;
        }
        if ((int)breakProgress != prevBreakProgress) {
            prevBreakProgress = (int)breakProgress;
            if (breakingState.is(InvBlocks.NEXUS_CORE)) {
                damageNexus(pos);
            }
        }
        mob.level().destroyBlockProgress(mob.getId(), pos, prevBreakProgress);
        if (mob.tickCount % 4 == 0) {
            mob.level().playSound(null, pos,
                    breakingState.getSoundType().getHitSound(),
                    SoundSource.BLOCKS,
                    (breakingState.getSoundType().getVolume() + 1) / 8F,
                    breakingState.getSoundType().getPitch() * 0.5F
            );
        }
    }

    @Override
    public void stop() {
        if (currentEntry != null) {
            mob.level().destroyBlockProgress(
                    mob.getId(), currentEntry.pos(), -1);
            currentEntry = null;
        }
    }

    static float getDiggingSpeed(LivingEntity entity, BlockState state, BlockPos pos) {
        ItemStack stack = entity.getMainHandItem();
        float multiplier = stack.getDestroySpeed(state);
        if (MobEffectUtil.hasDigSpeed(entity)) {
            multiplier *= 1 + (MobEffectUtil.getDigSpeedAmplification(entity) + 1) * 0.2F;
        }
        if (entity.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            multiplier *= switch(entity.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }

        float hardness = state.getDestroySpeed(entity.level(), pos);
        float speed = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state) ? 30 : 100;
        return multiplier / hardness / speed;
    }

    private void addClearRegion(BlockPos center) {
        var bounds = mob.getDimensions(mob.getPose()).makeBoundingBox(
                com.invasion.util.math.PosUtils.bottomCenter(new BlockPos(
                        center.getX(), mob.blockPosition().getY(), center.getZ())));
        for (BlockPos mutablePos : BlockPos.betweenClosed(bounds)) {
            BlockState state = mob.level().getBlockState(mutablePos);
            if (!IMLandPathNodeMaker.canMineBlock(mob, mutablePos)
                    && !state.is(InvBlocks.NEXUS_CORE)) {
                continue;
            }
            BlockPos pos = mutablePos.immutable();
            double priority = mob.distanceToSqr(
                    com.invasion.util.math.PosUtils.center(pos))
                    + BlockMetadata.getStrength(pos, state, mob.level());
            BreakEntry entry = new BreakEntry(pos, state, priority);
            int index = 0;
            while (index < breakingBlockPos.size()
                    && breakingBlockPos.get(index).priority() <= priority) {
                index++;
            }
            breakingBlockPos.add(index, entry);
        }
    }

    private void damageNexus(BlockPos pos) {
        if (mob.level().getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
            nexus.getNexus().damage(mob.damageSources().mobAttack(mob), 1);
        }
    }

    record BreakEntry(
            BlockPos pos, BlockState lastKnownState, double priority) {}
}
