package com.invasion.compat;

import com.faboslav.friendsandfoes.common.client.render.entity.renderer.WildfireEntityRenderer;
import com.faboslav.friendsandfoes.common.entity.WildfireEntity;
import com.invasion.InvasionMod;
import com.invasion.item.InvasionSpawnEggItem;
import com.invasion.item.InvItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;

/** Registrations loaded only when Friends & Foes is present. */
public final class FriendsAndFoesIntegration {
    public static EntityType<? extends net.minecraft.world.entity.Mob> WILDFIRE;
    public static Item IM_WILDFIRE_SPAWN_EGG;

    private FriendsAndFoesIntegration() {
    }

    @SuppressWarnings("unchecked")
    public static void bootstrap() {
        var id = InvasionMod.id("wildfire");
        WILDFIRE = InvasionMod.INSTANCE.register(Registries.ENTITY_TYPE, id,
                EntityType.Builder.<WildfireEntity>of(
                                WildfireEntity::new, MobCategory.MONSTER)
                        .fireImmune().sized(1.1F, 2.8F)
                        .clientTrackingRange(10).build(id.toString()));
        MinecraftForge.EVENT_BUS.addListener(
                WildfireNexusHandler::onProjectileImpact);
    }

    public static void registerItem() {
        var id = com.invasion.InvasionMod.id("wildfire_spawn_egg");
        IM_WILDFIRE_SPAWN_EGG = InvasionMod.INSTANCE.register(
                Registries.ITEM, id,
                new InvasionSpawnEggItem(
                        new Item.Properties(), WILDFIRE, 0xF4B41B, 0x6B3514));
        InvItems.REGISTRY.add(IM_WILDFIRE_SPAWN_EGG);
        InvItems.SPAWN_EGGS.add(IM_WILDFIRE_SPAWN_EGG);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerAttributes(
            net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put((EntityType) WILDFIRE,
                WildfireEntity.createWildfireAttributes().build());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerRenderer(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType) WILDFIRE,
                WildfireEntityRenderer::new);
    }

}
