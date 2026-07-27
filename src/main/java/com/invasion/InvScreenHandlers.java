package com.invasion;

import com.invasion.block.container.NexusScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public interface InvScreenHandlers {
    MenuType<NexusScreenHandler> NEXUS = register("nexus", new MenuType<>(NexusScreenHandler::new, FeatureFlags.VANILLA_SET));

    static <T extends AbstractContainerMenu> MenuType<T> register(String name, MenuType<T> type) {
        return Registry.register(BuiltInRegistries.MENU, InvasionMod.id(name), type);
    }

    static void bootstrap() { }
}
