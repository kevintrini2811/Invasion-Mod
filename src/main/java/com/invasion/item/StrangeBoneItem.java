package com.invasion.item;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

import com.invasion.entity.IMWolfEntity;
import com.invasion.entity.InvEntities;
import com.invasion.block.InvBlocks;
import com.invasion.block.NexusBlockEntity;
import com.invasion.nexus.Mode;
import com.invasion.nexus.Nexus;
import com.invasion.nexus.NexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

class StrangeBoneItem extends Item {
    public StrangeBoneItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(InvBlocks.NEXUS_CORE)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null
                || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof NexusBlockEntity blockEntity)
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

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Wolf wolf) || entity instanceof IMWolfEntity) {
            return InteractionResult.PASS;
        }
        if (entity.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (wolf.isTame() && !wolf.isOwnedBy(user)) {
            return InteractionResult.FAIL;
        }

        @Nullable
        NexusAccess nexus = entity.level() instanceof ServerLevel serverLevel
                ? WorldNexusStorage.of(serverLevel).getNexus()
                        .filter(NexusAccess::isActive)
                        .orElse(null)
                : null;

        if (nexus == null) {
            user.sendSystemMessage(Component.translatable("invmod.message.bone.nonearbynexus1").withStyle(ChatFormatting.RED));
            user.sendSystemMessage(Component.translatable("invmod.message.bone.nonearbynexus2").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        IMWolfEntity newWolf = InvEntities.WOLF.create(wolf.level());
        if (newWolf == null) {
            return InteractionResult.FAIL;
        }
        newWolf.restoreFrom(wolf);
        // restoreFrom also copies the original UUID. The old wolf still
        // exists while the replacement is added, so the server rejects the
        // duplicate unless the replacement receives its own identity.
        newWolf.setUUID(UUID.randomUUID());
        newWolf.setNexus(nexus);

        if (!wolf.level().addFreshEntity(newWolf)) {
            return InteractionResult.FAIL;
        }
        if (!wolf.isTame()) {
            newWolf.tame(user);
        }
        ItemStack heldItem = user.getItemInHand(hand);
        try {
            user.setItemInHand(hand, new ItemStack(Items.BLUE_DYE));
            newWolf.mobInteract(user, hand);
        } finally {
            user.setItemInHand(hand, heldItem);
        }
        wolf.discard();
        stack.consume(1, user);
        return InteractionResult.SUCCESS;
    }
}
