package com.invasion.client.render;

import com.invasion.entity.TrapEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class TrapRenderState extends EntityRenderState {
    public TrapEntity.Type trapType = TrapEntity.Type.EMPTY;
}
