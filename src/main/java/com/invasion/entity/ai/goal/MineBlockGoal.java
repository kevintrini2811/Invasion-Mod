package com.invasion.entity.ai.goal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;
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
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult.Type;
import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.block.BlockMetadata;
import com.invasion.block.InvBlockEntities;
import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import com.invasion.entity.pathfinding.IMLandPathNodeMaker;

public class MineBlockGoal extends Goal {
    private final PathfinderMob mob;
    private final PathNavigation navigation;

    private float breakProgress;
    private int prevBreakProgress;
    private Stack<BreakEntry> breakingBlockPos = new Stack<>();
    private Optional<BreakEntry> currentEntry = Optional.empty();

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
            breakingBlockPos.addAll(getClearRegion(mob, navigation.getPath().getNextNodePos()).distinct().toList());
            navigation.stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return breakingBlockPos.stream().anyMatch(entry -> !mob.level().isEmptyBlock(entry.pos())) || currentEntry.isPresent();
    }

    @Override
    public void tick() {
        currentEntry = currentEntry.or(() -> {
            return breakingBlockPos.isEmpty() ? Optional.empty() : Optional.of(breakingBlockPos.pop());
        }).filter(entry -> {
            BlockPos pos = entry.pos();
            BlockState breakingState = mob.level().getBlockState(pos);
            if (breakingState != entry.lastKnownState()) {
                return false;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            mob.getLookControl().setLookAt(com.invasion.util.math.PosUtils.center(pos));

            float speed = getDiggingSpeed(mob, breakingState, pos) * 10;
            breakProgress += speed;
            if (breakProgress >= 10) {
                mob.level().destroyBlockProgress(mob.getId(), pos, -1);
                mob.level().destroyBlock(pos, InvasionMod.getConfig().destructedBlocksDrop);
                breakProgress = 0;
                return false;
            } else {
                if ((int)breakProgress != prevBreakProgress) {
                    prevBreakProgress = (int)breakProgress;
                    if (breakingState.is(InvBlocks.NEXUS_CORE)) {
                        mob.level()
                            .getBlockEntity(pos, InvBlockEntities.NEXUS)
                            .map(NexusBlockEntity::getNexus)
                            .ifPresent(nexus -> nexus.damage(mob.damageSources().mobAttack(mob), 1));
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
            return true;
        });
    }

    @Override
    public void stop() {
        currentEntry = currentEntry.filter(entry -> {
            mob.level().destroyBlockProgress(mob.getId(), entry.pos(), -1);
            return false;
        });
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

    static Stream<BreakEntry> getClearRegion(PathfinderMob mob, BlockPos center) {
        return BlockPos.betweenClosedStream(mob.getDimensions(mob.getPose()).makeBoundingBox(
                    com.invasion.util.math.PosUtils.bottomCenter(new BlockPos(center.getX(), mob.blockPosition().getY(), center.getZ()))
                ))
                .filter(pos -> IMLandPathNodeMaker.canMineBlock(mob, pos) || mob.level().getBlockState(pos).is(InvBlocks.NEXUS_CORE))
                .map(BlockPos::immutable)
                .sorted(Comparator.comparing(i -> mob.distanceToSqr(com.invasion.util.math.PosUtils.center(i)) + BlockMetadata.getStrength(i, mob.level().getBlockState(i), mob.level())))
                .map(i -> new BreakEntry(i, mob.level().getBlockState(i)));
    }

    record BreakEntry(BlockPos pos, BlockState lastKnownState) {}
}
