package com.invasion.entity;

import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.wave.BudgetWavePlan;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The exceptionally rare ???? zombie variant. */
public final class MysteryZombieEntity extends EntityIMZombie {
    private boolean releasedMob;
    private boolean suppressRelease;

    public MysteryZombieEntity(
            EntityType<? extends EntityIMZombie> type, Level world) {
        super(type, world);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
    }

    public void suppressReleaseOnNexusDeath() {
        suppressRelease = true;
    }

    @Override
    public void die(DamageSource source) {
        if (!releasedMob && !suppressRelease
                && level() instanceof ServerLevel world) {
            releasedMob = true;
            EntityConstruct construct = BudgetWavePlan.randomMobConstruct(
                    getRandom());
            Mob replacement = construct.createMob(world, getNexus());
            if (replacement != null) {
                equipDeathSpawn(replacement);
                replacement.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                replacement.setDeltaMovement(getDeltaMovement());
                if (replacement instanceof EntityIMLiving imMob) {
                    imMob.setCountsTowardMobCap(countsTowardMobCap());
                }
                world.addFreshEntity(replacement);
            }
        }
        super.die(source);
    }

    private void equipDeathSpawn(Mob mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()
                    || !EquipmentUtil.canUseRandomArmor(mob, slot)
                    || !getRandom().nextBoolean()) {
                continue;
            }
            List<Item> armor = findEquipment(mob, slot, true);
            if (!armor.isEmpty()) {
                mob.setItemSlot(slot, armor.get(
                        getRandom().nextInt(armor.size())).getDefaultInstance());
            }
        }

        if (!EquipmentUtil.canUseRandomWeapon(mob)
                || !getRandom().nextBoolean()) {
            return;
        }
        List<Item> weapons = findEquipment(
                mob, EquipmentSlot.MAINHAND, false);
        if (!weapons.isEmpty()) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapons.get(
                    getRandom().nextInt(weapons.size())).getDefaultInstance());
        }
    }

    private static List<Item> findEquipment(
            Mob mob, EquipmentSlot slot, boolean armor) {
        return BuiltInRegistries.ITEM.stream().filter(item -> {
            ItemStack stack = item.getDefaultInstance();
            return (armor
                            ? EquipmentUtil.isHumanoidArmor(stack)
                            : EquipmentUtil.isWeapon(stack))
                    && mob.getEquipmentSlotForItem(stack) == slot
                    && stack.canEquip(slot, mob)
                    && mob.canHoldItem(stack);
        }).toList();
    }
}
