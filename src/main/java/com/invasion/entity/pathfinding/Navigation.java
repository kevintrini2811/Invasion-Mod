package com.invasion.entity.pathfinding;

import com.invasion.Notifiable;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.pathfinding.path.PathAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.NodeEvaluator;

public interface Navigation extends Notifiable, HasAiGoals {
    @Deprecated
    Actor<?> getActor();

    NodeEvaluator createNodeMaker();

    PathAction getCurrentWorkingAction();

    void autoPathToEntity(Entity target);

    boolean isWaitingForTask();

    Status getLastActionResult();

    boolean isIdle();

    int getStuckTime();

    float getLastPathDistanceToTarget();

    void haltForTick();

    Entity getTargetEntity();

    void setCanDestroyBlocks(boolean flag);

    void setCanDigDown(boolean flag);

    void setCanClimbLadders(boolean flag);
}