package com.invasion.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SearingBowItem extends BowItem {
    static final float SEARING_ACTIVATION_PULL_PERCENTAGE = 3.8F;
    public SearingBowItem(Properties settings) {
        super(settings);
    }

    public static float getUncappedPullProgress(int useTicks) {
        float f = useTicks / 20F;
        return (f * f + f * 2) / 3F;
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target) {
        super.shootProjectile(shooter, projectile, index, speed, divergence, yaw, target);
        float pullProgress = getUncappedPullProgress(getUseDuration(getDefaultInstance(), shooter) - shooter.getUseItemRemainingTicks());
        if (pullProgress > SEARING_ACTIVATION_PULL_PERCENTAGE) {
            projectile.setRemainingFireTicks(100);
            if (projectile instanceof AbstractArrow p) {
                p.setBaseDamage(4.0D);
            }
        }
    }

    @Override
    protected int getDurabilityUse(ItemStack projectile) {
        return 0;
    }
}
