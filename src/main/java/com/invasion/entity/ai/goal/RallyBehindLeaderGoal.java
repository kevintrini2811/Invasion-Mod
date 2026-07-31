package com.invasion.entity.ai.goal;

import com.invasion.entity.Leader;
import com.invasion.entity.NexusEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class RallyBehindLeaderGoal<T extends LivingEntity> extends FollowEntityGoal<T> {
    private static final float DEFAULT_FOLLOW_DISTANCE = 5;

    private int rallyCooldown;

    public <E extends PathfinderMob & NexusEntity> RallyBehindLeaderGoal(E entity, Class<T> leader) {
        this(entity, leader, DEFAULT_FOLLOW_DISTANCE);
    }

    public <E extends PathfinderMob & NexusEntity> RallyBehindLeaderGoal(E entity, Class<T> leader, float followDistance) {
        super(entity, leader, followDistance);
    }

    @Override
    public boolean canUse() {
        if (rallyCooldown > 0) {
            rallyCooldown--;
        }
        return rallyCooldown <= 0 && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return rallyCooldown <= 0 && super.canContinueToUse();
    }

    @Override
    public void tick() {
        super.tick();
        if (rallyCooldown > 0) {
            rallyCooldown--;
        }
        if (rallyCooldown <= 0 && getTarget() instanceof Leader leader && leader.isMartyr()) {
            rallyCooldown = 30;
        }
    }
}