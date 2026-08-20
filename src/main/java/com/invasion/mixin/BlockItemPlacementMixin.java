package com.invasion.mixin;

import com.invasion.InvasionMod;
import com.invasion.nexus.WorldNexusStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts successful player block placements for the next Nexus theme bias. */
@Mixin(BlockItem.class)
abstract class BlockItemPlacementMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void invasion$recordPlayerPlacement(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (!callback.getReturnValue().consumesAction()
                || !(context.getPlayer() instanceof ServerPlayer player)
                || InvasionMod.SERVER == null) {
            return;
        }
        InvasionMod.SERVER.getAllLevels().forEach(level ->
                WorldNexusStorage.of(level).recordPlayerBlockPlacement());
    }
}
