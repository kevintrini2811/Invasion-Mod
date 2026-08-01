package com.invasion.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.invasion.InvTags;
import com.invasion.InvasionMod;

import net.minecraftforge.common.Tags;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMetadata {
    public static final float AIR_STRENGTH = 0.01F;
    public static final float SHRUBBERY_STRENGTH = 0.3F;
    public static final float OVERGROWTH_STRENGTH = 1.25F;
    public static final float SOFT_STRENGTH = 2.5F;
    public static final float OVERGROWN_DIRT_STRENGTH = 3.125F;
    public static final float SEMI_HARD_STRENGTH = 3.85F;
    public static final float HARD_STRENGTH = 5.5F;
    public static final float HEAVY_MATERIAL_STRENGTH = 7.7F;
    public static final float QUEST_REWARD_STRENGTH = 15F;
    public static final float METAL_ENTRYWAY_STRENGTH = 15.4F;

    public static final float AIR_COST = 1;
    public static final float SOFT_DOOR_COST = 1.4F;
    public static final float SOFT_COST = 2;
    public static final float DOOR_COST = 2.24F;
    public static final float HARD_COST = 3.2F;

    private static final Lookup<Float> BLOCK_COSTS = Util.make(new Lookup<>(), costs -> {
        costs.put(BlockTags.CLIMBABLE, AIR_COST);
        costs.put(Tags.Blocks.STONE, HARD_COST);
        costs.put(BlockTags.STONE_BRICKS, HARD_COST);
        costs.put(Tags.Blocks.COBBLESTONE, HARD_COST);
        costs.put(Blocks.MOSSY_COBBLESTONE, HARD_COST);
        costs.put(Blocks.BRICKS, HARD_COST);
        costs.put(Blocks.OBSIDIAN, HARD_COST);
        costs.put(BlockTags.DIRT, SOFT_COST);
        costs.put(BlockTags.SAND, SOFT_COST);
        costs.put(Blocks.GRAVEL, SOFT_COST);
        costs.put(Tags.Blocks.GLASS, SOFT_COST);
        costs.put(Tags.Blocks.GLASS_PANES, SOFT_COST);
        costs.put(BlockTags.LEAVES, SOFT_COST);
        costs.put(Blocks.IRON_DOOR, DOOR_COST);
        costs.put(Blocks.IRON_TRAPDOOR, DOOR_COST);
        costs.put(BlockTags.WOODEN_DOORS, SOFT_DOOR_COST);
        costs.put(BlockTags.WOODEN_TRAPDOORS, SOFT_DOOR_COST);
        costs.put(Tags.Blocks.SANDSTONE, HARD_COST);
        costs.put(Tags.Blocks.SANDSTONE, HARD_COST);
        costs.put(Tags.Blocks.SANDSTONE, HARD_COST);
        costs.put(BlockTags.LOGS, HARD_COST);
        costs.put(BlockTags.PLANKS, HARD_COST);
        costs.put(Tags.Blocks.ORES, HARD_COST);
        costs.put(Tags.Blocks.STORAGE_BLOCKS_GOLD, HARD_COST);
        costs.put(Tags.Blocks.STORAGE_BLOCKS_DIAMOND, HARD_COST);
        costs.put(Tags.Blocks.STORAGE_BLOCKS_IRON, HARD_COST);
        costs.put(Tags.Blocks.STORAGE_BLOCKS_EMERALD, HARD_COST);
        costs.put(Tags.Blocks.STORAGE_BLOCKS_NETHERITE, HARD_COST);
        costs.put(Tags.Blocks.STORAGE_BLOCKS_COPPER, HARD_COST);
        costs.put(BlockTags.FENCES, HARD_COST);
        costs.put(BlockTags.FENCE_GATES, HARD_COST);
        costs.put(Blocks.NETHERRACK, HARD_COST);
        costs.put(Blocks.NETHER_BRICKS, HARD_COST);
        costs.put(Blocks.SOUL_SAND, SOFT_COST);
        costs.put(Blocks.GLOWSTONE, SOFT_COST);
        costs.put(BlockTags.SMALL_FLOWERS, AIR_COST);
        costs.put(BlockTags.FLOWERS, AIR_COST);
    });
    private static final Lookup<Float> BLOCK_STRENGTHS = Util.make(new Lookup<>(), strengths -> {
        strengths.put(Tags.Blocks.STONE, HARD_STRENGTH);
        strengths.put(BlockTags.STONE_BRICKS, HARD_STRENGTH);
        strengths.put(Tags.Blocks.COBBLESTONE, HARD_STRENGTH);
        strengths.put(Blocks.MOSSY_COBBLESTONE, HARD_STRENGTH);
        strengths.put(Blocks.BRICKS, HARD_STRENGTH);
        strengths.put(Blocks.OBSIDIAN, HEAVY_MATERIAL_STRENGTH);
        strengths.put(Blocks.IRON_BLOCK, HEAVY_MATERIAL_STRENGTH);
        strengths.put(BlockTags.DIRT, OVERGROWN_DIRT_STRENGTH);
        strengths.put(Blocks.GRASS_BLOCK, OVERGROWN_DIRT_STRENGTH);
        strengths.put(Blocks.MYCELIUM, OVERGROWN_DIRT_STRENGTH);
        strengths.put(Blocks.PODZOL, OVERGROWN_DIRT_STRENGTH);
        strengths.put(BlockTags.SAND, SOFT_STRENGTH);
        strengths.put(Blocks.GRAVEL, SOFT_STRENGTH);
        strengths.put(Tags.Blocks.GLASS, SOFT_STRENGTH);
        strengths.put(Tags.Blocks.GLASS_PANES, SOFT_STRENGTH);
        strengths.put(BlockTags.LEAVES, OVERGROWTH_STRENGTH);
        strengths.put(BlockTags.CAVE_VINES, OVERGROWTH_STRENGTH);
        strengths.put(Blocks.IRON_DOOR, METAL_ENTRYWAY_STRENGTH);
        strengths.put(Blocks.IRON_TRAPDOOR, METAL_ENTRYWAY_STRENGTH);
        strengths.put(BlockTags.WOODEN_DOORS, HARD_STRENGTH);
        strengths.put(Tags.Blocks.SANDSTONE, HARD_STRENGTH);
        strengths.put(Tags.Blocks.SANDSTONE, HARD_STRENGTH);
        strengths.put(Tags.Blocks.SANDSTONE, HARD_STRENGTH);
        strengths.put(BlockTags.LOGS, HARD_STRENGTH);
        strengths.put(BlockTags.PLANKS, HARD_STRENGTH);
        strengths.put(Tags.Blocks.STORAGE_BLOCKS_GOLD, HARD_STRENGTH);
        strengths.put(Tags.Blocks.STORAGE_BLOCKS_DIAMOND, HARD_STRENGTH);
        strengths.put(Tags.Blocks.STORAGE_BLOCKS_IRON, HARD_STRENGTH);
        strengths.put(Tags.Blocks.STORAGE_BLOCKS_EMERALD, HARD_STRENGTH);
        strengths.put(Tags.Blocks.STORAGE_BLOCKS_NETHERITE, HEAVY_MATERIAL_STRENGTH);
        strengths.put(Tags.Blocks.STORAGE_BLOCKS_COPPER, HARD_STRENGTH);
        strengths.put(BlockTags.FENCES, HARD_STRENGTH);
        strengths.put(BlockTags.FENCE_GATES, HARD_STRENGTH);
        strengths.put(Blocks.NETHERRACK, 3.85F);
        strengths.put(Blocks.NETHER_BRICKS, HARD_STRENGTH);
        strengths.put(Blocks.SOUL_SAND, SOFT_STRENGTH);
        strengths.put(Blocks.GLOWSTONE, SOFT_STRENGTH);
        strengths.put(BlockTags.SMALL_FLOWERS, SHRUBBERY_STRENGTH);
        strengths.put(BlockTags.FLOWERS, SHRUBBERY_STRENGTH);
        strengths.put(Blocks.DRAGON_EGG, QUEST_REWARD_STRENGTH);
    });
    private static final List<Block> UNDESTRUCTABLE_BLOCKS = List.of(
            Blocks.BEDROCK,
            Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.END_PORTAL_FRAME, Blocks.LADDER,
            Blocks.NETHER_PORTAL, Blocks.END_GATEWAY, Blocks.END_PORTAL
    );

    public static boolean isIndestructible(BlockState state) {
        return state.getBlock().defaultDestroyTime() < 0
                || UNDESTRUCTABLE_BLOCKS.contains(state.getBlock());
    }

    public static Optional<Float> getCost(BlockState state) {
        return InvasionMod.getConfig().getBlockCost(state.getBlock()).or(() -> BLOCK_COSTS.get(state));
    }

    public static float getStrength(BlockPos pos, BlockState state, CollisionGetter world) {
        int bonus = 0;
        BlockPos.MutableBlockPos mutable = pos.mutable();
        float strength = InvasionMod.getConfig().getBlockStrength(state.getBlock())
                .orElseGet(() -> BLOCK_STRENGTHS.get(state).orElse(state.getDestroySpeed(world, pos)));
        switch (BlockSpecial.of(state)) {
            case CONSTRUCTION_BRICKS:
                for (Direction direction : Direction.values()) {
                    if (world.getBlockState(mutable.set(pos).move(direction)).is(state.getBlock())) {
                        bonus++;
                    }
                }
                break;
            case CONSTRUCTION_STONE:
                for (Direction direction : Direction.values()) {
                    if (world.getBlockState(mutable.set(pos).move(direction)).is(InvTags.Blocks.STONE_CONSTRUCTION_BONUS_MATERIALS)) {
                        bonus++;
                    }
                }
                break;
            default:
        }
        return strength * (1 + bonus * 0.1F);
    }

    static final class Lookup<T> {
        private final List<Entry<T>> entries = new ArrayList<>();

        public Lookup<T> put(Block block, T value) {
            entries.add(new Entry<>(b -> b == block, value));
            return this;
        }

        @SuppressWarnings("deprecation")
        public Lookup<T> put(TagKey<Block> tag, T value) {
            entries.add(new Entry<>(b -> b.builtInRegistryHolder().is(tag), value));
            return this;
        }

        public Optional<T> get(BlockState state) {
            return get(state.getBlock());
        }

        public Optional<T> get(Block block) {
            for (Entry<T> i : entries) {
                if (i.predicate().test(block)) {
                    return Optional.of(i.value());
                }
            }
            return Optional.empty();
        }

        record Entry<T>(Predicate<Block> predicate, T value) { }
    }
}
