package com.invasion.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;

final class CustomToolMaterial {
    static final ToolMaterial INFUSED_GOLD = new ToolMaterial(
            BlockTags.INCORRECT_FOR_GOLD_TOOL,
            101,
            12.0F,
            4.0F,
            22,
            ItemTags.GOLD_TOOL_MATERIALS
    );

    private CustomToolMaterial() {
    }
}
