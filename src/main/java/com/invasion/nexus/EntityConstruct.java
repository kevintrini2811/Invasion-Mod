package com.invasion.nexus;

import com.invasion.entity.IMSilverfishEntity;
import com.invasion.entity.IMEndermiteEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record EntityConstruct (
        EntityType<? extends Mob> entityType,
        int texture,
        int tier,
        int flavour,
        float scaling,
        int minAngle,
        int maxAngle
    ) {

    public Mob createMob(NexusAccess nexus) {
        return createMob(nexus.getWorld(), nexus);
    }

    public Mob createMob(Level world, @Nullable NexusAccess nexus) {
        Mob entity = entityType().create(world);
        if (entity instanceof BuildableMob b) {
            b.onSpawned(nexus, this);
            applyWaveInfection(entity, nexus);
        }
        return entity;
    }

    public Mob createMob(ServerLevel world, @Nullable NexusAccess nexus, BlockPos position) {
        return entityType().create(world, entity -> {
            if (entity instanceof BuildableMob b) {
                b.onSpawned(nexus, this);
                applyWaveInfection(entity, nexus);
            }
        }, position, MobSpawnType.NATURAL, true, false);
    }

    public interface BuildableMob {
        void onSpawned(NexusAccess nexus, EntityConstruct spawnConditions);
    }

    private static void applyWaveInfection(
            Mob entity, @Nullable NexusAccess nexus) {
        if (nexus == null || entity instanceof IMSilverfishEntity
                || entity instanceof IMEndermiteEntity) {
            return;
        }
        int chancePercent = Math.clamp(nexus.getProgressionLevel() - 9, 0, 100);
        if (entity.getRandom().nextInt(100) < chancePercent) {
            entity.addTag(IMSilverfishEntity.INFECTED_TAG);
        }
    }
}
