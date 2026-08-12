package com.invasion.entity;

import com.invasion.item.InvItems;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;

public final class EquipmentUtil {
    private EquipmentUtil() {
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return stack.is(Tags.Items.RANGED_WEAPON_TOOLS)
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW)
                || stack.is(InvItems.SEARING_BOW);
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return stack.is(Tags.Items.MELEE_WEAPON_TOOLS)
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE)
                || stack.is(InvItems.INFUSED_SWORD);
    }

    public static boolean isWeapon(ItemStack stack) {
        return isMeleeWeapon(stack) || isRangedWeapon(stack);
    }

    public static boolean isHumanoidArmor(ItemStack stack) {
        return stack.is(Tags.Items.ARMORS_HUMANOID)
                || stack.is(ItemTags.HEAD_ARMOR)
                || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR)
                || stack.is(ItemTags.FOOT_ARMOR);
    }

    public static boolean canUseRandomWeapon(Mob mob) {
        return mob instanceof EntityIMZombie
                || mob instanceof EntityIMZombiePigman
                || mob instanceof IMZombifiedPiglinEntity
                || mob instanceof ImpEnitty;
    }

    public static boolean canUseRandomArmor(Mob mob, EquipmentSlot slot) {
        boolean supported = mob instanceof IMSkeletonEntity
                || mob instanceof PigmanEngineerEntity
                || mob instanceof EntityIMZombie
                || mob instanceof EntityIMZombiePigman
                || mob instanceof IMZombifiedPiglinEntity
                || mob instanceof IMCreeperEntity
                || mob instanceof NexusSpiderEntity
                || mob instanceof IMEndermanEntity
                || mob instanceof IMBlazeEntity
                || mob instanceof IMGhastEntity;
        if (!supported) {
            return false;
        }

        boolean helmetOnly = mob instanceof IMCreeperEntity
                || mob instanceof NexusSpiderEntity
                || mob instanceof IMEndermanEntity
                || mob instanceof IMBlazeEntity
                || mob instanceof IMGhastEntity;
        if (helmetOnly && slot != EquipmentSlot.HEAD) {
            return false;
        }

        boolean brute = mob instanceof EntityIMZombie zombie && zombie.isBrute()
                || mob instanceof EntityIMZombiePigman pigman && pigman.isBrute();
        if (brute && slot != EquipmentSlot.HEAD
                && slot != EquipmentSlot.LEGS
                && slot != EquipmentSlot.FEET) {
            return false;
        }
        return !(mob instanceof IMZombifiedPiglinEntity)
                || slot != EquipmentSlot.HEAD;
    }
}
