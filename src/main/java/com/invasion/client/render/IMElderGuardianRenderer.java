package com.invasion.client.render;

import com.invasion.entity.IMElderGuardianEntity;

import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.world.phys.Vec3;

/** Extends the vanilla elder renderer with its block-targeted Nexus beam. */
public final class IMElderGuardianRenderer extends ElderGuardianRenderer {
    public IMElderGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(
            net.minecraft.world.entity.monster.Guardian guardian,
            GuardianRenderState state, float partialTick) {
        super.extractRenderState(guardian, state, partialTick);
        if (!(guardian instanceof IMElderGuardianEntity elder)) {
            return;
        }
        elder.getNexusBeamTarget().ifPresent(pos -> {
            float elapsed = elder.tickCount
                    - elder.getNexusBeamStartTick() + partialTick;
            state.attackTime = elapsed;
            state.attackScale = Math.min(
                    elapsed / elder.getAttackDuration(), 1.0F);
            state.attackTargetPosition = Vec3.atCenterOf(pos);
            state.lookDirection = elder.getViewVector(partialTick);
            state.lookAtPosition = state.attackTargetPosition;
        });
    }
}
