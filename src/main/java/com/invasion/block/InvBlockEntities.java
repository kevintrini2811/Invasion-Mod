package com.invasion.block;

import com.invasion.InvasionMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface InvBlockEntities {
    BlockEntityType<NexusBlockEntity> NEXUS = register("nexus", BlockEntityType.Builder.of(NexusBlockEntity::new, InvBlocks.NEXUS_CORE));

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, InvasionMod.id(name), builder.build(null));
    }

    static void bootstrap() {
        var ignored = NEXUS;
    }
}
