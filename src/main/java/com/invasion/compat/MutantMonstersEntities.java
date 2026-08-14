package com.invasion.compat;

import java.util.List;

import com.invasion.InvasionMod;
import com.invasion.entity.mutant.IMMutantCreeperEntity;
import com.invasion.entity.mutant.IMCreeperMinionEntity;
import com.invasion.entity.mutant.IMMutantEndermanEntity;
import com.invasion.entity.mutant.IMMutantSkeletonEntity;
import com.invasion.entity.mutant.IMMutantZombieEntity;
import com.invasion.entity.mutant.IMSpiderPigEntity;

import fuzs.mutantmonsters.world.entity.mutant.MutantCreeper;
import fuzs.mutantmonsters.world.entity.CreeperMinion;
import fuzs.mutantmonsters.world.entity.mutant.MutantEnderman;
import fuzs.mutantmonsters.world.entity.mutant.MutantSkeleton;
import fuzs.mutantmonsters.world.entity.mutant.MutantZombie;
import fuzs.mutantmonsters.world.entity.mutant.SpiderPig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

/** Entity registrations that are loaded only when Mutant Monsters is present. */
public final class MutantMonstersEntities {
    public static final EntityType<IMCreeperMinionEntity> CREEPER_MINION = register(
            "creeper_minion", EntityType.Builder.of(
                    IMCreeperMinionEntity::new, MobCategory.MONSTER)
                    .sized(0.3F, 0.85F));
    public static final EntityType<IMMutantZombieEntity> MUTANT_ZOMBIE = register(
            "mutant_zombie", EntityType.Builder.of(
                    IMMutantZombieEntity::new, MobCategory.MONSTER)
                    .sized(1.8F, 3.2F));
    public static final EntityType<IMMutantCreeperEntity> MUTANT_CREEPER = register(
            "mutant_creeper", EntityType.Builder.of(
                    IMMutantCreeperEntity::new, MobCategory.MONSTER)
                    .sized(1.99F, 2.8F));
    public static final EntityType<IMMutantSkeletonEntity> MUTANT_SKELETON = register(
            "mutant_skeleton", EntityType.Builder.of(
                    IMMutantSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.6F));
    public static final EntityType<IMMutantEndermanEntity> MUTANT_ENDERMAN = register(
            "mutant_enderman", EntityType.Builder.of(
                    IMMutantEndermanEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 4.2F));
    public static final EntityType<IMSpiderPigEntity> SPIDER_PIG = register(
            "spider_pig", EntityType.Builder.of(
                    IMSpiderPigEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F));

    private MutantMonstersEntities() {
    }

    public static List<EntityType<? extends Mob>> mobTypes() {
        return List.of(MUTANT_ZOMBIE, MUTANT_CREEPER, MUTANT_SKELETON,
                MUTANT_ENDERMAN, SPIDER_PIG, CREEPER_MINION);
    }

    public static List<EntityType<? extends Mob>> freeBossTypes() {
        return List.of(MUTANT_ZOMBIE, MUTANT_CREEPER,
                MUTANT_SKELETON, MUTANT_ENDERMAN);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(MUTANT_ZOMBIE, MutantZombie.registerAttributes().build());
        event.put(MUTANT_CREEPER, MutantCreeper.registerAttributes().build());
        event.put(MUTANT_SKELETON, MutantSkeleton.registerAttributes().build());
        event.put(MUTANT_ENDERMAN, MutantEnderman.registerAttributes().build());
        event.put(SPIDER_PIG, SpiderPig.registerAttributes().build());
        event.put(CREEPER_MINION, CreeperMinion.registerAttributes().build());
    }

    public static void bootstrap() {
        // Class initialization performs entity registration.
    }

    private static <T extends Entity> EntityType<T> register(
            String name, EntityType.Builder<T> builder) {
        var id = InvasionMod.id(name);
        return InvasionMod.INSTANCE.register(
                Registries.ENTITY_TYPE, id, builder.build(id.toString()));
    }
}
