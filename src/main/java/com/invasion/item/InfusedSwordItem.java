package com.invasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

public class InfusedSwordItem extends SwordItem {
    public InfusedSwordItem(Properties properties) {
        super(CustomToolMaterial.INFUSED_GOLD, properties.stacksTo(1));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (stack.isDamaged()) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isDamaged()) {
            return InteractionResultHolder.fail(stack);
        }
        if (player.isShiftKeyDown()) {
            player.getFoodData().eat(6, 0.5F);
            world.playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP,
                    player.getSoundSource(), 0.5F,
                    world.getRandom().nextFloat() * 0.1F + 0.9F);
        } else {
            player.heal(6.0F);
            if (world instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, player.getX() + 1.5D, player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
                server.sendParticles(ParticleTypes.HEART, player.getX() - 1.5D, player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
                server.sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY(), player.getZ() + 1.5D, 1, 0, 0, 0, 0);
                server.sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY(), player.getZ() - 1.5D, 1, 0, 0, 0, 0);
            }
        }
        stack.setDamageValue(getTier().getUses());
        return InteractionResultHolder.success(stack);
    }
}
