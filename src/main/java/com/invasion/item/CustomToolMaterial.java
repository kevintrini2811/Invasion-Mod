package com.invasion.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

record CustomToolMaterial(
        TagKey<Block> inverseTag,
        int durability,
        float miningSpeedMultiplier,
        float attackDamage,
        int enchantability,
        Ingredient repairIngredient) implements Tier {
    static final CustomToolMaterial INFUSED_GOLD = new CustomToolMaterial(
            null, 40, 12, 4, 22,
            Ingredient.of(Items.GOLD_INGOT));

    @Override public int getUses() { return durability; }
    @Override public float getSpeed() { return miningSpeedMultiplier; }
    @Override public float getAttackDamageBonus() { return attackDamage; }
    @Override public int getLevel() { return 2; }
    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public Ingredient getRepairIngredient() { return repairIngredient; }
}
