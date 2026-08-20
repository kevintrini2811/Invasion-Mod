package com.invasion.compat;

import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.fabricmc.loader.api.FabricLoader;

/** Safe entry point that keeps Friends & Foes classes behind a mod check. */
public final class FriendsAndFoesCompatibility {
    public static final String MOD_ID = "friendsandfoes";

    private FriendsAndFoesCompatibility() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static void registerEntity() {
        if (isLoaded()) FriendsAndFoesIntegration.bootstrap();
    }

    public static void registerItem() {
        if (isLoaded()) FriendsAndFoesIntegration.registerItem();
    }

    public static void registerRenderer() {
        if (isLoaded()) FriendsAndFoesIntegration.registerRenderer();
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
