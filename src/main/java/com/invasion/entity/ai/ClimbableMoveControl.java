package com.invasion.entity.ai;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;


public class ClimbableMoveControl extends MoveControl {
    private float turnRate = 90;

    public ClimbableMoveControl(Mob entity) {
        super(entity);
    }

    public float getTurnRate() {
        return turnRate;
    }

    public void setTurnRate(float rate) {
        turnRate = rate;
    }

    @Override
    public void tick() {
        super.tick();
    }

    protected Optional<Direction> getClimbFace(BlockPos pos) {
        return Optional.empty();
    }
}
