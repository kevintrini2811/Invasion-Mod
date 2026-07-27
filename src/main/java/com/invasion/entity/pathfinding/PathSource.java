package com.invasion.entity.pathfinding;

import com.invasion.entity.NexusEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.Path;

@Deprecated
public interface PathSource {

    int getSearchDepth();

    void setSearchDepth(int depth);

    int getQuickFailDepth();

    void setQuickFailDepth(int depth);

    Path createPath(IMPathNodeMaker pather, BlockPos from, BlockPos to, float targetRadius, float maxSearchRange, BlockGetter world);

    default Path createPath(NexusEntity entity, BlockPos pos, float targetRadius, float maxSearchRange, BlockGetter terrainMap) {
        return createPath(
                entity.getNavigatorNew().getActor(),
                getPathBegin(entity),
                pos.offset(Mth.floor(0.5F - entity.asEntity().getBbWidth() * 0.5F), 0, Mth.floor(0.5F - entity.asEntity().getBbWidth() * 0.5F)),
                targetRadius, maxSearchRange, terrainMap);
    }

    static BlockPos getPathBegin(NexusEntity entity) {
        if (entity.asEntity().getBbWidth() <= 1) {
            return entity.blockPosition();
        }

        return BlockPos.containing(entity.getBoundingBox().getMinPosition());
    }

    public enum PathPriority {
        LOW, MEDIUM, HIGH
    }
}