package com.invasion.entity.animation;

import com.invasion.client.render.animation.AnimationAction;
import com.invasion.client.render.animation.AnimationController;
import com.invasion.client.render.animation.AnimationState;
import com.invasion.entity.VultureEntity;
import com.invasion.entity.ai.FlyState;
import com.invasion.entity.ai.MoveState;

public class WingController implements AnimationController {
    private final VultureEntity entity;
    private final AnimationState<?> state;

    private int timeAttacking;
    private float flapEffort = 1;
    private final float[] flapEffortSamples = { 1, 1, 1, 1, 1, 1 };
    private int sampleIndex;

    public WingController(VultureEntity entity, AnimationState<?> state) {
        this.entity = entity;
        this.state = state;
    }

    @Override
    public AnimationState<?> getState() {
        return state;
    }

    @Override
    public void update() {
        AnimationAction currAnimation = state.getCurrentAction();
        boolean wingAttack = entity.isAttackingWithWings();
        if (!wingAttack)
            timeAttacking = 0;
        else {
            timeAttacking++;
        }
        if (entity.tickCount % 5 == 0) {
            if (++sampleIndex >= flapEffortSamples.length) {
                sampleIndex = 0;
            }
            float sample = entity.getThrustEffort();
            flapEffort -= flapEffortSamples[sampleIndex] / flapEffortSamples.length;
            flapEffort += sample / flapEffortSamples.length;
            flapEffortSamples[sampleIndex] = sample;
        }

        if (entity.getFlyState() != FlyState.GROUNDED) {
            if (currAnimation == AnimationAction.WINGTUCK) {
                ensureAnimation(AnimationAction.WINGSPREAD, 2.2F, true);
            } else if (entity.isThrustOn()) {
                ensureAnimation(AnimationAction.WINGFLAP, 2 * flapEffort, false);
            } else {
                ensureAnimation(AnimationAction.WINGGLIDE, 0.7F, false);
            }
        } else {
            boolean wingsActive = false;
            if (entity.getMoveState() == MoveState.RUNNING) {
                if (currAnimation == AnimationAction.WINGTUCK) {
                    ensureAnimation(AnimationAction.WINGSPREAD, 2.2F, true);
                } else {
                    ensureAnimation(AnimationAction.WINGFLAP, 1, false);
                    if (!wingAttack && currAnimation == AnimationAction.WINGSPREAD && state.getCurrentAnimationPercent() >= 0.65F) {
                        state.setPaused(true);
                    }
                }
                wingsActive = true;
            }

            if (wingAttack) {
                ensureAnimation(AnimationAction.WINGFLAP, (float) (1D / Math.min(timeAttacking / 40 * 0.6D + 0.4D, 1)), false);
                wingsActive = true;
            }

            if (!wingsActive) {
                ensureAnimation(AnimationAction.WINGTUCK, 1.8F, true);
            }
        }
        state.update();
    }
}