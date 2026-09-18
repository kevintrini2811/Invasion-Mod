package com.invasion.entity.ai.builder;

import com.invasion.InvasionMod;
import com.invasion.compat.ConfiguredModMobs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persists tower footprints independently of their builders and building materials. */
public final class EngineerTowerStorage extends SavedData {
    private static final Codec<EngineerTower> TOWER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("base").forGetter(EngineerTower::base),
            Direction.CODEC.fieldOf("facing").forGetter(EngineerTower::ladderFacing)
    ).apply(instance, EngineerTower::new));
    private static final SavedDataType<EngineerTowerStorage> TYPE = new SavedDataType<>(
            InvasionMod.id("engineer_towers"), EngineerTowerStorage::new,
            TOWER_CODEC.listOf().xmap(EngineerTowerStorage::new, storage -> List.copyOf(storage.towers)), null);
    private final Set<EngineerTower> towers = new LinkedHashSet<>();

    public EngineerTowerStorage() {}

    private EngineerTowerStorage(List<EngineerTower> saved) {
        saved.stream().filter(tower -> tower.ladderFacing().getAxis().isHorizontal()).forEach(towers::add);
    }

    public static EngineerTowerStorage of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void remember(EngineerTower tower) {
        if (towers.add(tower)) setDirty();
    }

    public List<EngineerTower> nearby(ServerLevel level, BlockPos current, BlockPos objective) {
        List<EngineerTower> found = new ArrayList<>();
        for (EngineerTower tower : towers) {
            if (inRange(tower, current, objective) && tower.isLoaded(level) && tower.hasRemains(level)) {
                found.add(tower);
            }
        }
        // Older worlds have no saved footprints. Discover their columns and ladders once.
        int radius = EngineerTower.SEARCH_RADIUS;
        Set<Block> materials = ConfiguredModMobs.towerMaterials();
        for (BlockPos candidate : BlockPos.betweenClosed(current.offset(-radius, -2, -radius),
                current.offset(radius, 2, radius))) {
            if (!level.hasChunkAt(candidate) || !materials.contains(level.getBlockState(candidate).getBlock())) continue;
            BlockPos base = candidate.immutable();
            if (towers.stream().anyMatch(tower -> Math.abs(tower.base().getX() - base.getX()) <= 1
                    && Math.abs(tower.base().getZ() - base.getZ()) <= 1
                    && Math.abs(tower.base().getY() - base.getY()) <= 3)) continue;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                EngineerTower tower = new EngineerTower(base, facing);
                if (inRange(tower, current, objective) && tower.isLoaded(level) && isLegacyTower(level, tower, materials)) {
                    found.add(tower);
                    remember(tower);
                    break;
                }
            }
        }
        found.sort(Comparator.comparingDouble(tower -> tower.ladderBase().distSqr(current)));
        return found;
    }

    static boolean inRange(EngineerTower tower, BlockPos current, BlockPos objective) {
        return Math.abs(tower.base().getX() - current.getX()) <= EngineerTower.SEARCH_RADIUS
                && Math.abs(tower.base().getZ() - current.getZ()) <= EngineerTower.SEARCH_RADIUS
                && tower.center().getY() >= current.getY() + 1
                && tower.center().getY() <= current.getY() + 5
                && tower.center().getY() <= objective.getY();
    }

    public static boolean isLegacyTower(Level level, EngineerTower tower) {
        return isLegacyTower(level, tower, ConfiguredModMobs.towerMaterials());
    }

    private static boolean isLegacyTower(Level level, EngineerTower tower, Set<Block> materials) {
        if (!tower.hasRemains(level)) return false;
        int column = 0;
        int ladders = 0;
        for (int height = 0; height <= 3; height++) {
            BlockPos support = tower.base().above(height);
            if (materials.contains(level.getBlockState(support).getBlock())) column++;
            BlockState ladder = level.getBlockState(tower.ladderBase().above(height));
            if (ladder.is(Blocks.LADDER)) {
                if (ladder.getValue(LadderBlock.FACING) != tower.ladderFacing()) return false;
                ladders++;
            }
        }
        if (ladders > 0 && column > 0) return true;
        if (column < 2) return false;
        int deck = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = tower.center().offset(x, 0, z);
                if (!pos.equals(tower.ladderBase().above(3))
                        && materials.contains(level.getBlockState(pos).getBlock())) deck++;
            }
        }
        if (deck >= 6) return true;
        // Two isolated column blocks also identify an unfinished tower without ladders.
        // Reject broad terrain and walls; never reinterpret a flat bridge as a tower.
        for (int height = 0; height < 3; height++) {
            BlockPos support = tower.base().above(height);
            if (!materials.contains(level.getBlockState(support).getBlock())) continue;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = support.relative(side);
                if (level.getBlockState(neighbor).isCollisionShapeFullBlock(level, neighbor)) return false;
            }
        }
        return true;
    }
}
