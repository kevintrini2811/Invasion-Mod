package com.invasion.entity.ai;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;

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

        if (operation == MoveControl.Operation.STRAFE) {
            Optional<Direction> ladderPos = getClimbFace(mob.blockPosition()).or(() -> getClimbFace(mob.blockPosition().above()));
            if (ladderPos.isPresent()) {
                mob.getJumpControl().jump();
                operation = Operation.WAIT;
                return;
            }
        }

        super.tick();

        if (prevState == Operation.MOVE_TO) {
            double dX = wantedX - mob.getX();
            double dZ = wantedZ - mob.getZ();
            double dY = wantedY - mob.getY();

            Optional<Direction> ladderPos = Optional.empty();
            if (Math.abs(dX) < 0.8D && Math.abs(dZ) < 0.8D && (dY > 0 || mob.isSuppressingSlidingDownLadder())) {
                ladderPos = getClimbFace(mob.blockPosition()).or(() -> getClimbFace(mob.blockPosition().above()));
            }

            double dXZSq = dX * dX + dZ * dZ;
            double distanceSquared = dXZSq + dY * dY;
            if ((distanceSquared < 0.01D) && ladderPos.isEmpty()) {
                if (mob instanceof Animatable ae) {
                    ae.setMoveState(MoveState.STANDING);
                }
            } else if (ladderPos.isPresent()) {
                Vector3f orientation = ladderPos.get().step();
                float newYaw = (float) (Math.atan2(
                        dX - orientation.x,
                        dZ - orientation.z) * Mth.RAD_TO_DEG) - 90;
                mob.setYRot(rotlerp(mob.getYRot(), newYaw, getTurnRate()));

                // A purely vertical MOVE_TO has no forward input in modern
                // vanilla movement, so ladder physics otherwise lets the mob
                // slide back down. Push gently toward the supporting wall and
                // provide the upward movement represented by the path node.
                if (dY > 0.05D) {
                    var velocity = mob.getDeltaMovement();
                    mob.setDeltaMovement(
                            velocity.x * 0.5D - orientation.x * 0.08D,
                            Math.max(velocity.y, 0.2D),
                            velocity.z * 0.5D - orientation.z * 0.08D);
                }
                if (mob instanceof Animatable ae) {
                    ae.setMoveState(MoveState.CLIMBING);
                }
            } else if (mob instanceof Animatable ae) {
                ae.setMoveState(MoveState.RUNNING);
            }
        }
    }

    protected Optional<Direction> getClimbFace(BlockPos pos) {
        BlockState state = mob.level().getBlockState(pos);
        if (state.is(BlockTags.CLIMBABLE)) {
            return state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Optional.empty();
    }
}
