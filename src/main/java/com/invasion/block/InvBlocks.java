package com.invasion.block;

import com.invasion.InvasionMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public interface InvBlocks {
    NexusBlock NEXUS_CORE = registerNexusCore();

    private static NexusBlock registerNexusCore() {
        var id = InvasionMod.id("nexus_core");
        var key = ResourceKey.create(Registries.BLOCK, id);
        var block = new NexusBlock(Properties.of()
                .explosionResistance(6000000).destroyTime(3).sound(SoundType.GLASS)
                .emissiveRendering((state, getter, pos) -> true)
                .lightLevel(state -> state.getValue(NexusBlock.LIT) ? 15 : 8));
        return InvasionMod.INSTANCE.register(Registries.BLOCK, id, block);
    }

    static void bootstrap() {
    }
}
