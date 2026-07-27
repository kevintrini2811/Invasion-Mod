package com.invasion.item;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import com.invasion.entity.NexusEntity;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class InvasionSpawnEggItem extends SpawnEggItem {
    private final EntityType<? extends Mob> entityType;

    public InvasionSpawnEggItem(Properties properties, EntityType<? extends Mob> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return spawnAndBind(context.getLevel(), context.getPlayer(), () -> super.useOn(context));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return spawnAndBind(level, player, () -> super.use(level, player, hand));
    }

    private InteractionResult spawnAndBind(Level level, Player player, Supplier<InteractionResult> spawnAction) {
        if (!(level instanceof ServerLevel serverLevel) || player == null) {
            return spawnAction.get();
        }

        Set<Integer> existingEntities = new HashSet<>();
        serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16),
                mob -> mob.getType() == entityType).forEach(mob -> existingEntities.add(mob.getId()));

        InteractionResult result = spawnAction.get();

        WorldNexusStorage.of(serverLevel).getNexus()
                .filter(nexus -> nexus.getMode().isActive())
                .ifPresent(nexus -> serverLevel.getEntitiesOfClass(Mob.class,
                                player.getBoundingBox().inflate(16),
                                mob -> mob.getType() == entityType && !existingEntities.contains(mob.getId()))
                        .forEach(mob -> {
                            if (mob instanceof NexusEntity nexusMob) {
                                nexusMob.setNexus(nexus);
                                nexusMob.resetHealth();
                            }
                        }));
        return result;
    }
}
