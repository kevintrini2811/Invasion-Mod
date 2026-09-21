package com.invasion.entity;

import java.util.function.Consumer;

import com.invasion.InvasionConfig;
import com.invasion.InvasionMod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class SpawnProxyEntity extends MobEntity {
    public SpawnProxyEntity(EntityType<SpawnProxyEntity> type, World world) {
        super(type, world);
    }

    @Override
    public void tick() {
        if (!getWorld().isClient) {
            generateMobGroup(getWorld(), entity -> {
                entity.updatePositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
                getWorld().spawnEntity(entity);
            });
        }
        discard();
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason reason) {
        return darkEnoughToSpawn(world)
                && getBlockPathWeight(world, getBlockPos()) >= 0
                && super.canSpawn(world, reason);
    }

    private boolean darkEnoughToSpawn(WorldAccess world) {
        BlockPos pos = getBlockPos();
        return world.getLightLevel(LightType.SKY, pos) <= getRandom().nextInt(32) && world.getLightLevel(pos) <= getRandom().nextInt(8);
    }

    public float getBlockPathWeight(WorldAccess world, BlockPos pos) {
        return 0.5F - getWorld().getLightLevel(pos);
    }

    public static void generateMobGroup(World world, Consumer<Entity> spawner) {
        InvasionConfig config = InvasionMod.getConfig();
        int numberOfMobs = world.getRandom().nextInt(config.nightMobMaxGroupSize) + 1;
        for (int i = 0; i < numberOfMobs; i++) {
            spawner.accept(config.getSpawnPool().selectNext(world.getRandom()).generateEntityConstruct(world.getRandom()).createMob(world, null));
        }
    }
}