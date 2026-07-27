package com.invasion.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.invasion.Debug;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.PathfindingRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

@Mixin(DebugRenderer.class)
abstract class DebugRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void invasion_after_render(PoseStack matrices, MultiBufferSource.BufferSource vertexConsumers, double cameraX, double cameraY, double cameraZ, CallbackInfo info) {
        if (Debug.DEBUG_PATHFINDING) {
            Minecraft.getInstance().debugRenderer.pathfindingRenderer.render(matrices, vertexConsumers, cameraX, cameraY, cameraZ);
        }
    }
}

@Mixin(PathfindingRenderer.class)
abstract class PathfindingDebugRendererMixin {
    @Shadow
    private static float distanceToCamera(BlockPos pos, double x, double y, double z) {
        return (float)(Math.abs(pos.getX() - x) + Math.abs(pos.getY() - y) + Math.abs(pos.getZ() - z));
    }

    @Inject(method = "renderPath", at = @At("RETURN"))
    private static void invasion_drawPath(
            PoseStack matrices, MultiBufferSource vertexConsumers, Path path,
            float nodeSize, boolean drawDebugNodes, boolean drawLabels,
            double cameraX, double cameraY, double cameraZ,
            CallbackInfo info) {
        if (drawLabels) {
            for (int i = 0; i < path.getNodeCount(); ++i) {
                Node node = path.getNode(i);
                if (distanceToCamera(node.asBlockPos(), cameraX, cameraY, cameraZ) <= 80) {
                    DebugRenderer.renderFloatingText(matrices, vertexConsumers,
                            String.valueOf(ActionablePathNode.getAction(node)),
                            node.x + 0.5, node.y + 0.75 + 0.2, node.z + 0.5,
                            CommonColors.WHITE, 0.02F, true, 0, true
                    );
                }
            }
        }
    }
}