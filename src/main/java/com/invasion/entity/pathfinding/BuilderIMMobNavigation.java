package com.invasion.entity.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;

import com.invasion.entity.NexusEntity;
import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.entity.pathfinding.path.ActionablePathNode;
import com.invasion.entity.pathfinding.path.PathAction;

/**
 * Engineer navigation with only the terrain action that is currently
 * supported: bridging traversable gaps.
 */
public class BuilderIMMobNavigation extends IMMobNavigation {

    public <T extends Mob & NexusEntity> BuilderIMMobNavigation(T entity) {
        super(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (mob instanceof PigmanEngineerEntity engineer
                && !engineer.isBuildingTower()
                && engineer.hasNexus()
                && getAIGoal() == Goal.BREAK_NEXUS
                && isDone()
                && engineer.getNexus().getOrigin().getY()
                        - engineer.blockPosition().getY() >= 2) {
            engineer.tryStartTowerBuild();
        }
    }

    @Override
    public boolean moveTo(net.minecraft.world.level.pathfinder.Path path, double speed) {
        if (mob instanceof PigmanEngineerEntity engineer
                && engineer.isBuildingTower()) {
            return false;
        }
        return super.moveTo(path, speed);
    }

    public void resumeAfterTowerBuild() {
        if (!nexusEntityHasTarget()) {
            return;
        }
        BlockPos target = ((NexusEntity) mob).getNexus().getOrigin();
        moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1);
    }

    private boolean nexusEntityHasTarget() {
        return mob instanceof NexusEntity nexusMob && nexusMob.hasNexus();
    }

    @Override
    public NodeEvaluator createNodeMaker() {
        var nodeMaker = new NodeMaker();
        nodeMaker.setCanPassDoors(true);
        nodeMaker.setCanOpenDoors(true);
        nodeMaker.setCanFloat(true);
        nodeMaker.setCanClimbLadders(true);
        return nodeMaker;
    }

    class NodeMaker extends IMLandPathNodeMaker {

        @Override
        public float getDistancePenalty(
                Node previousNode, Node nextNode, CollisionGetter world) {
            PathAction action = ActionablePathNode.getAction(nextNode);
            if (action.getType() == PathAction.Type.BRIDGE) {
                return 1.7F;
            }
            return super.getDistancePenalty(previousNode, nextNode, world);
        }

        @Override
        protected Node findAcceptedNode(
                int x,
                int y,
                int z,
                int maxYStep,
                double feetY,
                Direction direction,
                PathType nodeType) {
            Node node = super.findAcceptedNode(
                    x, y, z, maxYStep, feetY, direction, nodeType);

            if (node != null
                    && direction.getAxis().isHorizontal()
                    && ActionablePathNode.getAction(node) == PathAction.NONE) {
                BlockPos.MutableBlockPos mutable =
                        new BlockPos.MutableBlockPos(x, y, z);
                if (isBridgableGap(mutable)) {
                    return getBridgeNode(x, y, z);
                }
            }

            return node;
        }

        private Node getBridgeNode(int x, int y, int z) {
            Node node = getNode(x, y, z);
            node.type = PathType.WALKABLE;
            node.costMalus = 1.5F;
            return ActionablePathNode.setAction(node, PathAction.BRIDGE);
        }

        private boolean isBridgableGap(BlockPos.MutableBlockPos mutable) {
            int originalY = mutable.getY();
            try {
                PathType type = getPathTypeStatic(
                        mob, mutable.setY(originalY - 1));
                return type == PathType.OPEN || type == PathType.WATER || type == PathType.LAVA;
            } finally {
                mutable.setY(originalY);
            }
        }
    }
}
