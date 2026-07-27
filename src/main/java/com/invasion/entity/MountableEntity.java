package com.invasion.entity;

import java.util.List;
import net.minecraft.util.Util;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

public interface MountableEntity extends NexusEntity {
    default void generateJockey(ServerLevelAccessor world, int currentWave, DifficultyInstance difficulty, EntitySpawnReason spawnReason) {
        PathfinderMob self = asEntity();
        int jockyAttempsts = currentWave - 10;
        while (--jockyAttempsts > 0) {
            RandomSource random = world.getRandom();
            if (random.nextInt(100) == 0) {
                Monster jockey = getJockeyType(world).create(self.level(), spawnReason);
                if (jockey != null) {
                    if (jockey instanceof NexusSpiderEntity) {
                        jockey.setBaby(true);
                    }
                    jockey.absSnapTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), 0.0F);
                    jockey.finalizeSpawn(world, difficulty, spawnReason, null);
                    if (jockey instanceof NexusEntity n) {
                        n.setNexus(getNexus());
                    }
                    AttributeUtil.applyNexusWaveComplications(jockey, world, currentWave / 2, difficulty, spawnReason);
                    jockey.startRiding(self);
                    break;
                }
            }
        }
    }

    default EntityType<? extends Monster> getJockeyType(ServerLevelAccessor world) {
        return Util.getRandom(List.of(
                InvEntities.SKELETON,
                InvEntities.ZOMBIE,
                InvEntities.ZOMBIE_PIGMAN,
                InvEntities.JUMPING_SPIDER,
                InvEntities.SPIDER
        ), world.getRandom());
    }
}
