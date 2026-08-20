package com.invasion.entity.ai;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.invasion.entity.NexusSpiderEntity;
import com.invasion.util.math.PosUtils;

public class IMSpiderMoveControl extends ClimbableMoveControl {
    private static final Direction[] CLIMB_FACES = {
            Direction.WEST,
            Direction.EAST,
            Direction.NORTH,
            Direction.SOUTH
    };

    public IMSpiderMoveControl(NexusSpiderEntity entity) {
        super(entity);
    }

    @Override
    public void tick() {
        Operation previousOperation = operation;
        double targetX = wantedX;
        double targetY = wantedY;
        double targetZ = wantedZ;

        super.tick();

        if (previousOperation != Operation.MOVE_TO) {
            return;
        }

        double deltaX = targetX - mob.getX();
        double deltaY = targetY - mob.getY();
        double deltaZ = targetZ - mob.getZ();
        if (Math.abs(deltaX) >= 0.8D
                || Math.abs(deltaZ) >= 0.8D
                || deltaY <= 0.05D) {
            return;
        }

        Optional<Direction> climbFace = getClimbFace(mob.blockPosition())
                .or(() -> getClimbFace(mob.blockPosition().above()));
        if (climbFace.isEmpty()) {
            return;
        }

        Direction face = climbFace.get();
        float targetYaw = (float)(Math.atan2(
                deltaX - face.getStepX(),
                deltaZ - face.getStepZ()) * Mth.RAD_TO_DEG) - 90.0F;
        mob.setYRot(rotlerp(mob.getYRot(), targetYaw, getTurnRate()));

        // A target directly above the spider gives vanilla MoveControl no
        // horizontal input. Pressing into the selected wall lets the spider's
        // normal climbing physics carry it to the vertical path node instead
        // of endlessly turning below it.
        Vec3 velocity = mob.getDeltaMovement();
        mob.setDeltaMovement(
                velocity.x * 0.5D - face.getStepX() * 0.08D,
                Math.max(velocity.y, 0.2D),
                velocity.z * 0.5D - face.getStepZ() * 0.08D);
    }

    @Override
    protected Optional<Direction> getClimbFace(BlockPos pos) {
        pos = BlockPos.containing(Vec3.atLowerCornerOf(pos).subtract(mob.getBbWidth() * 0.5F, 0, mob.getBbWidth() * 0.5F));

        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int index = 0; index < PosUtils.OFFSET_ADJACENT_2.size(); index++) {
            Vec3i offset = PosUtils.OFFSET_ADJACENT_2.get(index);
            BlockState state = mob.level().getBlockState(mutable.set(pos).move(offset));
            if (state.isCollisionShapeFullBlock(mob.level(), mutable)) {
                return Optional.of(CLIMB_FACES[index / 2]);
            }
        }
        return Optional.empty();
    }

}
