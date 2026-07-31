package com.invasion.entity;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import com.invasion.InvasionConfig;
import com.invasion.InvasionMod;

public class SpawnProxyEntity extends Mob {
    public SpawnProxyEntity(EntityType<SpawnProxyEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            generateMobGroup(level(), entity -> {
                entity.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                level().addFreshEntity(entity);
            });
        }
        discard();
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType reason) {
        return darkEnoughToSpawn(world)
                && getBlockPathWeight(world, blockPosition()) >= 0
                && super.checkSpawnRules(world, reason);
    }

    private boolean darkEnoughToSpawn(LevelAccessor world) {
        BlockPos pos = blockPosition();
        return world.getBrightness(LightLayer.SKY, pos) <= random.nextInt(32) && world.getMaxLocalRawBrightness(pos) <= random.nextInt(8);
    }

    public float getBlockPathWeight(LevelAccessor world, BlockPos pos) {
        return 0.5F - level().getMaxLocalRawBrightness(pos);
    }

    public static void generateMobGroup(Level world, Consumer<Entity> spawner) {
        InvasionConfig config = InvasionMod.getConfig();
        int numberOfMobs = world.getRandom().nextInt(config.nightMobMaxGroupSize) + 1;
        for (int i = 0; i < numberOfMobs; i++) {
            spawner.accept(config.getSpawnPool().selectNext(world.getRandom()).generateEntityConstruct(world.getRandom()).createMob(world, null));
        }
    }
}