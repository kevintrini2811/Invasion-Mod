package com.invasion.block;

import com.invasion.nexus.Nexus;
import com.invasion.nexus.WorldNexusStorage;
import org.jetbrains.annotations.Nullable;

import com.invasion.item.InvItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class NexusBlock extends BaseEntityBlock {
    private static final MapCodec<NexusBlock> CODEC = Block.simpleCodec(NexusBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public NexusBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world,
                                             BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        // SERVER: Nexus für die Commands merken
        if (!world.isClientSide()) {
            ServerLevel sw = (ServerLevel) world;

            world.getBlockEntity(pos, InvBlockEntities.NEXUS).ifPresent(be -> {
                NexusBlockEntity nexusBe = (NexusBlockEntity) be;

                // Über die BlockEntity den Nexus holen (erzwingt, dass er in WorldNexusStorage existiert)
                var access = nexusBe.getNexus();
                if (access instanceof Nexus nexus) {
                    WorldNexusStorage storage = WorldNexusStorage.of(sw);
                    if (storage.setActiveNexus(nexus)) {
                        player.sendSystemMessage(
                                Component.literal("Nexus für /invasion-Befehle ausgewählt.")
                                        .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        player.sendSystemMessage(
                                Component.literal("Ein anderer Nexus ist bereits aktiv.")
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            });
        }

        // Vorhandenes Verhalten (GUI öffnen) beibehalten
        if (!stack.is(InvItems.MATERIAL_PROBE)
                && !stack.is(InvItems.NEXUS_ADJUSTER)
                && !stack.is(InvItems.DEBUG_WAND)) {

            MenuProvider factory = getMenuProvider(state, world, pos);
            if (factory != null) {
                player.openMenu(factory);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {

        if (!state.getValue(LIT)) {
            return;
        }

        for (int i = 0; i < 6; i++) {
            double y1 = pos.getY() + random.nextFloat();
            double y2 = (random.nextFloat() - 0.5D) * 0.5D;

            int direction = random.nextInt(2) * 2 - 1;
            double x2;
            double x1;
            double z1;
            double z2;
            if (random.nextInt(2) == 0) {
                z1 = pos.getZ() + 0.5D + 0.25D * direction;
                z2 = random.nextFloat() * 2.0F * direction;

                x1 = pos.getX() + random.nextFloat();
                x2 = (random.nextFloat() - 0.5D) * 0.5D;
            } else {
                x1 = pos.getX() + 0.5D + 0.25D * direction;
                x2 = random.nextFloat() * 2.0F * direction;
                z1 = pos.getZ() + random.nextFloat();
                z2 = (random.nextFloat() - 0.5D) * 0.5D;
            }

            world.addParticle(ParticleTypes.PORTAL, x1, y1, z1, x2, y2, z2);
        }
    }

    @Override
    public NexusBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NexusBlockEntity(pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.getBlockEntity(pos, InvBlockEntities.NEXUS).ifPresent(NexusBlockEntity::discard);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, InvBlockEntities.NEXUS, (w, pos, s, entity) -> entity.tick((ServerLevel)w, pos, s));
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        return state.getValue(LIT) ? -1 : super.getDestroyProgress(state, player, world, pos);
    }
}
