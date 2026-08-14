package com.invasion.compat;

import java.util.List;

import com.invasion.InvasionMod;
import com.invasion.entity.mutant.IMMutantCreeperEntity;
import com.invasion.entity.mutant.IMMutantEndermanEntity;
import com.invasion.entity.mutant.IMMutantSkeletonEntity;
import com.invasion.entity.mutant.IMMutantZombieEntity;
import com.invasion.entity.mutant.IMSpiderPigEntity;

import fuzs.mutantmonsters.common.world.entity.mutant.MutantCreeper;
import fuzs.mutantmonsters.common.world.entity.mutant.MutantEnderman;
import fuzs.mutantmonsters.common.world.entity.mutant.MutantSkeleton;
import fuzs.mutantmonsters.common.world.entity.mutant.MutantZombie;
import fuzs.mutantmonsters.common.world.entity.mutant.SpiderPig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Entity registrations that are loaded only when Mutant Monsters is present. */
public final class MutantMonstersEntities {
    public static final EntityType<IMMutantZombieEntity> MUTANT_ZOMBIE = register(
            "mutant_zombie", EntityType.Builder.of(
                    IMMutantZombieEntity::new, MobCategory.MONSTER)
                    .sized(1.8F, 3.2F).eyeHeight(2.8F).notInPeaceful());
    public static final EntityType<IMMutantCreeperEntity> MUTANT_CREEPER = register(
            "mutant_creeper", EntityType.Builder.of(
                    IMMutantCreeperEntity::new, MobCategory.MONSTER)
                    .sized(1.99F, 2.8F).eyeHeight(2.6F).notInPeaceful());
    public static final EntityType<IMMutantSkeletonEntity> MUTANT_SKELETON = register(
            "mutant_skeleton", EntityType.Builder.of(
                    IMMutantSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.6F).eyeHeight(3.25F).notInPeaceful());
    public static final EntityType<IMMutantEndermanEntity> MUTANT_ENDERMAN = register(
            "mutant_enderman", EntityType.Builder.of(
                    IMMutantEndermanEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 4.2F).eyeHeight(3.9F).notInPeaceful());
    public static final EntityType<IMSpiderPigEntity> SPIDER_PIG = register(
            "spider_pig", EntityType.Builder.of(
                    IMSpiderPigEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F).eyeHeight(0.675F));

    private MutantMonstersEntities() {
    }

    private static boolean bootstrapped;

    public static List<EntityType<? extends Mob>> mobTypes() {
        return List.of(MUTANT_ZOMBIE, MUTANT_CREEPER, MUTANT_SKELETON,
                MUTANT_ENDERMAN, SPIDER_PIG);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MUTANT_ZOMBIE, MutantZombie.createAttributes().build());
        event.put(MUTANT_CREEPER, MutantCreeper.createAttributes().build());
        event.put(MUTANT_SKELETON, MutantSkeleton.createAttributes().build());
        event.put(MUTANT_ENDERMAN, MutantEnderman.createAttributes().build());
        event.put(SPIDER_PIG, SpiderPig.createAttributes().build());
    }

    public static void bootstrap() {
        if (!bootstrapped) {
            bootstrapped = true;
            NeoForge.EVENT_BUS.addListener(MutantMonstersEntities::addOriginalDrops);
        }
    }

    private static void addOriginalDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(
                event.getEntity().getType());
        String name = entityId.getPath();
        if (!entityId.getNamespace().equals("invmod")
                || !MutantMonstersCompatibility.MOB_NAMES.contains(name)) {
            return;
        }
        ResourceKey<LootTable> lootTable = ResourceKey.create(
                Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath("mutantmonsters", "entities/" + name));
        event.getEntity().dropFromLootTable(
                level, event.getSource(), event.isRecentlyHit(), lootTable,
                stack -> event.getDrops().add(new ItemEntity(
                        level, event.getEntity().getX(), event.getEntity().getY(),
                        event.getEntity().getZ(), stack)));
    }

    private static <T extends Entity> EntityType<T> register(
            String name, EntityType.Builder<T> builder) {
        var id = InvasionMod.id(name);
        var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
    }
}
