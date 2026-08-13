package com.invasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

public class InfusedSwordItem extends SwordItem {
    private static final int REQUIRED_DAMAGE = 100;
    private static final int CHARGE_SCALE = 1000;
    private static final String CHARGE_TAG = "invmodInfusedSwordCharge";

    public InfusedSwordItem(Properties properties) {
        super(CustomToolMaterial.INFUSED_GOLD,
                properties.stacksTo(1).attributes(
                        SwordItem.createAttributes(
                                CustomToolMaterial.INFUSED_GOLD, 3, -2.4F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state,
            BlockPos pos, LivingEntity miner) {
        return true;
    }

    public static void addDamageCharge(ItemStack stack, float inflictedDamage) {
        if (!(stack.getItem() instanceof InfusedSwordItem) || !stack.isDamaged()
                || inflictedDamage <= 0.0F) return;
        var chargeData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int currentCharge = chargeData.contains(CHARGE_TAG)
                ? chargeData.getInt(CHARGE_TAG)
                : Math.max(0, REQUIRED_DAMAGE - stack.getDamageValue()) * CHARGE_SCALE;
        int newCharge = Math.min(REQUIRED_DAMAGE * CHARGE_SCALE,
                currentCharge + Math.round(inflictedDamage * CHARGE_SCALE));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (newCharge >= REQUIRED_DAMAGE * CHARGE_SCALE) tag.remove(CHARGE_TAG);
            else tag.putInt(CHARGE_TAG, newCharge);
        });
        stack.setDamageValue((int)Math.ceil(
                (double)(REQUIRED_DAMAGE * CHARGE_SCALE - newCharge) / CHARGE_SCALE));
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
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(CHARGE_TAG, 0));
        stack.setDamageValue(REQUIRED_DAMAGE);
        return InteractionResultHolder.success(stack);
    }
}
