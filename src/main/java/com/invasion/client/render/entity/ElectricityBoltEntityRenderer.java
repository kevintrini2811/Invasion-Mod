package com.invasion.client.render.entity;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.invasion.entity.ElectricityBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class ElectricityBoltEntityRenderer extends NoopRenderer<ElectricityBoltEntity> {
    public ElectricityBoltEntityRenderer(Context context) {
        super(context);
    }

    @Override
    public void render(ElectricityBoltEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        Vector3f[] vertices = entity.getVertices();
        if (vertices != null) {
            matrices.pushPose();
            setupTransform(entity, matrices, tickDelta);
            renderBranches(matrices, vertices, vertexConsumers.getBuffer(RenderType.lightning()));
            matrices.popPose();
        }
    }

    private void setupTransform(ElectricityBoltEntity entity, PoseStack matrices, float tickDelta) {
        matrices.mulPose(Axis.YP.rotationDegrees(entity.getViewYRot(tickDelta)));
        matrices.mulPose(Axis.ZP.rotationDegrees(entity.getViewXRot(tickDelta)));
        matrices.scale(0.0625F, 0.0625F, 0.0625F);
    }

    private void renderBranches(PoseStack matrices, Vector3f[] vertices, VertexConsumer buffer) {
        Matrix4f matrix = matrices.last().pose();
        float drawWidth = -0.1F;
        for (int pass = 0; pass < 4; pass++) {
            drawWidth += 0.32F;
            for (int i = 1; i < vertices.length; i++) {
                for (int j = 0; j < 5; j++) {
                    float xOffset = 0.5F - drawWidth;
                    float zOffset = 0.5F - drawWidth;
                    if (j == 1 || j == 2) {
                        xOffset += drawWidth * 2;
                    }
                    if (j == 2 || j == 3) {
                        zOffset += drawWidth * 2;
                    }
                    drawBranchSegment(matrix, buffer, vertices[i - 1], vertices[i], 0.5F, 0.5F, 0.6F, xOffset, zOffset);
                }
            }
        }
    }

    private static void drawBranchSegment(Matrix4f matrix, VertexConsumer buffer, Vector3f from, Vector3f to, float red, float green, float blue, float xOffset, float zOffset) {
        buffer.addVertex(matrix, from.x + xOffset, from.y * 16, from.z + zOffset).setColor(red, green, blue, 0.6F);
        buffer.addVertex(matrix, to.x + xOffset, to.y * 16, to.z + zOffset).setColor(red, green, blue, 0.6F);
    }
}