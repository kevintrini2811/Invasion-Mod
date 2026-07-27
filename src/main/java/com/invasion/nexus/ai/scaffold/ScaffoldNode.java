package com.invasion.nexus.ai.scaffold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public record ScaffoldNode(
        BlockPos pos,
        Direction orientation,
        int height
    ) {
    public ScaffoldNode(CompoundTag compound) {
        this(
            compound.read("pos", BlockPos.CODEC).orElse(BlockPos.ZERO),
            Direction.from2DDataValue(compound.getIntOr("orientation", 0)),
            compound.getIntOr("height", 0)
        );
    }

    public void toNbt(CompoundTag compound) {
        compound.store("pos", BlockPos.CODEC, pos);
        compound.putInt("orientation", orientation.get2DDataValue());
        compound.putInt("height", height);
    }

    public int bottom() {
        return pos.getY();
    }

    public int top() {
        return bottom() + height();
    }

    public ScaffoldNode merge(ScaffoldNode newScaffold) {
        BlockPos newPos = newScaffold.pos();

        if (pos.getX() != newPos.getX() || pos.getZ() != newPos.getZ()) {
            return this;
        }

        int yChange = newScaffold.bottom() - bottom();

        if (yChange > 0 && yChange < height()) {
            return new ScaffoldNode(pos, orientation, yChange + newScaffold.height());
        }

        if (newScaffold.top() > bottom()) {
            return new ScaffoldNode(newPos, orientation, height() + newScaffold.height());
        }

        return this;
    }

    public boolean contains(BlockPos pos) {
        return pos().getX() == pos.getX() && pos().getZ() == pos.getZ()
                && bottom() <= pos.getY()
                && top() >= pos.getY();
    }
}
