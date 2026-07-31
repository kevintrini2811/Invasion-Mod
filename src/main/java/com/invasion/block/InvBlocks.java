package com.invasion.block;

import com.invasion.InvasionMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public interface InvBlocks {
    NexusBlock NEXUS_CORE = register("nexus_core", new NexusBlock(Properties.of()
            .explosionResistance(6000000).destroyTime(3).sound(SoundType.GLASS).emissiveRendering((state, level, pos) -> true)
            .lightLevel(state -> state.getValue(NexusBlock.LIT) ? 15 : 8)
    ));

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, InvasionMod.id(name), block);
    }

    static void bootstrap() {
        var ignored = NEXUS_CORE;
    }
}
