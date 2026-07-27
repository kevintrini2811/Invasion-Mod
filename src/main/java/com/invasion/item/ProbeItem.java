package com.invasion.item;

import org.jetbrains.annotations.Nullable;

import com.invasion.block.BlockMetadata;
import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import com.invasion.nexus.ControllableNexusAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

class ProbeItem extends Item {
    private final boolean isProbe;

    public ProbeItem(Properties settings, boolean isProbe) {
        super(settings);
        this.isProbe = isProbe;
    }

    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        @Nullable
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        if (state.is(InvBlocks.NEXUS_CORE)) {
            if (((NexusBlockEntity) world.getBlockEntity(pos)).getNexus() instanceof ControllableNexusAccess nexus) {
                int newRange = nexus.getSpawnRadius();

                // check if the player wants to increase or decrease the range
                newRange += player.isShiftKeyDown() ? -8 : 8;
                // TODO: this check should be handled by the block entity, not here
                newRange = Mth.clamp(newRange, 32, 128);

                if (nexus.setSpawnRadius(newRange)) {
                    player.sendSystemMessage(Component.translatable("invmod.message.probe.rangechanged", Component.literal(nexus.getSpawnRadius() + "").withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.DARK_GREEN));
                } else if (nexus.isActive()) {
                    player.sendSystemMessage(Component.translatable("invmod.message.probe.cannotchangerange", Component.literal(nexus.getSpawnRadius() + "")).withStyle(ChatFormatting.RED));
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (isProbe) {
            float blockStrength = BlockMetadata.getStrength(pos, state, world);
            int strengthRounded = (int) ((blockStrength + 0.005D) * 100.0D) / 100;
            player.sendSystemMessage(Component.translatable("invmod.message.probe.blockstrength", Component.literal(strengthRounded + "").withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.DARK_GREEN));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
