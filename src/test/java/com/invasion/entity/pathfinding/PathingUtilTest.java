package com.invasion.entity.pathfinding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class PathingUtilTest {
    @Test
    void terrainObstructionsPreservePassableBlockExceptions() {
        assertTrue(PathingUtil.blocksMotion(Blocks.STONE.defaultBlockState()));
        assertFalse(PathingUtil.blocksMotion(Blocks.AIR.defaultBlockState()));
        assertFalse(PathingUtil.blocksMotion(Blocks.WATER.defaultBlockState()));
        assertFalse(PathingUtil.blocksMotion(Blocks.COBWEB.defaultBlockState()));
        assertFalse(PathingUtil.blocksMotion(Blocks.BAMBOO_SAPLING.defaultBlockState()));
    }
}
