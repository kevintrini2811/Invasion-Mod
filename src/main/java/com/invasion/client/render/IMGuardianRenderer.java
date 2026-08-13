package com.invasion.client.render;

import com.invasion.entity.IMGuardianEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.world.phys.Vec3;

/** Extends the vanilla renderer with the Guardian's block-targeted Nexus beam. */
public final class IMGuardianRenderer extends GuardianRenderer {
    public IMGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(
            net.minecraft.world.entity.monster.Guardian guardian,
            GuardianRenderState state, float partialTick) {
        super.extractRenderState(guardian, state, partialTick);
        if (!(guardian instanceof IMGuardianEntity imGuardian)) {
            return;
        }
        imGuardian.getNexusBeamTarget().ifPresent(pos -> {
            float elapsed = imGuardian.tickCount
                    - imGuardian.getNexusBeamStartTick() + partialTick;
            state.attackTime = elapsed;
            state.attackScale = Math.min(
                    elapsed / imGuardian.getAttackDuration(), 1.0F);
            state.attackTargetPosition = Vec3.atCenterOf(pos);
            state.lookDirection = imGuardian.getViewVector(partialTick);
            state.lookAtPosition = state.attackTargetPosition;
        });
    }
}
