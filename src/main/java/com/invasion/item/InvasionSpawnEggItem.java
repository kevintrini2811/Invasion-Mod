package com.invasion.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.invasion.entity.NexusEntity;
import com.invasion.entity.TieredIMMobEntity;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

public final class InvasionSpawnEggItem extends SpawnEggItem {
    private final EntityType<? extends Mob> entityType;
    private final CustomData variantData;

    public InvasionSpawnEggItem(Properties properties, EntityType<? extends Mob> entityType,
            int primaryColor, int secondaryColor) {
        this(properties, entityType, primaryColor, secondaryColor, CustomData.EMPTY);
    }

    public InvasionSpawnEggItem(Properties properties, EntityType<? extends Mob> entityType,
            int primaryColor, int secondaryColor, CustomData variantData) {
        super(entityType, primaryColor, secondaryColor, properties);
        this.entityType = entityType;
        this.variantData = variantData;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return spawnAndBind(context.getLevel(), context.getPlayer(), () -> super.useOn(context));
    }

    private InteractionResult spawnAndBind(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player,
            Supplier<InteractionResult> spawnAction) {
        if (!(level instanceof ServerLevel serverLevel) || player == null) {
            return spawnAction.get();
        }

        Set<Integer> existingEntities = new HashSet<>();
        serverLevel.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16),
                mob -> mob.getType() == entityType).forEach(mob -> existingEntities.add(mob.getId()));

        InteractionResult result = spawnAction.get();

        List<Mob> spawnedEntities = serverLevel.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(16),
                mob -> mob.getType() == entityType && !existingEntities.contains(mob.getId()));
        spawnedEntities.forEach(this::applyEntityData);

        WorldNexusStorage.of(serverLevel).getNexus()
                .filter(nexus -> nexus.getMode().isActive())
                .ifPresent(nexus -> spawnedEntities.forEach(mob -> {
                            if (mob instanceof IHasNexus nexusMob) {
                                nexusMob.setNexus(nexus);
                                if (mob instanceof NexusEntity configuredMob) {
                                    configuredMob.resetHealth();
                                }
                            }
                        }));
        return result;
    }

    public boolean appliesTo(Mob mob) {
        return mob.getType() == entityType;
    }

    public void applyEntityData(Mob mob) {
        if (mob instanceof TieredIMMobEntity tieredMob) {
            var data = variantData.copyTag();
            if (data.contains("tier")) tieredMob.setTier(data.getInt("tier"));
            if (data.contains("flavour")) tieredMob.setFlavour(data.getInt("flavour"));
        }
    }
}
