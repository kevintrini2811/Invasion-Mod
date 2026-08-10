package com.invasion.entity;

import java.lang.reflect.Field;

import com.invasion.InvasionConfig;
import com.invasion.InvasionMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.phys.Vec3;

public interface InvEntities {
    EntityType<IMSkeletonEntity> SKELETON = register("skeleton", EntityType.Builder.<IMSkeletonEntity>of(IMSkeletonEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMBoggedEntity> BOGGED = register(
            "bogged",
            EntityType.Builder.<IMBoggedEntity>of(
                            IMBoggedEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F)
                    .ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMParchedEntity> PARCHED = register(
            "parched",
            EntityType.Builder.<IMParchedEntity>of(
                            IMParchedEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F)
                    .ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMStrayEntity> STRAY = register(
            "stray",
            EntityType.Builder.<IMStrayEntity>of(
                            IMStrayEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F)
                    .ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMWitherSkeletonEntity> WITHER_SKELETON = register(
            "wither_skeleton",
            EntityType.Builder.<IMWitherSkeletonEntity>of(
                            IMWitherSkeletonEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(0.7F, 2.4F).eyeHeight(2.1F)
                    .ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMWitchEntity> WITCH = register("witch",
            EntityType.Builder.<IMWitchEntity>of(
                            IMWitchEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F)
                    .ridingOffset(-0.45F).clientTrackingRange(8));
    EntityType<IMGhastEntity> GHAST = register("ghast",
            EntityType.Builder.<IMGhastEntity>of(
                            IMGhastEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(4.0F, 4.0F).eyeHeight(2.6F)
                    .passengerAttachments(4.0625F).clientTrackingRange(10));
    EntityType<EntityIMZombie> ZOMBIE = register("zombie", EntityType.Builder.<EntityIMZombie>of(EntityIMZombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.8F).eyeHeight(1.53F).passengerAttachments(1.865F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<EntityIMSpeedyZombie> SPEEDY_ZOMBIE = register(
            "speedy_zombie",
            EntityType.Builder.<EntityIMSpeedyZombie>of(
                            EntityIMSpeedyZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.53F)
                    .passengerAttachments(1.865F).ridingOffset(-0.7F)
                    .clientTrackingRange(8));
    EntityType<IMHuskEntity> HUSK = register(
            "husk",
            EntityType.Builder.<IMHuskEntity>of(
                            IMHuskEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .passengerAttachments(2.0125F).ridingOffset(-0.7F)
                    .clientTrackingRange(8));
    EntityType<IMDrownedEntity> DROWNED = register(
            "drowned",
            EntityType.Builder.<IMDrownedEntity>of(
                            IMDrownedEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .passengerAttachments(2.0125F).ridingOffset(-0.7F)
                    .clientTrackingRange(8));
    EntityType<IMZombieVillagerEntity> ZOMBIE_VILLAGER = register(
            "zombie_villager",
            EntityType.Builder.<IMZombieVillagerEntity>of(
                            IMZombieVillagerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).eyeHeight(1.53F)
                    .passengerAttachments(1.865F).ridingOffset(-0.7F)
                    .clientTrackingRange(8));
    EntityType<EntityIMZombiePigman> ZOMBIE_PIGMAN = register("zombie_pigman", EntityType.Builder.<EntityIMZombiePigman>of(EntityIMZombiePigman::new, MobCategory.MONSTER)
            .sized(0.6F, 1.8F).eyeHeight(1.53F).passengerAttachments(1.865F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMZoglinEntity> ZOGLIN = register("zoglin",
            EntityType.Builder.<IMZoglinEntity>of(IMZoglinEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(1.3964844F, 1.4F)
                    .passengerAttachments(1.49375F).clientTrackingRange(8));
    EntityType<IMWitherEntity> WITHER = register("wither",
            EntityType.Builder.<IMWitherEntity>of(
                            IMWitherEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(0.9F, 3.5F)
                    .clientTrackingRange(10));
    EntityType<IMWardenEntity> WARDEN = register("warden",
            EntityType.Builder.<IMWardenEntity>of(
                            IMWardenEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(0.9F, 2.9F).eyeHeight(2.55F)
                    .clientTrackingRange(16));
    EntityType<IMWitherSkullEntity> WITHER_SKULL = register(
            "wither_skull",
            EntityType.Builder.<IMWitherSkullEntity>of(
                            IMWitherSkullEntity::new, MobCategory.MISC)
                    .sized(0.3125F, 0.3125F).clientTrackingRange(4)
                    .updateInterval(10));
    EntityType<IMWitchPotionEntity> WITCH_POTION = register(
            "witch_potion",
            EntityType.Builder.<IMWitchPotionEntity>of(
                            IMWitchPotionEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4)
                    .updateInterval(10));
    EntityType<IMZombifiedPiglinEntity> ZOMBIFIED_PIGLIN = register(
            "zombified_piglin",
            EntityType.Builder.<IMZombifiedPiglinEntity>of(
                            IMZombifiedPiglinEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(0.6F, 1.95F).eyeHeight(1.79F)
                    .passengerAttachments(2.0125F).ridingOffset(-0.7F)
                    .clientTrackingRange(8));
    EntityType<PigmanEngineerEntity> PIGMAN_ENGINEER = register("pigman_engineer", EntityType.Builder.<PigmanEngineerEntity>of(PigmanEngineerEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<ZombieBuilderEntity> ZOMBIE_BUILDER = register("zombie_builder", EntityType.Builder.<ZombieBuilderEntity>of(ZombieBuilderEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMCreeperEntity> CREEPER = register("creeper", EntityType.Builder.<IMCreeperEntity>of(IMCreeperEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.7F).clientTrackingRange(8));

    EntityType<NexusSpiderEntity> SPIDER = register("spider", EntityType.Builder.<NexusSpiderEntity>of(NexusSpiderEntity::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).clientTrackingRange(8));
    EntityType<JumpingSpiderEntity> JUMPING_SPIDER = register("jumping_spider", EntityType.Builder.<JumpingSpiderEntity>of(JumpingSpiderEntity::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).clientTrackingRange(8));
    EntityType<IMCaveSpiderEntity> CAVE_SPIDER = register("cave_spider", EntityType.Builder.<IMCaveSpiderEntity>of(IMCaveSpiderEntity::new, MobCategory.MONSTER)
            .sized(0.7F, 0.5F).eyeHeight(0.45F).clientTrackingRange(8));
    EntityType<QueenSpiderEntity> QUEEN_SPIDER = register("queen_spider", EntityType.Builder.<QueenSpiderEntity>of(QueenSpiderEntity::new, MobCategory.MONSTER)
            .sized(2.8F, 1.8F).eyeHeight(0.65F).passengerAttachments(0.765F).clientTrackingRange(8));

    EntityType<ThrowerEntity> THROWER = register("thrower", EntityType.Builder.<ThrowerEntity>of(ThrowerEntity::new, MobCategory.MONSTER)
            .sized(1.8F, 1.95F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<BurrowerEntity> BURROWER = register("burrower", EntityType.Builder.<BurrowerEntity>of(BurrowerEntity::new, MobCategory.MONSTER)
            .sized(0.5F, 0.5F).eyeHeight(0.25F).clientTrackingRange(10));
    EntityType<BurrowerTailEntity> BURROWER_TAIL = register("burrower_tail",
            EntityType.Builder.<BurrowerTailEntity>of(BurrowerTailEntity::new, MobCategory.MISC)
                    .sized(0.7F, 0.7F).clientTrackingRange(10).updateInterval(2).noSummon().noSave());
    EntityType<ImpEnitty> IMP = register("imp", EntityType.Builder.<ImpEnitty>of(ImpEnitty::new, MobCategory.MONSTER)
            .sized(0.6F, 1.8F).eyeHeight(1.53F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMBlazeEntity> BLAZE = register("blaze",
            EntityType.Builder.<IMBlazeEntity>of(IMBlazeEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(0.6F, 1.8F).eyeHeight(1.53F)
                    .clientTrackingRange(8));
    EntityType<IMBreezeEntity> BREEZE = register("breeze",
            EntityType.Builder.<IMBreezeEntity>of(
                            IMBreezeEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.77F).eyeHeight(1.3452F)
                    .clientTrackingRange(10));
    EntityType<IMSilverfishEntity> SILVERFISH = register("silverfish",
            EntityType.Builder.<IMSilverfishEntity>of(
                            IMSilverfishEntity::new, MobCategory.MONSTER)
                    .sized(0.4F, 0.3F).eyeHeight(0.13F)
                    .clientTrackingRange(8));
    EntityType<IMEndermiteEntity> ENDERMITE = register("endermite",
            EntityType.Builder.<IMEndermiteEntity>of(
                            IMEndermiteEntity::new, MobCategory.MONSTER)
                    .sized(0.4F, 0.3F).eyeHeight(0.13F)
                    .clientTrackingRange(8));
    EntityType<IMSlimeEntity> SLIME = register("slime",
            EntityType.Builder.<IMSlimeEntity>of(
                            IMSlimeEntity::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F).eyeHeight(0.325F)
                    .spawnDimensionsScale(4.0F).clientTrackingRange(10));
    EntityType<IMMagmaCubeEntity> MAGMA_CUBE = register("magma_cube",
            EntityType.Builder.<IMMagmaCubeEntity>of(
                            IMMagmaCubeEntity::new, MobCategory.MONSTER)
                    .fireImmune().sized(0.52F, 0.52F).eyeHeight(0.325F)
                    .spawnDimensionsScale(4.0F).clientTrackingRange(10));
    EntityType<IMEndermanEntity> ENDERMAN = register("enderman", EntityType.Builder.<IMEndermanEntity>of(IMEndermanEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 2.9F).eyeHeight(2.55F).clientTrackingRange(8));
    EntityType<IMPhantomEntity> PHANTOM = register(
            "phantom",
            EntityType.Builder.<IMPhantomEntity>of(
                            IMPhantomEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F).eyeHeight(0.175F)
                    .passengerAttachments(0.3375F)
                    .clientTrackingRange(8));
    EntityType<IMWolfEntity> WOLF = register("wolf", EntityType.Builder.<IMWolfEntity>of(IMWolfEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 0.85F).eyeHeight(0.68F).passengerAttachments(new Vec3(0.0, 0.81875, -0.0625)).clientTrackingRange(10));

    EntityType<SpiderEggEntity> SPIDER_EGG = register("spider_egg", EntityType.Builder.<SpiderEggEntity>of(SpiderEggEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.8F).eyeHeight(0.5F).clientTrackingRange(10));

    EntityType<TrapEntity> TRAP = register("trap", EntityType.Builder.<TrapEntity>of(TrapEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.28F).fireImmune().clientTrackingRange(10).noSummon());

    @Deprecated
    EntityType<SfxEntity> SFX = register("sfx", EntityType.Builder.<SfxEntity>of(SfxEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F).clientTrackingRange(8).noSummon().noSave());
    EntityType<SpawnProxyEntity> SPAWN_PROXY = register("spawn_proxy", EntityType.Builder.<SpawnProxyEntity>of(SpawnProxyEntity::new, MobCategory.MONSTER)
            .sized(0.5F, 0.5F).clientTrackingRange(8).noSummon().noSave());
    EntityType<ElectricityBoltEntity> BOLT = register("bolt", EntityType.Builder.<ElectricityBoltEntity>of(ElectricityBoltEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F).clientTrackingRange(8).noSummon().noSave());
    EntityType<BoulderEntity> BOULDER = register("boulder", EntityType.Builder.<BoulderEntity>of(BoulderEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F).clientTrackingRange(8));
    EntityType<SkeletonArrowEntity> SKELETON_ARROW = register("skeleton_arrow",
            EntityType.Builder.<SkeletonArrowEntity>of(SkeletonArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20));
    EntityType<IMThrownItemEntity> THROWN_ITEM = register("thrown_item",
            EntityType.Builder.<IMThrownItemEntity>of(
                            IMThrownItemEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4)
                    .updateInterval(10));
    EntityType<EntityIMPrimedTNT> TNT = register("tnt", EntityType.Builder.<EntityIMPrimedTNT>of(EntityIMPrimedTNT::new, MobCategory.MISC)
            .fireImmune().sized(0.98F, 0.98F).eyeHeight(0.15F).clientTrackingRange(10).updateInterval(10));

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        var id = InvasionMod.id(name);
        var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
    }

    private static <T extends Entity> EntityType.Builder<T> betaFeature(EntityType.Builder<T> builder) {
        return InvasionMod.getConfig().debugMode ? builder : builder.noSummon().noSave();
    }

    static void bootstrap() {
        FabricDefaultAttributeRegistry.register(SKELETON, IMSkeletonEntity.createIMSkeletonAttributes());
        FabricDefaultAttributeRegistry.register(
                BOGGED, IMBoggedEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                PARCHED, IMParchedEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                STRAY, IMSkeletonEntity.createIMSkeletonAttributes());
        FabricDefaultAttributeRegistry.register(
                WITHER_SKELETON, IMSkeletonEntity.createIMSkeletonAttributes());
        FabricDefaultAttributeRegistry.register(WITCH, Witch.createAttributes());
        FabricDefaultAttributeRegistry.register(GHAST, Ghast.createAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE, EntityIMZombie.createTierT1V0Attributes());
        FabricDefaultAttributeRegistry.register(
                SPEEDY_ZOMBIE,
                EntityIMZombie.createTierT1V0Attributes());
        FabricDefaultAttributeRegistry.register(
                HUSK, EntityIMZombie.createTierT1V0Attributes());
        FabricDefaultAttributeRegistry.register(
                DROWNED, IMDrownedEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                ZOMBIE_VILLAGER, EntityIMZombie.createTierT1V0Attributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_PIGMAN, EntityIMZombiePigman.createT1Attributes());
        FabricDefaultAttributeRegistry.register(ZOGLIN, IMZoglinEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WITHER, IMWitherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WARDEN, IMWardenEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(
                ZOMBIFIED_PIGLIN,
                IMZombifiedPiglinEntity.createIMAttributes());
        FabricDefaultAttributeRegistry.register(PIGMAN_ENGINEER, PigmanEngineerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_BUILDER, PigmanEngineerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CREEPER, IMCreeperEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER, NexusSpiderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(JUMPING_SPIDER, JumpingSpiderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CAVE_SPIDER, IMCaveSpiderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(QUEEN_SPIDER, QueenSpiderEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(THROWER, ThrowerEntity.createT1V0Attributes());
        FabricDefaultAttributeRegistry.register(BURROWER, BurrowerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(IMP, ImpEnitty.createAttributes());
        FabricDefaultAttributeRegistry.register(BLAZE, net.minecraft.world.entity.monster.Blaze.createAttributes());
        FabricDefaultAttributeRegistry.register(BREEZE,
                net.minecraft.world.entity.monster.breeze.Breeze
                        .createAttributes());
        FabricDefaultAttributeRegistry.register(SILVERFISH, Silverfish.createAttributes());
        FabricDefaultAttributeRegistry.register(ENDERMITE, Endermite.createAttributes());
        FabricDefaultAttributeRegistry.register(SLIME,
                net.minecraft.world.entity.monster.Monster
                        .createMonsterAttributes());
        FabricDefaultAttributeRegistry.register(
                MAGMA_CUBE, MagmaCube.createAttributes());
        FabricDefaultAttributeRegistry.register(ENDERMAN, IMEndermanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PHANTOM, IMPhantomEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WOLF, IMWolfEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER_EGG, SpiderEggEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SPAWN_PROXY, Mob.createMobAttributes());

        IMGhastEntity.bootstrap();

        InvasionConfig config = InvasionMod.getConfig();

        if (config.nightSpawnsEnabled) {
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.SPIDER), SPAWN_PROXY.getCategory(), SPAWN_PROXY, config.nightMobSpawnChance, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityTypes.ZOMBIE), ZOMBIE.getCategory(), ZOMBIE, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityTypes.SPIDER), SPIDER.getCategory(), SPIDER, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityTypes.SPIDER), SPIDER.getCategory(), JUMPING_SPIDER, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityTypes.SPIDER), SPIDER.getCategory(), QUEEN_SPIDER, 1, 1, 1);
            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityTypes.SKELETON), SKELETON.getCategory(), SKELETON, 1, 1, 1);
        }

        if (config.maxNightMobs != 70) {
            try {
                // TODO: Use a mixin for this. Reflection is slow and won't work in an obfuscated environment
                Field field = MobCategory.class.getDeclaredField("capacity");
                field.setAccessible(true);
                field.set(MobCategory.MONSTER, config.maxNightMobs);
            } catch (Exception e) {
                InvasionMod.LOGGER.error("Error whilst updating max hostile entity cap", e);
            }
        }
    }
}
