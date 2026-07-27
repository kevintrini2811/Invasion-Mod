package com.invasion.entity;

import java.lang.reflect.Field;

import com.invasion.InvasionMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public interface InvEntities {
    EntityType<IMSkeletonEntity> SKELETON = register("skeleton", EntityType.Builder.<IMSkeletonEntity>of(IMSkeletonEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<EntityIMZombie> ZOMBIE = register("zombie", EntityType.Builder.<EntityIMZombie>of(EntityIMZombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<EntityIMZombiePigman> ZOMBIE_PIGMAN = register("zombie_pigman", EntityType.Builder.<EntityIMZombiePigman>of(EntityIMZombiePigman::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<PigmanEngineerEntity> PIGMAN_ENGINEER = register("pigman_engineer", EntityType.Builder.<PigmanEngineerEntity>of(PigmanEngineerEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMCreeperEntity> CREEPER = register("creeper", EntityType.Builder.<IMCreeperEntity>of(IMCreeperEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.7F).clientTrackingRange(8));

    EntityType<NexusSpiderEntity> SPIDER = register("spider", EntityType.Builder.<NexusSpiderEntity>of(NexusSpiderEntity::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).clientTrackingRange(8));
    EntityType<JumpingSpiderEntity> JUMPING_SPIDER = register("jumping_spider", EntityType.Builder.<JumpingSpiderEntity>of(JumpingSpiderEntity::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).clientTrackingRange(8));
    EntityType<QueenSpiderEntity> QUEEN_SPIDER = register("queen_spider", EntityType.Builder.<QueenSpiderEntity>of(QueenSpiderEntity::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F).clientTrackingRange(8));

    EntityType<ThrowerEntity> THROWER = register("thrower", EntityType.Builder.<ThrowerEntity>of(ThrowerEntity::new, MobCategory.MONSTER)
            .sized(1.8F, 1.95F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<BurrowerEntity> BURROWER = register("burrower", EntityType.Builder.<BurrowerEntity>of(BurrowerEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<ImpEnitty> IMP = register("imp", EntityType.Builder.<ImpEnitty>of(ImpEnitty::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8));
    EntityType<IMEndermanEntity> ENDERMAN = register("enderman", EntityType.Builder.<IMEndermanEntity>of(IMEndermanEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 2.9F).eyeHeight(2.55F).clientTrackingRange(8));
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
    EntityType<EntityIMPrimedTNT> TNT = register("tnt", EntityType.Builder.<EntityIMPrimedTNT>of(EntityIMPrimedTNT::new, MobCategory.MISC)
            .fireImmune().sized(0.98F, 0.98F).eyeHeight(0.15F).clientTrackingRange(10).updateInterval(10));

    EntityType<VultureEntity> BIRD = register("bird", betaFeature(EntityType.Builder.<VultureEntity>of(VultureEntity::new, MobCategory.MONSTER)
            .sized(1, 1).clientTrackingRange(10).updateInterval(10)));
    EntityType<EntityIMGiantBird> VULTURE = register("vulture", betaFeature(EntityType.Builder.<EntityIMGiantBird>of(EntityIMGiantBird::new, MobCategory.MONSTER)
            .attach(EntityAttachment.VEHICLE, 0, -0.2F, 0)
            .sized(1.9F, 2.8F).clientTrackingRange(10).updateInterval(10)));

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, InvasionMod.id(name), builder.build(name));
    }

    private static <T extends Entity> EntityType.Builder<T> betaFeature(EntityType.Builder<T> builder) {
        return InvasionMod.getConfig().debugMode ? builder : builder.noSummon().noSave();
    }

    static void bootstrap() {
        if (InvasionMod.getConfig().maxNightMobs != 70) {
            try {
                // TODO: Use a mixin for this. Reflection is slow and won't work in an obfuscated environment
                Field field = MobCategory.class.getDeclaredField("capacity");
                field.setAccessible(true);
                field.set(MobCategory.MONSTER, InvasionMod.getConfig().maxNightMobs);
            } catch (Exception e) {
                InvasionMod.LOGGER.error("Error whilst updating max hostile entity cap", e);
            }
        }
    }

    final class Attributes {
        private Attributes() {
        }

        public static void register(EntityAttributeCreationEvent event) {
            event.put(SKELETON, IMSkeletonEntity.createIMSkeletonAttributes().build());
            event.put(ZOMBIE, EntityIMZombie.createTierT1V0Attributes().build());
            event.put(ZOMBIE_PIGMAN, EntityIMZombiePigman.createT1Attributes().build());
            event.put(PIGMAN_ENGINEER, PigmanEngineerEntity.createAttributes().build());
            event.put(CREEPER, IMCreeperEntity.createAttributes().build());
            event.put(SPIDER, NexusSpiderEntity.createAttributes().build());
            event.put(JUMPING_SPIDER, JumpingSpiderEntity.createAttributes().build());
            event.put(QUEEN_SPIDER, QueenSpiderEntity.createAttributes().build());
            event.put(THROWER, ThrowerEntity.createT1V0Attributes().build());
            event.put(BURROWER, BurrowerEntity.createAttributes().build());
            event.put(IMP, ImpEnitty.createAttributes().build());
            event.put(WOLF, IMWolfEntity.createAttributes().build());
            event.put(SPIDER_EGG, SpiderEggEntity.createAttributes().build());
            event.put(SPAWN_PROXY, Mob.createMobAttributes().build());
            event.put(BIRD, VultureEntity.createBirdAttributes().build());
            event.put(VULTURE, EntityIMGiantBird.createVultureAttributes().build());
        }
    }
}
