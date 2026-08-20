package com.invasion.compat;

import com.faboslav.friendsandfoes.common.client.render.entity.renderer.WildfireEntityRenderer;
import com.faboslav.friendsandfoes.common.entity.WildfireEntity;
import com.invasion.InvasionMod;
import com.invasion.item.InvasionSpawnEggItem;
import com.invasion.item.InvItems;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.TypedEntityData;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** Registrations loaded only when Friends & Foes is present. */
public final class FriendsAndFoesIntegration {
    public static EntityType<? extends net.minecraft.world.entity.Mob> WILDFIRE;
    public static Item IM_WILDFIRE_SPAWN_EGG;

    private FriendsAndFoesIntegration() {
    }

    @SuppressWarnings("unchecked")
    public static void bootstrap() {
        var id = InvasionMod.id("wildfire");
        var key = net.minecraft.resources.ResourceKey.create(
                Registries.ENTITY_TYPE, id);
        WILDFIRE = Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
                EntityType.Builder.<WildfireEntity>of(
                                WildfireEntity::new, MobCategory.MONSTER)
                        .fireImmune().sized(1.1F, 2.8F).eyeHeight(2.3F)
                        .clientTrackingRange(10).build(key));
        WildfireNexusHandler.bootstrap();
        FabricDefaultAttributeRegistry.register((EntityType) WILDFIRE,
                WildfireEntity.createWildfireAttributes());
    }

    public static void registerItem() {
        var id = com.invasion.InvasionMod.id("wildfire_spawn_egg");
        var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ITEM, id);
        TypedEntityData<EntityType<?>> data = TypedEntityData.of(
                (EntityType<?>) WILDFIRE, new CompoundTag());
        IM_WILDFIRE_SPAWN_EGG = Registry.register(
                BuiltInRegistries.ITEM, id,
                new InvasionSpawnEggItem(
                        new Item.Properties().setId(key)
                                .component(DataComponents.ENTITY_DATA, data),
                        WILDFIRE, data));
        InvItems.REGISTRY.add(IM_WILDFIRE_SPAWN_EGG);
        InvItems.SPAWN_EGGS.add(IM_WILDFIRE_SPAWN_EGG);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerRenderer() {
        EntityRendererRegistry.register((EntityType) WILDFIRE,
                WildfireEntityRenderer::new);
    }

}
