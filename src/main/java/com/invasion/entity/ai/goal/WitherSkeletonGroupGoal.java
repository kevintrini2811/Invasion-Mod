package com.invasion.entity.ai.goal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.invasion.entity.IMWitherEntity;
import com.invasion.entity.IMWitherSkeletonEntity;
import com.invasion.entity.InvEntities;
import com.invasion.nexus.NexusAccess;
import com.invasion.util.math.PosUtils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Coordinates IM Wither Skeleton groups and merges complete groups of four. */
public final class WitherSkeletonGroupGoal extends Goal {
    private static final double MERGE_DISTANCE_SQUARED = 3.5D * 3.5D;
    private static final int REQUIRED_MEMBERS = 4;

    private final IMWitherSkeletonEntity skeleton;
    private List<IMWitherSkeletonEntity> group = List.of();
    private IMWitherSkeletonEntity leader;

    public WitherSkeletonGroupGoal(IMWitherSkeletonEntity skeleton) {
        this.skeleton = skeleton;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        refreshGroup();
        return group.size() >= 2;
    }

    @Override
    public boolean canContinueToUse() {
        refreshGroup();
        return group.size() >= 2;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        skeleton.getNavigatorNew().autoPathToEntity(null);
        skeleton.setGroupLeaderWaiting(false);
    }

    @Override
    public void tick() {
        refreshGroup();
        if (group.size() < 2) {
            return;
        }

        if (leader != skeleton) {
            skeleton.setGroupLeaderWaiting(false);
            // Let IMMobNavigation follow the moving leader. Unlike replacing
            // the path here every tick, this preserves an active ladder climb
            // and repaths once the skeleton has safely reached the top.
            skeleton.getNavigatorNew().autoPathToEntity(leader);
            return;
        }

        skeleton.getNavigatorNew().autoPathToEntity(null);
        if (group.size() < REQUIRED_MEMBERS) {
            // No fourth member is currently expected. Keep the incomplete
            // group together, but let its leader continue towards the Nexus.
            skeleton.setGroupLeaderWaiting(false);
            var target = PosUtils.center(skeleton.getNexus().getOrigin());
            skeleton.getNavigation().moveTo(
                    target.x, target.y, target.z, 1.0D);
            return;
        }

        skeleton.getNavigation().stop();
        skeleton.setGroupLeaderWaiting(true);
        List<IMWitherSkeletonEntity> mergeMembers = group.stream()
                .sorted(Comparator.comparingDouble(skeleton::distanceToSqr))
                .limit(REQUIRED_MEMBERS)
                .toList();
        if (mergeMembers.size() == REQUIRED_MEMBERS
                && mergeMembers.stream().allMatch(member ->
                        member.distanceToSqr(skeleton)
                                <= MERGE_DISTANCE_SQUARED)) {
            merge(mergeMembers);
        }
    }

    private void refreshGroup() {
        if (!(skeleton.level() instanceof ServerLevel level)
                || !skeleton.hasNexus()
                || skeleton.getNexus().isDiscarded()
                || !skeleton.getNexus().isActive()) {
            group = List.of();
            leader = null;
            return;
        }
        GroupSnapshot snapshot = GroupCoordinator.get(
                level, skeleton.getNexus().getUuid());
        group = snapshot.members();
        leader = snapshot.leader();
    }

    private void merge(List<IMWitherSkeletonEntity> members) {
        if (!(skeleton.level() instanceof ServerLevel level)) {
            return;
        }
        NexusAccess nexus = skeleton.getNexus();
        double health = members.stream()
                .mapToDouble(IMWitherSkeletonEntity::getHealth)
                .sum() * 2.0D;
        double x = members.stream().mapToDouble(Entity::getX).average().orElse(getX());
        double y = members.stream().mapToDouble(Entity::getY).average().orElse(getY());
        double z = members.stream().mapToDouble(Entity::getZ).average().orElse(getZ());

        IMWitherEntity wither = InvEntities.WITHER.create(level);
        if (wither == null) {
            return;
        }
        wither.moveTo(x, y, z, skeleton.getYRot(), 0.0F);
        wither.setNexus(nexus);
        wither.setMergedHealth(health);
        if (!level.addFreshEntity(wither)) {
            return;
        }

        // Three skeleton slots become completed; the fourth is represented by
        // the new Wither so the wave's remaining-mob count stays consistent.
        for (int i = 0; i < members.size(); i++) {
            IMWitherSkeletonEntity member = members.get(i);
            if (i < members.size() - 1) {
                nexus.notifyCombatantRemoved(member, Entity.RemovalReason.KILLED);
            }
            member.setNexus(null);
            member.discard();
        }
    }

    private double getX() {
        return skeleton.getX();
    }

    private double getY() {
        return skeleton.getY();
    }

    private double getZ() {
        return skeleton.getZ();
    }

    private record GroupSnapshot(
            List<IMWitherSkeletonEntity> members,
            IMWitherSkeletonEntity leader) {
        private static final GroupSnapshot EMPTY =
                new GroupSnapshot(List.of(), null);
    }

    private record LevelSnapshot(
            long gameTime, Map<UUID, GroupSnapshot> groups) {
    }

    /** Builds one shared group index per level tick instead of per skeleton. */
    private static final class GroupCoordinator {
        private static final Map<ServerLevel, LevelSnapshot> LEVELS =
                new WeakHashMap<>();

        private GroupCoordinator() {
        }

        private static synchronized GroupSnapshot get(
                ServerLevel level, UUID nexusId) {
            long gameTime = level.getGameTime();
            LevelSnapshot snapshot = LEVELS.get(level);
            if (snapshot == null || snapshot.gameTime() != gameTime) {
                snapshot = build(level, gameTime);
                LEVELS.put(level, snapshot);
            }
            return snapshot.groups().getOrDefault(
                    nexusId, GroupSnapshot.EMPTY);
        }

        private static LevelSnapshot build(ServerLevel level, long gameTime) {
            Map<UUID, List<IMWitherSkeletonEntity>> members = new HashMap<>();
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof IMWitherSkeletonEntity candidate)
                        || !candidate.isAlive()
                        || candidate.isRemoved()
                        || !candidate.hasNexus()) {
                    continue;
                }
                NexusAccess nexus = candidate.getNexus();
                if (nexus == null || nexus.isDiscarded() || !nexus.isActive()) {
                    continue;
                }
                members.computeIfAbsent(nexus.getUuid(), ignored ->
                        new ArrayList<>()).add(candidate);
            }

            Map<UUID, GroupSnapshot> groups = new HashMap<>();
            members.forEach((nexusId, candidates) -> {
                List<IMWitherSkeletonEntity> sharedMembers =
                        List.copyOf(candidates);
                IMWitherSkeletonEntity sharedLeader = sharedMembers.stream()
                        .min(Comparator.comparing(Entity::getUUID))
                        .orElse(null);
                groups.put(nexusId,
                        new GroupSnapshot(sharedMembers, sharedLeader));
            });
            return new LevelSnapshot(gameTime, Map.copyOf(groups));
        }
    }
}
