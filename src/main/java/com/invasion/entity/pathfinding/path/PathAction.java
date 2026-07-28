package com.invasion.entity.pathfinding.path;

import java.util.List;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public enum PathAction {
    NONE(Type.NONE, null),
    BRIDGE(Type.BRIDGE, null),
    SWIM(Type.SWIM, null),
    DIG(Type.DIG, null),
    CLIMB_UP(Type.CLIMB, Direction.UP),
    CLIMB_DOWN(Type.CLIMB, Direction.UP),
    LADDER_UP_PX(Type.LADDER, Direction.EAST),
    LADDER_UP_NX(Type.LADDER, Direction.WEST),
    LADDER_UP_PZ(Type.LADDER, Direction.SOUTH),
    LADDER_UP_NZ(Type.LADDER, Direction.NORTH),
    LADDER_TOWER_UP_PX(Type.TOWER, Direction.EAST),
    LADDER_TOWER_UP_NX(Type.TOWER, Direction.WEST),
    LADDER_TOWER_UP_PZ(Type.TOWER, Direction.SOUTH),
    LADDER_TOWER_UP_NZ(Type.TOWER, Direction.NORTH),
    SCAFFOLD_UP(Type.SCAFFOLD, Direction.UP);

    private static final List<PathAction> LADDER_ACTIONS = List.of(
            LADDER_UP_PZ, LADDER_UP_NX, LADDER_UP_NZ, LADDER_UP_PX
    );
    private static final List<PathAction> TOWER_ACTIONS = List.of(
            LADDER_TOWER_UP_PZ, LADDER_TOWER_UP_NX,
            LADDER_TOWER_UP_NZ, LADDER_TOWER_UP_PX
    );

    public static PathAction getLadderActionForDirection(Direction direction) {
        return LADDER_ACTIONS.get(direction.get2DDataValue());
    }

    public static PathAction getTowerActionForDirection(Direction direction) {
        return TOWER_ACTIONS.get(direction.get2DDataValue());
    }

    public static PathAction getClimbing(Direction direction) {
        return direction == Direction.UP ? CLIMB_UP : CLIMB_DOWN;
    }

    private final Type type;
    private final Direction direction;

    PathAction(Type type, Direction direction) {
        this.type = type;
        this.direction = direction;
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public Direction getOrientation() {
        return direction;
    }

    public boolean isHorizontal() {
        return direction != null && direction.getAxis().isHorizontal();
    }

    public enum Type {
        NONE,
        LADDER,
        BRIDGE,
        DIG,
        CLIMB,
        SWIM,
        TOWER,
        SCAFFOLD
    }
}
