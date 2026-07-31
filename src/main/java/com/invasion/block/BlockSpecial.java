package com.invasion.block;

import java.util.Arrays;
import java.util.List;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.invasion.InvTags;

public enum BlockSpecial {
    CONSTRUCTION_BRICKS(InvTags.Blocks.BRITTLE_CONSTRUCTION_MATERIALS),
    CONSTRUCTION_STONE(InvTags.Blocks.SOLID_CONSTRUCTION_MATERIALS),
    DEFLECTION(InvTags.Blocks.REPULSIVE_CONSTRUCTION_MATERIALS),
    NONE(null);

    private static final List<BlockSpecial> VALUES = Arrays.asList(values());

    private final TagKey<Block> tag;

    BlockSpecial(TagKey<Block> tag) {
        this.tag = tag;
    }

    public static BlockSpecial of(BlockState state) {
        for (BlockSpecial special : VALUES) {
            if (special.tag != null && state.is(special.tag)) {
                return special;
            }
        }
        return NONE;
    }
}
