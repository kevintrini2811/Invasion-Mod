package com.invasion.item;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import com.invasion.entity.NexusEntity;
import com.invasion.nexus.WorldNexusStorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;

public final class InvasionSpawnEggItem extends SpawnEggItem {
    private final EntityType<? extends Mob> entityType;
    private final CompoundTag variantData;

    public InvasionSpawnEggItem(Properties properties, EntityType<? extends Mob> entityType,
            int primaryColor, int secondaryColor) {
        super(entityType, primaryColor, secondaryColor, properties);
        this.entityType = entityType;
        this.variantData = null;
    }

    public InvasionSpawnEggItem(Properties properties, EntityType<? extends Mob> entityType,
            int primaryColor, int secondaryColor, CompoundTag variantData) {
        super(entityType, primaryColor, secondaryColor, properties);
        this.entityType = entityType;
        this.variantData = variantData.copy();
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        if (variantData != null) {
            stack.getOrCreateTag().put("EntityTag", variantData.copy());
        }
        return stack;
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
