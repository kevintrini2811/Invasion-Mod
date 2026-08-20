package com.invasion.entity.ai.goal;

import java.util.EnumSet;
import java.util.Collections;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
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
    private static final int FAILURES_BEFORE_REPATH = 3;
    private static final Set<PathfinderMob> ACTIVE_MINERS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<PathfinderMob> RECOVERY_REQUESTS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private final PathfinderMob mob;
    private final PathNavigation navigation;

    private float breakProgress;
    private int prevBreakProgress;
    private Stack<BreakEntry> breakingBlockPos = new Stack<>();
    @Nullable
    private BreakEntry currentEntry;
    @Nullable
    private BlockPos lastFailedBlock;
    private int consecutiveFailures;

    public MineBlockGoal(PathfinderMob mob) {
        this.mob = mob;
        navigation = mob.getNavigation();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return ((net.minecraft.server.level.ServerLevel) mob.level()).getGameRules().get(GameRules.MOB_GRIEFING)
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
            if (!breakingBlockPos.isEmpty()) {
                ACTIVE_MINERS.add(mob);
            }
        }
    }

    public static boolean isMining(PathfinderMob mob) {
        return ACTIVE_MINERS.contains(mob);
    }

    public static boolean consumeRecoveryRequest(PathfinderMob mob) {
        return RECOVERY_REQUESTS.remove(mob);
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
            clearFailures();
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
            if (removed) {
                clearFailures();
            } else {
                recordFailure(pos);
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
        ACTIVE_MINERS.remove(mob);
        if (currentEntry != null) {
            recordFailure(currentEntry.pos());
            mob.level().destroyBlockProgress(
                    mob.getId(), currentEntry.pos(), -1);
            currentEntry = null;
        }
    }

    private void recordFailure(BlockPos pos) {
        if (pos.equals(lastFailedBlock)) {
            consecutiveFailures++;
        } else {
            lastFailedBlock = pos;
            consecutiveFailures = 1;
        }
        if (consecutiveFailures >= FAILURES_BEFORE_REPATH) {
            RECOVERY_REQUESTS.add(mob);
            ACTIVE_MINERS.remove(mob);
            breakingBlockPos.clear();
            consecutiveFailures = 0;
        }
    }

    private void clearFailures() {
        lastFailedBlock = null;
        consecutiveFailures = 0;
    }

    static float getDiggingSpeed(LivingEntity entity, BlockState state, BlockPos pos) {
        ItemStack stack = entity.getMainHandItem();
        float multiplier = stack.getDestroySpeed(state);
        if (MobEffectUtil.hasDigSpeed(entity)) {
            multiplier *= 1 + (MobEffectUtil.getDigSpeedAmplification(entity) + 1) * 0.2F;
        }
        if (entity.hasEffect(MobEffects.MINING_FATIGUE)) {
            multiplier *= switch(entity.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
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
        for (BlockPos mutablePos : BlockPos.betweenClosed(
                Mth.floor(bounds.minX), Mth.floor(bounds.minY),
                Mth.floor(bounds.minZ), Mth.floor(bounds.maxX),
                Mth.floor(bounds.maxY), Mth.floor(bounds.maxZ))) {
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
