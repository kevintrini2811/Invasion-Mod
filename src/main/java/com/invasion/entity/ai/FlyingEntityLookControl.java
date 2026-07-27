package com.invasion.entity.ai;

import java.util.Optional;
import net.minecraft.world.entity.ai.control.LookControl;
import com.invasion.entity.EntityIMFlying;

public class FlyingEntityLookControl extends LookControl {
    public FlyingEntityLookControl(EntityIMFlying entity) {
        super(entity);
    }

    @Override
    protected boolean resetXRotOnTick() {
        return false;
    }

    @Override
    protected Optional<Float> getXRotD() {
        return super.getXRotD().map(pitch -> pitch + 40);
    }

    @Override
    protected Optional<Float> getYRotD() {
        return super.getYRotD().map(yaw -> Math.abs(yaw) > 100 ? 0 : yaw / 6F);
    }
}