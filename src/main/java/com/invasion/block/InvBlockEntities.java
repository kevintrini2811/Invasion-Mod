package com.invasion.block;

import com.invasion.InvasionMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Set;

public interface InvBlockEntities {
    BlockEntityType<NexusBlockEntity> NEXUS = register(
            "nexus",
            new BlockEntityType<>(NexusBlockEntity::new, Set.of(InvBlocks.NEXUS_CORE), null)
    );

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, InvasionMod.id(name), type);
    }

    static void bootstrap() { }
}
