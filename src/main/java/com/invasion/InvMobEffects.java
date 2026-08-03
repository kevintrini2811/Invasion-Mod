package com.invasion;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class InvMobEffects {
    public static final MobEffect INVASION_STRENGTH = register(
            "invasion_strength",
            new InvasionEffect(MobEffectCategory.BENEFICIAL, 0xB61E1E)
                    .addAttributeModifier(
                            Attributes.ATTACK_DAMAGE,
                            "8dfe7e65-29d2-4a5f-a730-777fa51a5e11",
                            1.0D,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));

    private InvMobEffects() {
    }

    private static MobEffect register(String name, MobEffect effect) {
        var id = InvasionMod.id(name);
        return InvasionMod.INSTANCE.register(Registries.MOB_EFFECT, id, effect);
    }

    public static void bootstrap() {
    }

    private static final class InvasionEffect extends MobEffect {
        private InvasionEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
