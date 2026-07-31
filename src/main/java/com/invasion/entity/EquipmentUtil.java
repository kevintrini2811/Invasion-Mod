package com.invasion.entity;

import com.invasion.item.InvItems;
import net.minecraftforge.common.Tags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.DiggerItem;

public final class EquipmentUtil {
    private EquipmentUtil() {
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(InvItems.SEARING_BOW);
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof DiggerItem
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(InvItems.INFUSED_SWORD);
    }

    public static boolean isWeapon(ItemStack stack) {
        return isMeleeWeapon(stack) || isRangedWeapon(stack);
    }

    public static boolean isHumanoidArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }
}
