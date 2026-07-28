package com.invasion.entity.pathfinding;

import com.invasion.entity.EntityIMLiving;
import com.invasion.entity.pathfinding.path.PathAction;
import com.invasion.util.math.PosRotate3D;

@Deprecated
public abstract class AbstractParametricNavigator extends IMNavigation {
    protected double minMoveToleranceSq = 21;
    protected int timeParam;

    public AbstractParametricNavigator(EntityIMLiving entity, PathSource pathSource) {
        super(entity, pathSource);
    }

    @Override
    public void tick() {
        if (theEntity.getTarget() != null) {
            transitionAIGoal(Goal.TARGET_ENTITY);
        } else if (theEntity.getNexus() != null) {
            transitionAIGoal(Goal.BREAK_NEXUS);
        } else {
            transitionAIGoal(Goal.CHILL);
        }

        totalTicks++;
        if (isIdle() || waitingForNotify) {
            return;
        }
        if (nodeActionFinished) {
            int pathIndex = path.getNextNodeIndex();
            pathFollow(timeParam + 1);
            doMovementTo(timeParam);

            if (path.getNextNodeIndex() != pathIndex) {
                ticksStuck = 0;
                if (getCurrentWorkingAction() != PathAction.NONE) {
                    nodeActionFinished = false;
                }
            }
        }
        if (nodeActionFinished) {
            if (!isPositionClear(activeNode.asBlockPos(), theEntity)) {
                if (theEntity.onPathBlocked(path, this)) {
                    setDoingTaskAndHold();
                } else {
                    stop();
                }
            }
        } else {
            handlePathAction(getCurrentWorkingAction());
        }
    }

    protected void doMovementTo(int time) {
        PosRotate3D movePos = entityPositionAtParam(time);
        theEntity.getMoveControl().setWantedPosition(movePos.position().x, movePos.position().y, movePos.position().z, 1);

        if (Math.abs(theEntity.distanceToSqr(movePos.position())) < minMoveToleranceSq) {
            timeParam = time;
            ticksStuck--;
        } else {
            ticksStuck++;
        }
    }

    protected abstract PosRotate3D entityPositionAtParam(int time);

    protected abstract boolean isReadyForNextNode(int time);

    protected void pathFollow(int time) {
        int nextIndex = path.getNextNodeIndex() + 1;
        if (isReadyForNextNode(time)) {
            if (nextIndex < path.getNodeCount()) {
                timeParam = 0;
                path.setNextNodeIndex(nextIndex);
                activeNode = path.getNode(path.getNextNodeIndex());
            }
        } else {
            timeParam = time;
        }
    }

    @Override
    protected void pathFollow() {
        pathFollow(0);
    }
}
