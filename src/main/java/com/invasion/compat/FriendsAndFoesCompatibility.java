package com.invasion.compat;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.ModList;

/** Safe entry point that keeps Friends & Foes classes behind a mod check. */
public final class FriendsAndFoesCompatibility {
    public static final String MOD_ID = "friendsandfoes";

    private FriendsAndFoesCompatibility() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void registerEntity() {
        if (isLoaded()) FriendsAndFoesIntegration.bootstrap();
    }

    public static void registerItem() {
        if (isLoaded()) FriendsAndFoesIntegration.registerItem();
    }

    public static void registerAttributes(
            net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        if (isLoaded()) FriendsAndFoesIntegration.registerAttributes(event);
    }

    public static void registerRenderer(
            net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        if (isLoaded()) FriendsAndFoesIntegration.registerRenderer(event);
    }

    @Nullable
    public static EntityType<? extends Mob> imWildfireType() {
        return isLoaded() ? FriendsAndFoesIntegration.WILDFIRE : null;
    }

    public static boolean isOriginalWildfire(EntityType<?> type) {
        if (!isLoaded()) return false;
        Identifier id = net.minecraft.core.registries.BuiltInRegistries
                .ENTITY_TYPE.getKey(type);
        return id != null && id.equals(Identifier.fromNamespaceAndPath(
                MOD_ID, "wildfire"));
    }

    public static boolean isAnyWildfire(EntityType<?> type) {
        return type == imWildfireType() || isOriginalWildfire(type);
    }
}
