package com.invasion.entity.ai.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared geometry and repair plan for every mob with the engineer tower ability. */
public record EngineerTower(BlockPos base, Direction ladderFacing) {
    public static final int SEARCH_RADIUS = 16;

    public BlockPos center() { return base.above(3); }
    public BlockPos ladderBase() { return base.relative(ladderFacing); }

    public List<BlockPos> footprint() {
        List<BlockPos> positions = new ArrayList<>();
        for (int height = 0; height < 3; height++) positions.add(base.above(height));
        for (int height = 0; height <= 3; height++) positions.add(ladderBase().above(height));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = center().offset(x, 0, z);
                if (!pos.equals(ladderBase().above(3))) positions.add(pos);
            }
        }
        return positions;
    }

    public boolean isLoaded(Level level) {
        return footprint().stream().allMatch(level::hasChunkAt);
    }

    public boolean hasRemains(Level level) {
        int count = 0;
        for (BlockPos pos : footprint()) {
            BlockState state = level.getBlockState(pos);
            boolean shaft = pos.getX() == ladderBase().getX() && pos.getZ() == ladderBase().getZ();
            if (shaft ? state.is(Blocks.LADDER) && state.getValue(LadderBlock.FACING) == ladderFacing
                    : state.isCollisionShapeFullBlock(level, pos)) {
                if (++count >= 2) return true;
            }
        }
        return false;
    }

    public BlockPos workPosition(Level level) {
        BlockPos pos = ladderBase();
        if (blocked(level, pos) || blocked(level, pos.above())) pos = pos.relative(ladderFacing);
        return level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below())
                && !blocked(level, pos) && !blocked(level, pos.above()) ? pos : null;
    }

    private static boolean blocked(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.is(Blocks.LADDER) && !state.getCollisionShape(level, pos).isEmpty();
    }

    public boolean canBuild(Level level, Predicate<BlockPos> canClearBlock) {
        BlockPos ladderBase = ladderBase();
        BlockPos towerBase = base;
        for (int height = 0; height < 3; height++) {
            BlockPos supportPos = towerBase.above(height);
            BlockState support = level.getBlockState(supportPos);
            if (!support.isCollisionShapeFullBlock(level, supportPos)
                    && !support.canBeReplaced()) {
                return false;
            }

            BlockState ladderSpace =
                    level.getBlockState(ladderBase.above(height));
            if (!ladderSpace.is(Blocks.LADDER)
                    && !ladderSpace.canBeReplaced()
                    && !canClearBlock.test(ladderBase.above(height))) {
                return false;
            }
        }

        BlockPos platformCenter = towerBase.above(3);
        BlockPos ladderOpening = ladderBase.above(3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = platformCenter.offset(x, 0, z);
                if (platformPos.equals(ladderOpening)) {
                    continue;
                }
                BlockState platformState = level.getBlockState(platformPos);
                if (!platformState.isCollisionShapeFullBlock(
                                level, platformPos)
                        && !platformState.canBeReplaced()) {
                    return false;
                }
            }
        }
        BlockState exitSpace = level.getBlockState(ladderOpening);
        if (!exitSpace.is(Blocks.LADDER) && !exitSpace.canBeReplaced()
                && !canClearBlock.test(ladderOpening)) {
            return false;
        }

        // The engineer must be able to stand anywhere on the completed deck.
        // Reject a tower site only when one of the three clearance layers
        // contains a block that the engineer cannot remove.
        for (int clearanceHeight = 1; clearanceHeight <= 3;
                clearanceHeight++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos clearancePos = platformCenter.offset(
                            x, clearanceHeight, z);
                    BlockState clearanceState =
                            level.getBlockState(clearancePos);
                    if (!clearanceState.isAir()
                            && !canClearBlock.test(clearancePos)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<ModifyBlockEntry> plan(Level level, BlockState planks, ToIntFunction<BlockPos> removalCost) {
        BlockPos ladderBase = ladderBase();
        BlockPos towerBase = base;
        List<ModifyBlockEntry> entries = new ArrayList<>(42);
        BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, ladderFacing);

        BlockPos platformCenter = towerBase.above(3);

        // Phase 1: clear three full blocks of headroom before placing any
        // ladders, so the engineer cannot climb into an unfinished exit.
        entries.addAll(clearancePlan(level, removalCost));

        for (int height = 0; height <= 3; height++) {
            BlockPos pos = ladderBase.above(height);
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.LADDER) && !state.canBeReplaced()) {
                entries.add(ModifyBlockEntry.ofDeletion(pos, removalCost.applyAsInt(pos)));
            }
        }

        // Phase 2: three solid support blocks, accepting existing full blocks.
        for (int height = 0; height < 3; height++) {
            BlockPos supportPos = towerBase.above(height);
            if (!level.getBlockState(supportPos)
                    .isCollisionShapeFullBlock(level, supportPos)) {
                entries.add(new ModifyBlockEntry(
                        supportPos, planks, 45));
            }
        }

        // Phase 3: ladders on the side of the column facing the engineer.
        for (int height = 0; height < 3; height++) {
            BlockPos ladderPos = ladderBase.above(height);
            if (level.getBlockState(ladderPos) != ladder) {
                entries.add(new ModifyBlockEntry(
                        ladderPos, ladder, 25));
            }
        }

        // Phase 4: build the platform centre first so the exit ladder has
        // support. Placing the ladder immediately afterwards guarantees that
        // the climbable column reaches through the platform before the
        // remaining deck blocks are filled in.
        BlockPos ladderOpening = ladderBase.above(3);
        if (level.getBlockState(platformCenter).canBeReplaced()) {
            entries.add(new ModifyBlockEntry(
                    platformCenter, planks, 45));
        }
        if (level.getBlockState(ladderOpening) != ladder) {
            entries.add(new ModifyBlockEntry(
                    ladderOpening, ladder, 25));
        }

        // Phase 5: complete the 3x3 platform footprint. The ladder cell stays
        // open as the only way through the deck.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = platformCenter.offset(x, 0, z);
                if (!platformPos.equals(platformCenter)
                        && !platformPos.equals(ladderOpening)
                        && level.getBlockState(platformPos).canBeReplaced()) {
                    entries.add(new ModifyBlockEntry(
                            platformPos, planks, 45));
                }
            }
        }

        return entries;
    }

    public List<ModifyBlockEntry> clearancePlan(Level level, ToIntFunction<BlockPos> removalCost) {
        BlockPos platformCenter = center();
        List<ModifyBlockEntry> entries = new ArrayList<>(27);
        for (int clearanceHeight = 1; clearanceHeight <= 3;
                clearanceHeight++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos clearancePos = platformCenter.offset(
                            x, clearanceHeight, z);
                    if (!level.getBlockState(clearancePos).isAir()) {
                        entries.add(ModifyBlockEntry.ofDeletion(
                                clearancePos,
                                removalCost.applyAsInt(clearancePos)));
                    }
                }
            }
        }
        return entries;
    }

}
