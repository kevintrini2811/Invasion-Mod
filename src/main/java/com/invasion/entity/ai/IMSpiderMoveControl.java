package com.invasion.entity.ai;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import com.invasion.entity.NexusSpiderEntity;
import com.invasion.util.math.PosUtils;

public class IMSpiderMoveControl extends ClimbableMoveControl {
    private static final Direction[] DIRECTIONS = Direction.values();

    public IMSpiderMoveControl(NexusSpiderEntity entity) {
        super(entity);
    }

    @Override
    protected Optional<Direction> getClimbFace(BlockPos pos) {
        pos = BlockPos.containing(Vec3.atLowerCornerOf(pos).subtract(mob.getBbWidth() * 0.5F, 0, mob.getBbWidth() * 0.5F));

        int index = getMoveDirection();

        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (Vec3i offset : PosUtils.OFFSET_ADJACENT_2) {
            BlockState state = mob.level().getBlockState(mutable.set(pos).move(offset));
            if (state.isCollisionShapeFullBlock(mob.level(), mutable)) {
                return Optional.of(DIRECTIONS[(index % 8) / 2]);
            }
            index++;
        }
        return Optional.empty();
    }

    private int getMoveDirection() {
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            Node currentPoint = path.getNextNode();
            int pathLength = path.getNodeCount();
            for (int i = path.getNextNodeIndex(); i < pathLength; i++) {
                Node point = path.getNode(i);
                if (point.x > currentPoint.x) {
                    return 0;
                }
                if (point.x < currentPoint.x) {
                    return 2;
                }
                if (point.z > currentPoint.z) {
                    return 4;
                }
                if (point.z < currentPoint.z) {
                    return 6;
                }
            }
        }
        return 0;
    }

}