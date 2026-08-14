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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;

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
        return stack.getItem() instanceof ArmorItem
                || stack.is(Tags.Items.ARMORS);
    }

    public static boolean canUseRandomWeapon(Mob mob) {
        return mob instanceof EntityIMZombie
                || mob instanceof EntityIMZombiePigman
                || mob instanceof IMZombifiedPiglinEntity
                || mob instanceof ImpEnitty;
    }

    public static boolean canUseRandomArmor(Mob mob, EquipmentSlot slot) {
        if (mob instanceof IMFatZombieEntity) {
            return false;
        }
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
