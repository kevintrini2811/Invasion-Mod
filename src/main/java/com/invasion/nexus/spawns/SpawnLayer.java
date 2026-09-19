package com.invasion.nexus.spawns;

import com.invasion.compat.ConfiguredModMobs;
import com.invasion.entity.Miner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.server.level.ServerLevel;

public enum SpawnLayer {
    BOTH("both"), UNDERGROUND("underground"), SURFACE("surface"), MINERS_ONLY("miners only");

    private final String value;

    SpawnLayer(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static SpawnLayer parse(String value) {
        for (SpawnLayer layer : values()) {
            if (layer.value.equalsIgnoreCase(value.strip())) return layer;
        }
        return BOTH;
    }

    // WORLD_SURFACE includes transparent blocks: glass and leaves are also roofs.
    static boolean isSurface(LevelReader world, BlockPos pos, float height) {
        return world.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ())
                <= pos.getY() + 0.5D + height;
    }

    boolean allows(ServerLevel world, BlockPos pos, Mob mob) {
        if (this == BOTH) return true;
        boolean surface = isSurface(world, pos, mob.getBbHeight());
        return switch (this) {
            case BOTH -> true;
            case SURFACE -> surface;
            case UNDERGROUND -> !surface;
            case MINERS_ONLY -> surface || world.getGameRules().get(GameRules.MOB_GRIEFING)
                    && ConfiguredModMobs.allowsMining(mob.getType(), mob instanceof Miner);
        };
    }
}
