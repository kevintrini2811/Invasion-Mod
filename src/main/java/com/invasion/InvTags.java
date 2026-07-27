package com.invasion;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public interface InvTags {

    interface Entities {
        TagKey<EntityType<?>> QUEEN_SPIDER_OFFSPRING = entity("queen_spider_offspring");

        private static TagKey<EntityType<?>> entity(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, InvasionMod.id(name));
        }
    }

    interface Blocks {
        TagKey<Block> STONE_CONSTRUCTION_BONUS_MATERIALS = block("stone_construction_bonus_materials");
        TagKey<Block> SOLID_CONSTRUCTION_MATERIALS = block("solid_construction_materials");
        TagKey<Block> BRITTLE_CONSTRUCTION_MATERIALS = block("brittle_construction_materials");
        TagKey<Block> REPULSIVE_CONSTRUCTION_MATERIALS = block("repulsive_construction_materials");

        private static TagKey<Block> block(String name) {
            return TagKey.create(Registries.BLOCK, InvasionMod.id(name));
        }
    }
}
