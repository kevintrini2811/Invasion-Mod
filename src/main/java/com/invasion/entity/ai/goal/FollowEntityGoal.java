package com.invasion.entity.ai.goal;

import com.invasion.entity.NexusEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class FollowEntityGoal<T extends LivingEntity> extends MoveToEntityGoal<T> {
    private final float followDistanceSq;

    @SuppressWarnings("unchecked")
    public <E extends PathfinderMob & NexusEntity> FollowEntityGoal(E entity, float followDistance) {
        this(entity, (Class<T>)LivingEntity.class, followDistance);
    }

    public <E extends PathfinderMob & NexusEntity> FollowEntityGoal(E entity, Class<? extends T> target, float followDistance) {
        super(entity, target);
        followDistanceSq = Mth.square(followDistance);
    }

    @Override
    public void start() {
        if (mob instanceof NexusEntity n) {
            n.onFollowingEntity(getTarget());
        }
        super.start();
    }

    @Override
    public void stop() {
        if (mob instanceof NexusEntity n) {
            n.onFollowingEntity(null);
        }
        super.stop();
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = getTarget();
        if (target != null && mob.distanceToSqr(target) < followDistanceSq) {
            navigation.haltForTick();
        }
    }
}