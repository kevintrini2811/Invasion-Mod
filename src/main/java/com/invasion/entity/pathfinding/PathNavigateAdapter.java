package com.invasion.entity.pathfinding;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

@Deprecated
public class PathNavigateAdapter extends PathNavigation {
    private final Navigation navigator;

    public PathNavigateAdapter(Mob entity, Level world, Navigation navigator) {
        super(entity, world);
        this.navigator = navigator;
    }

    public Navigation getNewNavigator() {
        return navigator;
    }

    @Override
    public void tick() {
        ((IMNavigation)navigator).tick();
    }

    @Override
    public void recomputePath() {

    }

    @Override
    public boolean isDone() {
        return navigator.isIdle();
    }

    @Override
    public void stop() {
        ((IMNavigation)navigator).stop();
    }

    @Override
    public void setSpeedModifier(double speed) {
        ((IMNavigation)navigator).setSpeed(speed);
    }

    @Override
    public boolean moveTo(double x, double y, double z, double movespeed) {
        return ((IMNavigation)navigator).startMovingTo(x, y, z, (float) movespeed);
    }

    @Override
    public boolean moveTo(Entity entity, double movespeed) {
        return ((IMNavigation)navigator).startMovingTo(entity, (float) movespeed);
    }

    @Override
    public NodeEvaluator getNodeEvaluator() {
        return null;
    }

    @Override
    public void setCanFloat(boolean canSwim) {
        navigator.getActor().setCanSwim(canSwim);
    }

    @Override
    public boolean canFloat() {
        return true;
    }

    @Override
    protected PathFinder createPathFinder(int range) {
        return null;
    }

    @Override
    protected Vec3 getTempMobPos() {
        return ((IMNavigation)navigator).getPos();
    }

    @Override
    protected boolean canUpdatePath() {
        return true;
    }

    @Override
    public boolean canNavigateGround() {
        return true;
    }
}
