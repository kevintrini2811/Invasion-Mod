package com.invasion.nexus.spawns;

import com.invasion.InvasionMod;
import com.invasion.util.math.PolarAngle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelReader;

public record SpawnPoint(BlockPos pos, int angle, SpawnType type) implements PolarAngle, Comparable<PolarAngle> {
    @Override
    public int getAngle() {
        return this.angle;
    }

    public void applyTo(Entity entity) {
        entity.absSnapTo(pos().getX() + 0.5, pos().getY() + 0.5, pos().getZ() + 0.5, angle, 0);
    }

    public boolean isValidFor(LevelReader world, Mob entity) {
        if (world.isOutsideBuildHeight(pos)) {
            InvasionMod.LOGGER.debug("[Spawn] Spawn point was outside of build limit {}", pos);
            return false;
        }
        applyTo(entity);
        return entity.checkSpawnObstruction(world) && world.noCollision(entity);
    }

    public boolean trySpawnEntity(ServerLevel world, Mob entity) {
        if (isValidFor(world, entity)) {
            entity.finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.STRUCTURE, null);
            world.addFreshEntityWithPassengers(entity);
            return true;
        }
        return false;
    }

    public boolean columnEquals(SpawnPoint position) {
        return pos().getX() == position.pos().getX() && pos.getZ() == position.pos().getZ();
    }

    @Override
    public int compareTo(PolarAngle polarAngle) {
        if (angle < polarAngle.getAngle()) {
            return -1;
        }
        if (angle > polarAngle.getAngle()) {
            return 1;
        }

        return 0;
    }
}
