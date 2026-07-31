package com.invasion.entity.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.Path;

@Deprecated
public class PathCreator implements PathSource {
    private final IMPathNodeNavigator pathFinder = new IMPathNodeNavigator();
    private final int[] nanosUsed = new int[6];

	private int searchDepth;
	private int quickFailDepth;

	private int index;

	public PathCreator() {
		this(200, 50);
	}

	public PathCreator(int searchDepth, int quickFailDepth) {
		this.searchDepth = searchDepth;
		this.quickFailDepth = quickFailDepth;
	}

	@Override
    public int getSearchDepth() {
		return searchDepth;
	}

	@Override
    public int getQuickFailDepth() {
		return quickFailDepth;
	}

	@Override
    public void setSearchDepth(int depth) {
		searchDepth = depth;
	}

	@Override
    public void setQuickFailDepth(int depth) {
		quickFailDepth = depth;
	}

	@Override
    public Path createPath(IMPathNodeMaker pather, BlockPos from, BlockPos to, float targetRadius, float maxSearchRange, BlockGetter terrainMap) {
		final long time = System.nanoTime();
		final Path path = pathFinder.createPath(pather, from, to, targetRadius, maxSearchRange, terrainMap, searchDepth, quickFailDepth);
		nanosUsed[index = (index + 1) % nanosUsed.length] = (int) (System.nanoTime() - time);
		return path;
	}
}