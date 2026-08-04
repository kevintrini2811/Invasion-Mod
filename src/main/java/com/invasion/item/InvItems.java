package com.invasion.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import com.invasion.InvasionMod;
import com.invasion.block.InvBlocks;
import com.invasion.entity.TrapEntity;
import com.invasion.entity.InvEntities;
import com.invasion.entity.NexusEntity;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.registries.RegisterEvent;

public interface InvItems {
    List<Item> REGISTRY = new ArrayList<>();
    List<Item> SPAWN_EGGS = new ArrayList<>();
    List<PendingItem> PENDING_ITEMS = new ArrayList<>();

    Item PHASE_CRYSTAL = register("phase_crystal", p -> new Item(p));
    Item RIFT_FLUX = register("rift_flux", p -> new Item(p));
    Item SMALL_REMNANTS = register("small_remnants", p -> new Item(p));

    Item INFUSED_SWORD = register("infused_sword", InfusedSwordItem::new);
    Item SEARING_BOW = register("searing_bow", p -> new SearingBowItem(p.durability(384)));
    Item ENGY_HAMMER = register("engineer_hammer", EngineerHammerItem::new);

    Item EMPTY_TRAP = register("empty_trap", p -> new Item(p));
    Item RIFT_TRAP = register("rift_trap", p -> new TrapItem(p, TrapEntity.Type.RIFT));
    Item FLAME_TRAP = register("flame_trap", p -> new TrapItem(p, TrapEntity.Type.FIRE));
    // TODO: Ice trap
    // Item XYZ_TRAP = register("xyz_trap", new ItemTrap(new Item.Settings()));

    Item CATALYST_MIXTURE = register("catalyst_mixture", p -> new Item(p));
    Item STABLE_CATALYST_MIXTURE = register("stable_catalyst_mixture", p -> new Item(p));

    Item NEXUS_CATALYST = register("nexus_catalyst", p -> new Item(p));
    Item STABLE_NEXUS_CATALYST = register("stable_nexus_catalyst", p -> new Item(p));
    Item STRONG_NEXUS_CATALYST = register("strong_nexus_catalyst", p -> new Item(p));

    Item DAMPING_AGENT = register("damping_agent", p -> new Item(p));
    Item STRONG_DAMPING_AGENT = register("strong_damping_agent", p -> new Item(p));

    Item STRANGE_BONE = register("strange_bone", p -> new StrangeBoneItem(p));
    Item NEXUS_ADJUSTER = register("nexus_adjuster", p -> new ProbeItem(p.stacksTo(1), false));
    Item MATERIAL_PROBE = register("material_probe", p -> new ProbeItem(p.stacksTo(1), true));

    //ItemSpawnEgg SPAWN_EGG;
    // TODO: Spawn eggs

    ResourceKey<Item> DEBUG_WAND = ResourceKey.create(Registries.ITEM, InvasionMod.id("debug_wand"));

    Item NEXUS_CORE = register("nexus_core", p -> new BlockItem(InvBlocks.NEXUS_CORE, p));

    Item ZOMBIE_SPAWN_EGG = register("zombie_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE, 0x5B6335, 0x00A8A8, NexusEntity.createVariant(0, 1)));
    Item SPEEDY_ZOMBIE_SPAWN_EGG = register(
            "speedy_zombie_spawn_egg",
            p -> createSpawnEgg(
                    p, InvEntities.SPEEDY_ZOMBIE,
                    0x049DAD, 0x003C41,
                    NexusEntity.createVariant(0, 1)));
    Item HUSK_SPAWN_EGG = register(
            "husk_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.HUSK, 0x6A5A3A, 0xD0B36A));
    Item DROWNED_SPAWN_EGG = register(
            "drowned_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.DROWNED, 0x4A6C6D, 0x8F9A6D));
    Item ZOMBIE_VILLAGER_SPAWN_EGG = register(
            "zombie_villager_spawn_egg",
            p -> createSpawnEgg(
                    p, InvEntities.ZOMBIE_VILLAGER, 0x563C33, 0x799C65));
    Item ARMED_ZOMBIE_SPAWN_EGG = register("armed_zombie_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE, 0x5B6335, 0x563A20, NexusEntity.createVariant(1, 1)));
    Item TIER_TWO_ZOMBIE_SPAWN_EGG = register("tier_two_zombie_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE, 0x476F34, 0x5D5F5C, NexusEntity.createVariant(0, 2)));
    Item TIER_TWO_ARMED_ZOMBIE_SPAWN_EGG = register("tier_two_armed_zombie_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE, 0x476F34, 0x526865, NexusEntity.createVariant(1, 2)));
    Item TAR_ZOMBIE_SPAWN_EGG = register("tar_zombie_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE, 0x4F5A32, 0x1E2518, NexusEntity.createVariant(2, 2)));
    Item ZOMBIE_BRUTE_SPAWN_EGG = register("zombie_brute_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE, 0x1E4538, 0x595D4D, NexusEntity.createVariant(0, 3)));
    Item SKELETON_SPAWN_EGG = register("skeleton_spawn_egg", p -> createSpawnEgg(p, InvEntities.SKELETON, 0x9B9B9B, 0x797979));
    Item STRAY_SPAWN_EGG = register(
            "stray_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.STRAY, 0x617677, 0xDDEAEA));
    Item WITHER_SKELETON_SPAWN_EGG = register("wither_skeleton_spawn_egg", p -> createSpawnEgg(p, InvEntities.WITHER_SKELETON, 0x141414, 0x474D4D));
    Item WITCH_SPAWN_EGG = register("witch_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.WITCH, 0x340000, 0x51A03E));

    Item SPIDER_SPAWN_EGG = register("spider_spawn_egg", p -> createSpawnEgg(p, InvEntities.SPIDER, 0x504A3E, 0xA4121C));
    Item JUMPING_SPIDER_SPAWN_EGG = register("jumping_spider_spawn_egg", p -> createSpawnEgg(p, InvEntities.JUMPING_SPIDER, 0x444167, 0x0A0328));
    Item CAVE_SPIDER_SPAWN_EGG = register("cave_spider_spawn_egg", p -> createSpawnEgg(p, InvEntities.CAVE_SPIDER, 0x0C424E, 0xA80E0E));
    Item MOTHER_SPIDER_SPAWN_EGG = register("mother_spider_spawn_egg", p -> createSpawnEgg(p, InvEntities.QUEEN_SPIDER, 0x2D2C25, 0x191814));

    Item CREEPER_SPAWN_EGG = register("creeper_spawn_egg", p -> createSpawnEgg(p, InvEntities.CREEPER, 0x238F1F, 0xA5AAA6));
    Item PIGMAN_ENGINEER_SPAWN_EGG = register("pigman_engineer_spawn_egg", p -> createSpawnEgg(p, InvEntities.PIGMAN_ENGINEER, 0xEC9695, 0x420000));
    Item THROWER_SPAWN_EGG = register("thrower_spawn_egg", p -> createSpawnEgg(p, InvEntities.THROWER, 0x586039, 0x06090C));
    Item BIG_THROWER_SPAWN_EGG = register("big_thrower_spawn_egg", p -> createSpawnEgg(p, InvEntities.THROWER, 0x394119, 0x181C0A, NexusEntity.createVariant(0, 2)));
    Item IMP_SPAWN_EGG = register("imp_spawn_egg", p -> createSpawnEgg(p, InvEntities.IMP, 0xB40113, 0xFF0000));
    Item BLAZE_SPAWN_EGG = register("blaze_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.BLAZE, 0xF6B201, 0xFFF87E));
    Item SILVERFISH_SPAWN_EGG = register("silverfish_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.SILVERFISH, 0x6E6E6E, 0x303030));
    Item ENDERMAN_SPAWN_EGG = register("enderman_spawn_egg", p -> createSpawnEgg(p, InvEntities.ENDERMAN, 0x161616, 0xE079FA));
    Item PHANTOM_SPAWN_EGG = register("phantom_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.PHANTOM, 0x43518A, 0x88A3BE));
    Item BURROWER_SPAWN_EGG = register("burrower_spawn_egg", p -> createSpawnEgg(p, InvEntities.BURROWER, 0x622523, 0x3D1110));
    Item WOLF_SPAWN_EGG = register("wolf_spawn_egg", p -> createSpawnEgg(p, InvEntities.WOLF, 0xE6E4E4, 0xD3C3B8));
    Item ZOMBIE_PIGMAN_SPAWN_EGG = register("pigman_zombie_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE_PIGMAN, 0xE89493, 0x4F5627, NexusEntity.createVariant(1, 1)));
    Item ZOMBIFIED_PIGLIN_SPAWN_EGG = register(
            "zombified_piglin_spawn_egg",
            p -> createSpawnEgg(
                    p, InvEntities.ZOMBIFIED_PIGLIN, 0xEA9393, 0x4C7129));
    Item TIER_TWO_ZOMBIE_PIGMAN_SPAWN_EGG = register("tier_two_pigman_zombie_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE_PIGMAN, 0xE89493, 0x4F5627, NexusEntity.createVariant(1, 2)));
    Item ZOMBIE_PIGMAN_BRUTE_SPAWN_EGG = register("zombie_pigman_brute_spawn_egg", p -> createSpawnEgg(p, InvEntities.ZOMBIE_PIGMAN, 0xE6908F, 0x545627, NexusEntity.createVariant(1, 3)));
    Item ZOGLIN_SPAWN_EGG = register("zoglin_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.ZOGLIN, 0xC66E55, 0x5F6464));
    Item WITHER_SPAWN_EGG = register("wither_spawn_egg",
            p -> createSpawnEgg(p, InvEntities.WITHER, 0x141414, 0x4A4A4A));

    private static Item createSpawnEgg(Item.Properties properties, EntityType<? extends Mob> type, int primaryColor, int secondaryColor, CompoundTag data) {
        InvasionSpawnEggItem egg = new InvasionSpawnEggItem(
                properties, type, primaryColor, secondaryColor, data);
        SPAWN_EGGS.add(egg);
        return egg;
    }

    private static Item createSpawnEgg(Item.Properties properties, EntityType<? extends Mob> type, int primaryColor, int secondaryColor) {
        InvasionSpawnEggItem egg = new InvasionSpawnEggItem(
                properties, type, primaryColor, secondaryColor);
        SPAWN_EGGS.add(egg);
        return egg;
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory) {
        var id = InvasionMod.id(name);
        var key = ResourceKey.create(Registries.ITEM, id);
        T item = factory.apply(new Item.Properties());
        REGISTRY.add(item);
        PENDING_ITEMS.add(new PendingItem(id, item));
        return item;
    }

    static void bootstrap(RegisterEvent event) {
        if (InvasionMod.getConfig().debugMode) {
            register("debug_wand", p -> new DebugWandItem(p.stacksTo(1)));
        }
        event.register(Registries.ITEM, helper ->
                PENDING_ITEMS.forEach(entry -> helper.register(entry.id(), entry.item())));
    }

    static void bootstrapCreativeTab(RegisterEvent event) {
        ResourceLocation tabId = InvasionMod.id("invasion_mod");
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(tabId, CreativeModeTab.builder().displayItems((context, entries) -> {
            REGISTRY.forEach(item -> entries.accept(item.getDefaultInstance()));
        }).icon(NEXUS_CORE::getDefaultInstance).title(Component.translatable(Util.makeDescriptionId("itemGroup", tabId))).build()));
    }

    record PendingItem(ResourceLocation id, Item item) {}

    static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            SPAWN_EGGS.forEach(event::accept);
        }
    }

    static void fuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        if (event.getItemStack().is(NEXUS_CATALYST)) {
            event.setBurnTime(10);
        } else if (event.getItemStack().is(STABLE_NEXUS_CATALYST)) {
            event.setBurnTime(16);
        }
    }
}
