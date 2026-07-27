package com.invasion.entity.pathfinding;

import com.invasion.entity.pathfinding.path.PathAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.Node;

@Deprecated
public interface IMPathNodeMaker {
    float getPathNodePenalty(Node startNode, Node endNode, BlockGetter world);

    void getSuccessors(BlockGetter world, Node node, PathBuilder pathBuilder);

    interface PathBuilder {
        void addNode(BlockPos pos, PathAction action);
    }
}