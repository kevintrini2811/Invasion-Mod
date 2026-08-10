package com.invasion.client.render;

import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.block.BlockModelRenderState;

public final class IMWitherSkeletonRenderState extends SkeletonRenderState {
    public boolean groupLeaderWaiting;
    public boolean dancing;
    public boolean carryingSkull;
    public final BlockModelRenderState skullModel = new BlockModelRenderState();
}
