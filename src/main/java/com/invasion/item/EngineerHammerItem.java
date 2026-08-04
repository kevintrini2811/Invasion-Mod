package com.invasion.item;

import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import com.invasion.nexus.Mode;
import com.invasion.nexus.Nexus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

class EngineerHammerItem extends Item {
    EngineerHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(InvBlocks.NEXUS_CORE)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null
                || !(level.getBlockEntity(context.getClickedPos()) instanceof NexusBlockEntity blockEntity)
                || !(blockEntity.getNexus() instanceof Nexus nexus)) {
            return InteractionResult.FAIL;
        }

        if (nexus.getMode() == Mode.DEBUG) {
            nexus.stop(false);
            player.sendSystemMessage(Component.translatable("invmod.message.bone.debugdisabled")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }

        if (nexus.startDebugMode()) {
            player.sendSystemMessage(Component.translatable("invmod.message.bone.debugactivated")
                    .withStyle(ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("invmod.message.bone.debugfailed")
                .withStyle(ChatFormatting.RED));
        return InteractionResult.FAIL;
    }
}
