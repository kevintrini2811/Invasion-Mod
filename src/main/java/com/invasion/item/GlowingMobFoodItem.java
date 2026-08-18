package com.invasion.item;

import java.util.List;

import com.invasion.entity.BoundIMMobRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class GlowingMobFoodItem extends Item {
    private final int glowingDuration;

    public GlowingMobFoodItem(Properties properties, int glowingDuration) {
        super(properties);
        this.glowingDuration = glowingDuration;
    }

    @Override
    public ItemStack finishUsingItem(
            ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (level instanceof ServerLevel serverLevel) {
            BoundIMMobRegistry.loaded(serverLevel).stream()
                    .map(combatant -> (LivingEntity) combatant.asEntity())
                    .filter(entity -> entity.isAlive() && !entity.isRemoved())
                    .forEach(entity -> entity.addEffect(new MobEffectInstance(
                            MobEffects.GLOWING, glowingDuration)));
        }
        return result;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
