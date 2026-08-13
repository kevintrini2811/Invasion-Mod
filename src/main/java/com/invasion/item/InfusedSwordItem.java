package com.invasion.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class InfusedSwordItem extends Item {
    private static final int REQUIRED_DAMAGE = 100;
    private static final int CHARGE_SCALE = 1000;
    private static final String CHARGE_TAG = "invmodInfusedSwordCharge";

    public InfusedSwordItem(Properties properties) {
        super(properties.stacksTo(1).sword(CustomToolMaterial.INFUSED_GOLD, 3.0F, -2.4F));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Charge is restored from the final damage value in InfusedSwordChargeHandler.
    }

    public static void addDamageCharge(ItemStack stack, float inflictedDamage) {
        if (!(stack.getItem() instanceof InfusedSwordItem) || !stack.isDamaged()
                || inflictedDamage <= 0.0F) {
            return;
        }

        int currentCharge = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(CHARGE_TAG, chargeFromDamageBar(stack));
        int newCharge = Math.min(REQUIRED_DAMAGE * CHARGE_SCALE,
                currentCharge + Math.round(inflictedDamage * CHARGE_SCALE));

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (newCharge >= REQUIRED_DAMAGE * CHARGE_SCALE) {
                tag.remove(CHARGE_TAG);
            } else {
                tag.putInt(CHARGE_TAG, newCharge);
            }
        });
        int missingCharge = REQUIRED_DAMAGE * CHARGE_SCALE - newCharge;
        stack.setDamageValue((int)Math.ceil((double)missingCharge / CHARGE_SCALE));
    }

    private static int chargeFromDamageBar(ItemStack stack) {
        return Math.max(0, REQUIRED_DAMAGE - stack.getDamageValue()) * CHARGE_SCALE;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        Tool toolComponent = stack.get(DataComponents.TOOL);
        return toolComponent != null ? toolComponent.getMiningSpeed(state) : 1.0F;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state,
            BlockPos pos, LivingEntity miner) {
        return true;
    }

    // get break speed
    // should be getStrVsBlock
    /*
     * @Override public float func_150893_a(ItemStack par1ItemStack, Block
     * par2Block) { if (par2Block == Blocks.web) { return 15.0F; }
     *
     * Material material = par2Block.getMaterial(); return (material !=
     * Material.plants) && (material != Material.vine) && (material !=
     * Material.coral) && (material != Material.leaves) && (material !=
     * Material.sponge) && (material != Material.cactus) ? 1.0F : 1.5F; }
     */

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isDamaged()) {
            return InteractionResult.FAIL;
        }
        // if player isSneaking then refill hunger else refill health
        if (player.isShiftKeyDown()) {
            player.getFoodData().eat(6, 0.5f);
            world.playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP, player.getSoundSource(),
                    0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        } else {
            player.heal(6.0F);

            // spawn heart particles around the player
            if (world instanceof ServerLevel sw) {
                sw.sendParticles(ParticleTypes.HEART, player.getX() + 1.5D, player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
                sw.sendParticles(ParticleTypes.HEART, player.getX() - 1.5D, player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
                sw.sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY(), player.getZ() + 1.5D, 1, 0, 0, 0, 0);
                sw.sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY(), player.getZ() - 1.5D, 1, 0, 0, 0, 0);
            }

        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(CHARGE_TAG, 0));
        stack.setDamageValue(REQUIRED_DAMAGE);
        return InteractionResult.SUCCESS;
    }
}
