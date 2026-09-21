package com.invasion.nexus;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record EntityConstruct (
        EntityType<? extends MobEntity> entityType,
        int texture,
        int tier,
        int flavour,
        float scaling,
        int minAngle,
        int maxAngle
    ) {

    public MobEntity createMob(NexusAccess nexus) {
        return createMob(nexus.getWorld(), nexus);
    }

    public MobEntity createMob(World world, @Nullable NexusAccess nexus) {
        MobEntity entity = entityType().create(world);
        if (entity instanceof BuildableMob b) {
            b.onSpawned(nexus, this);
        }
        return entity;
    }

    public MobEntity createMob(ServerWorld world, @Nullable NexusAccess nexus, BlockPos position) {
        return entityType().create(world, entity -> {
            if (entity instanceof BuildableMob b) {
                b.onSpawned(nexus, this);
            }
        }, position, SpawnReason.NATURAL, true, false);
    }

    public interface BuildableMob {
        void onSpawned(NexusAccess nexus, EntityConstruct spawnConditions);
    }
}