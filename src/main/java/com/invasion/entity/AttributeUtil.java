package com.invasion.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;
import com.invasion.InvasionMod;

public interface AttributeUtil {
    ResourceLocation NEXUS_WAVE_DIFFICULTY_BUFFS = InvasionMod.id("nexus_wave_difficulty_buff");
    List<Holder<Attribute>> TIER_SCALING_ATTRIBUTES = List.of(
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.KNOCKBACK_RESISTANCE
    );

    static void toggleAttribute(Mob entity, Holder<Attribute> attribute, AttributeModifier modifier, boolean apply) {
        AttributeInstance instance = entity.getAttribute(attribute);
        instance.removeModifier(modifier.id());
        if (apply) {
            instance.addTransientModifier(modifier);
        }
    }

    static void toggleAttribute(Mob entity, List<Holder<Attribute>> attributes, AttributeModifier modifier, boolean apply) {
        attributes.forEach(attribute -> toggleAttribute(entity, attribute, modifier, apply));
    }

    static AttributeModifier addToBase(ResourceLocation id, float amount) {
        return new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    static AttributeModifier addPercentage(ResourceLocation id, float amount) {
        return new AttributeModifier(id, amount / 100F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    static AttributeModifier multiplyTotal(ResourceLocation id, float multiplier) {
        return new AttributeModifier(id, multiplier - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    static void applyNexusWaveComplications(Mob entity, ServerLevelAccessor world, int currentWave, DifficultyInstance difficulty, MobSpawnType spawnReason) {
        toggleAttribute(entity, TIER_SCALING_ATTRIBUTES, multiplyTotal(NEXUS_WAVE_DIFFICULTY_BUFFS, 1.0001F * currentWave), true);

        if (entity instanceof MountableEntity mountable) {
            mountable.generateJockey(world, currentWave, difficulty, spawnReason);
        }

        if (currentWave > 5) {
            int effectAttempts = currentWave - 15;
            var unfairEffects = new ArrayList<>(List.of(
                    MobEffects.DAMAGE_RESISTANCE,
                    MobEffects.FIRE_RESISTANCE,
                    MobEffects.WATER_BREATHING,
                    MobEffects.JUMP,
                    MobEffects.WIND_CHARGED
            ));
            while (--effectAttempts > 0 && !unfairEffects.isEmpty()) {
                RandomSource random = world.getRandom();
                if (random.nextInt(100) == 0) {
                    entity.addEffect(new MobEffectInstance(unfairEffects.remove(random.nextInt(unfairEffects.size())), -1));
                }
            }
        }

        if (currentWave > 15) {
            int effectAttempts = currentWave - 15;
            var unfairEffects = new ArrayList<>(List.of(
                    MobEffects.INFESTED,
                    MobEffects.SLOW_FALLING,
                    MobEffects.OOZING,
                    MobEffects.WEAVING
            ));
            while (--effectAttempts > 0 && !unfairEffects.isEmpty()) {
                RandomSource random = world.getRandom();
                if (random.nextInt(100) == 0) {
                    entity.addEffect(new MobEffectInstance(unfairEffects.remove(random.nextInt(unfairEffects.size())), -1));
                }
            }
        }
    }
}
