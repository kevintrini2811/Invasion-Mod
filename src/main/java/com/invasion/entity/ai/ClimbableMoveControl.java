package com.invasion.entity.ai;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

import com.invasion.entity.Animatable;

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
        Operation prevState = operation;

        super.tick();

        if (prevState == Operation.MOVE_TO && mob instanceof Animatable ae) {
            double dX = wantedX - mob.getX();
            double dY = wantedY - mob.getY();
            double dZ = wantedZ - mob.getZ();
            ae.setMoveState(dX * dX + dY * dY + dZ * dZ < 0.01D
                    ? MoveState.STANDING
                    : MoveState.RUNNING);
        }
    }

    protected Optional<Direction> getClimbFace(BlockPos pos) {
        return Optional.empty();
    }
}
