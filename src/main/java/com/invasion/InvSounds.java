package com.invasion;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public interface InvSounds {
    SoundEvent ENTITY_SCRAPE = register("entity.scrape");
    SoundEvent ENTITY_EXPLODE = register("entity.explode");
    SoundEvent ENTITY_CRASH = register("entity.crash");

    SoundEvent BLOCK_NEXUS_RUMBLE = register("block.nexus.rumble");
    SoundEvent BLOCK_NEXUS_CHIME = register("block.nexus.chime");

    SoundEvent ENTITY_THROWER_RAGE = register("entity.thrower.rage");

    SoundEvent ENTITY_BOULDER_LAND = register("entity.boulder.land");

    SoundEvent ENTITY_LIGHTNING_ZAP = register("entity.lightning.zap");
    SoundEvent ENTITY_SPIDER_EGG_HATCH = register("entity.spider_egg.hatch");
    SoundEvent ENTITY_BIG_ZOMBIE_AMBIENT = register("entity.big_zombie.ambient");

    SoundEvent ENTITY_TRAP_READY = register("entity.trap.ready");
    SoundEvent ENTITY_TRAP_COUNTDOWN = register("entity.trap.countdown");

    private static SoundEvent register(String name) {
        ResourceLocation id = InvasionMod.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    static void boostrap() {}
}
