package com.invasion;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class InvMobEffects {
    public static final Holder<MobEffect> INVASION_STRENGTH = register(
            "invasion_strength",
            new InvasionEffect(MobEffectCategory.BENEFICIAL, 0xB61E1E)
                    .addAttributeModifier(
                            Attributes.ATTACK_DAMAGE,
                            InvasionMod.id("invasion_strength"),
                            1.0D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private InvMobEffects() {
    }

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        var id = InvasionMod.id(name);
        var key = ResourceKey.create(BuiltInRegistries.MOB_EFFECT.key(), id);
        Registry.register(BuiltInRegistries.MOB_EFFECT, id, effect);
        return BuiltInRegistries.MOB_EFFECT.get(key).orElseThrow();
    }

    public static void bootstrap() {
    }

    private static final class InvasionEffect extends MobEffect {
        private InvasionEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
