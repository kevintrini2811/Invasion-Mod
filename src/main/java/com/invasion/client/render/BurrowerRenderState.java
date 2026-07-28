package com.invasion.client.render;

import com.invasion.entity.BurrowerEntity;
import com.invasion.util.math.PosRotate3D;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class BurrowerRenderState extends LivingEntityRenderState {
    public final PosRotate3D[] segments = new PosRotate3D[BurrowerEntity.NUMBER_OF_SEGMENTS + 1];
}
