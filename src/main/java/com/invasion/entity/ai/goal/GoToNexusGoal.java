package com.invasion.entity.ai.goal;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.invasion.entity.NexusEntity;
import com.invasion.entity.HasAiGoals;
import com.invasion.entity.pathfinding.Navigation;
import com.invasion.nexus.NexusAccess;

public class GoToNexusGoal extends Goal {
    private PathfinderMob mob;
    private final NexusEntity nexusEntity;
    private Optional<BlockPos> lastPathRequestPos = Optional.empty();
    private final Navigation navigation;
    private int pathRequestTimer;
    private int pathFailedCount;

    public <E extends PathfinderMob & NexusEntity> GoToNexusGoal(E entity) {
        this.mob = entity;
        this.nexusEntity = entity;
        this.navigation = entity.getNavigatorNew();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return nexusEntity.hasGoal(HasAiGoals.Goal.BREAK_NEXUS) && nexusEntity.hasNexus();
    }

    @Override
    public void start() {
        boolean pathSet = false;
        double distance = nexusEntity.findDistanceToNexus();

        if (--pathRequestTimer <= 0) {
            if (distance > 1.5) {
                NexusAccess nexus = nexusEntity.getNexus();
                BlockPos target = nexus.getOrigin();

                for (Direction i : Direction.Plane.HORIZONTAL) {
                    if (mob.level().getBlockState(nexus.getOrigin().relative(i)).isPathfindable(PathComputationType.LAND)) {
                        target = target.offset(i.getStepX(), 0, i.getStepZ());
                    }
                }

                @Nullable
                Path path = mob.getNavigation().createPath(target, 1);
                if (path != null) {
                    mob.setTarget(null);
                    mob.getNavigation().moveTo(path, distance > 2000 ? 2 : 1);
                    pathSet = true;
                }

            }

            if (!pathSet || (navigation.getLastPathDistanceToTarget() > 3 && lastPathRequestPos.isPresent() && mob.blockPosition().closerThan(lastPathRequestPos.get(), 3.5))) {
                pathFailedCount++;
                pathRequestTimer = 40 * pathFailedCount + mob.getRandom().nextInt(10);
            } else {
                pathFailedCount = 0;
                pathRequestTimer = 20;
            }


            lastPathRequestPos = Optional.of(mob.blockPosition());
        }
    }

    @Override
    public void tick() {
        if (pathFailedCount > 1) {
            @Nullable
            NexusAccess nexus = nexusEntity.getNexus();
            if (nexus != null) {
                Vec3 target = com.invasion.util.math.PosUtils.center(nexus.getOrigin());
                mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1);
                mob.setTarget(null);
            }
        }
        if (mob.getNavigation().isDone() || nexusEntity.getNavigatorNew().getStuckTime() > 40) {
            start();
        }
    }
}
