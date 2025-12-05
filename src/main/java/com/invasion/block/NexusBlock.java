package com.invasion.block;

import com.invasion.nexus.Nexus;
import com.invasion.nexus.WorldNexusStorage;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import com.invasion.item.InvItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class NexusBlock extends BlockWithEntity {
    private static final MapCodec<NexusBlock> CODEC = Block.createCodec(NexusBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.of("lit");

    public NexusBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LIT, false));
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world,
                                             BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

        // SERVER: Nexus für die Commands merken
        if (!world.isClient) {
            ServerWorld sw = (ServerWorld) world;

            world.getBlockEntity(pos, InvBlockEntities.NEXUS).ifPresent(be -> {
                NexusBlockEntity nexusBe = (NexusBlockEntity) be;

                // Über die BlockEntity den Nexus holen (erzwingt, dass er in WorldNexusStorage existiert)
                var access = nexusBe.getNexus();
                if (access instanceof Nexus nexus) {
                    WorldNexusStorage storage = WorldNexusStorage.of(sw);
                    if (storage.setActiveNexus(nexus)) {
                        player.sendMessage(
                                Text.literal("Nexus für /invasion-Befehle ausgewählt.")
                                        .formatted(Formatting.GREEN),
                                false
                        );
                    } else {
                        player.sendMessage(
                                Text.literal("Ein anderer Nexus ist bereits aktiv.")
                                        .formatted(Formatting.RED),
                                false
                        );
                    }
                }
            });
        }

        // Vorhandenes Verhalten (GUI öffnen) beibehalten
        if (!stack.isOf(InvItems.MATERIAL_PROBE)
                && !stack.isOf(InvItems.NEXUS_ADJUSTER)
                && !stack.getRegistryEntry().matchesKey(InvItems.DEBUG_WAND)) {

            NamedScreenHandlerFactory factory = createScreenHandlerFactory(state, world, pos);
            if (factory != null) {
                player.openHandledScreen(factory);
            }
            return ItemActionResult.SUCCESS;
        }

        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }


    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {

        if (!state.get(LIT)) {
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
    public NexusBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NexusBlockEntity(pos, state);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            world.getBlockEntity(pos, InvBlockEntities.NEXUS).ifPresent(NexusBlockEntity::discard);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, InvBlockEntities.NEXUS, (w, pos, s, entity) -> entity.tick((ServerWorld)w, pos, s));
    }

    @Override
    protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        return state.get(LIT) ? -1 : super.calcBlockBreakingDelta(state, player, world, pos);
    }
}