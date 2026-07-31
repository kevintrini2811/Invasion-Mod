package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

final class VanillaLoot {
    private VanillaLoot() {}

    static void drop(LivingEntity entity, EntityType<?> vanillaType,
            DamageSource source, Player lastHurtByPlayer) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        LootTable table = level.getServer().getLootData()
                .getLootTable(vanillaType.getDefaultLootTable());
        LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity());
        if (lastHurtByPlayer != null) {
            params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, lastHurtByPlayer)
                    .withLuck(lastHurtByPlayer.getLuck());
        }
        table.getRandomItems(params.create(LootContextParamSets.ENTITY),
                entity.getLootTableSeed(), entity::spawnAtLocation);
    }
}
