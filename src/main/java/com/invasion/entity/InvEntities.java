package com.invasion.entity;

import java.lang.reflect.Field;

import com.invasion.InvasionConfig;
import com.invasion.InvasionMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.Vec3d;

public interface InvEntities {
    EntityType<IMSkeletonEntity> SKELETON = register("skeleton", EntityType.Builder.<IMSkeletonEntity>create(IMSkeletonEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.99F).eyeHeight(1.74F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<EntityIMZombie> ZOMBIE = register("zombie", EntityType.Builder.<EntityIMZombie>create(EntityIMZombie::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<EntityIMZombiePigman> ZOMBIE_PIGMAN = register("zombie_pigman", EntityType.Builder.<EntityIMZombiePigman>create(EntityIMZombiePigman::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<PigmanEngineerEntity> PIGMAN_ENGINEER = register("pigman_engineer", EntityType.Builder.<PigmanEngineerEntity>create(PigmanEngineerEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<ZombieBuilderEntity> ZOMBIE_BUILDER = register("zombie_builder", EntityType.Builder.<ZombieBuilderEntity>create(ZombieBuilderEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<IMCreeperEntity> CREEPER = register("creeper", EntityType.Builder.<IMCreeperEntity>create(IMCreeperEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.7F).maxTrackingRange(8));

    EntityType<NexusSpiderEntity> SPIDER = register("spider", EntityType.Builder.<NexusSpiderEntity>create(NexusSpiderEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).maxTrackingRange(8));
    EntityType<JumpingSpiderEntity> JUMPING_SPIDER = register("jumping_spider", EntityType.Builder.<JumpingSpiderEntity>create(JumpingSpiderEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).maxTrackingRange(8));
    EntityType<QueenSpiderEntity> QUEEN_SPIDER = register("queen_spider", EntityType.Builder.<QueenSpiderEntity>create(QueenSpiderEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).maxTrackingRange(8));

    EntityType<ThrowerEntity> THROWER = register("thrower", EntityType.Builder.<ThrowerEntity>create(ThrowerEntity::new, SpawnGroup.MONSTER)
            .dimensions(1.8F, 1.95F).eyeHeight(1.74F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<BurrowerEntity> BURROWER = register("burrower", EntityType.Builder.<BurrowerEntity>create(BurrowerEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.99F).eyeHeight(1.74F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<ImpEnitty> IMP = register("imp", EntityType.Builder.<ImpEnitty>create(ImpEnitty::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.99F).eyeHeight(1.74F).vehicleAttachment(-0.7F).maxTrackingRange(8));
    EntityType<IMWolfEntity> WOLF = register("wolf", EntityType.Builder.<IMWolfEntity>create(IMWolfEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6F, 0.85F).eyeHeight(0.68F).passengerAttachments(new Vec3d(0.0, 0.81875, -0.0625)).maxTrackingRange(10));

    EntityType<SpiderEggEntity> SPIDER_EGG = register("spider_egg", EntityType.Builder.<SpiderEggEntity>create(SpiderEggEntity::new, SpawnGroup.MISC)
            .dimensions(0.5F, 0.8F).eyeHeight(0.5F).maxTrackingRange(10));

    EntityType<TrapEntity> TRAP = register("trap", EntityType.Builder.<TrapEntity>create(TrapEntity::new, SpawnGroup.MISC)
            .dimensions(0.5F, 0.28F).makeFireImmune().maxTrackingRange(10).disableSummon());

    @Deprecated
    EntityType<SfxEntity> SFX = register("sfx", EntityType.Builder.<SfxEntity>create(SfxEntity::new, SpawnGroup.MISC)
            .dimensions(0.5F, 0.5F).maxTrackingRange(8).disableSummon().disableSaving());
    EntityType<SpawnProxyEntity> SPAWN_PROXY = register("spawn_proxy", EntityType.Builder.<SpawnProxyEntity>create(SpawnProxyEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.5F, 0.5F).maxTrackingRange(8).disableSummon().disableSaving());
    EntityType<ElectricityBoltEntity> BOLT = register("bolt", EntityType.Builder.<ElectricityBoltEntity>create(ElectricityBoltEntity::new, SpawnGroup.MISC)
            .dimensions(0.5F, 0.5F).maxTrackingRange(8).disableSummon().disableSaving());
    EntityType<BoulderEntity> BOULDER = register("boulder", EntityType.Builder.<BoulderEntity>create(BoulderEntity::new, SpawnGroup.MISC)
            .dimensions(0.5F, 0.5F).maxTrackingRange(8));
    EntityType<EntityIMPrimedTNT> TNT = register("tnt", EntityType.Builder.<EntityIMPrimedTNT>create(EntityIMPrimedTNT::new, SpawnGroup.MISC)
            .makeFireImmune().dimensions(0.98F, 0.98F).eyeHeight(0.15F).maxTrackingRange(10).trackingTickInterval(10));

    EntityType<VultureEntity> BIRD = register("bird", betaFeature(EntityType.Builder.<VultureEntity>create(VultureEntity::new, SpawnGroup.MONSTER)
            .dimensions(1, 1).maxTrackingRange(10).trackingTickInterval(10)));
    EntityType<EntityIMGiantBird> VULTURE = register("vulture", betaFeature(EntityType.Builder.<EntityIMGiantBird>create(EntityIMGiantBird::new, SpawnGroup.MONSTER)
            .attachment(EntityAttachmentType.VEHICLE, 0, -0.2F, 0)
            .dimensions(1.9F, 2.8F).maxTrackingRange(10).trackingTickInterval(10)));

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, InvasionMod.id(name), builder.build());
    }

    private static <T extends Entity> EntityType.Builder<T> betaFeature(EntityType.Builder<T> builder) {
        return InvasionMod.getConfig().debugMode ? builder : builder.disableSummon().disableSaving();
    }

    static void bootstrap() {
        FabricDefaultAttributeRegistry.register(SKELETON, IMSkeletonEntity.createIMSkeletonAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE, EntityIMZombie.createTierT1V0Attributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_PIGMAN, EntityIMZombiePigman.createT1Attributes());
        FabricDefaultAttributeRegistry.register(PIGMAN_ENGINEER, PigmanEngineerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_BUILDER, PigmanEngineerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CREEPER, IMCreeperEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER, NexusSpiderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(JUMPING_SPIDER, JumpingSpiderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(QUEEN_SPIDER, QueenSpiderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(THROWER, ThrowerEntity.createT1V0Attributes());
        FabricDefaultAttributeRegistry.register(BURROWER, BurrowerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(IMP, ImpEnitty.createAttributes());
        FabricDefaultAttributeRegistry.register(WOLF, IMWolfEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER_EGG, SpiderEggEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPAWN_PROXY, MobEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(BIRD, VultureEntity.createBirdAttributes());
        FabricDefaultAttributeRegistry.register(VULTURE, EntityIMGiantBird.createVultureAttributes());

        InvasionConfig config = InvasionMod.getConfig();

        if (config.nightSpawnsEnabled) {
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER), SPAWN_PROXY.getSpawnGroup(), SPAWN_PROXY, config.nightMobSpawnChance, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.ZOMBIE), ZOMBIE.getSpawnGroup(), ZOMBIE, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.SPIDER), SPIDER.getSpawnGroup(), SPIDER, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.SPIDER), SPIDER.getSpawnGroup(), JUMPING_SPIDER, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.SPIDER), SPIDER.getSpawnGroup(), QUEEN_SPIDER, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.SKELETON), SKELETON.getSpawnGroup(), SKELETON, 1, 1, 1);
        }

        if (config.maxNightMobs != 70) {
            try {
                // TODO: Use a mixin for this. Reflection is slow and won't work in an obfuscated environment
                Field field = SpawnGroup.class.getDeclaredField("capacity");
                field.setAccessible(true);
                field.set(SpawnGroup.MONSTER, config.maxNightMobs);
            } catch (Exception e) {
                InvasionMod.LOGGER.error("Error whilst updating max hostile entity cap", e);
            }
        }
    }
}
