package com.invasion.entity;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
    List<Attribute> TIER_SCALING_ATTRIBUTES = List.of(
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.KNOCKBACK_RESISTANCE
    );

    static void toggleAttribute(Mob entity, Attribute attribute, AttributeModifier modifier, boolean apply) {
        AttributeInstance instance = entity.getAttribute(attribute);
        instance.removeModifier(modifier.getId());
        if (apply) {
            instance.addTransientModifier(modifier);
        }
    }

    static void toggleAttribute(Mob entity, List<Attribute> attributes, AttributeModifier modifier, boolean apply) {
        attributes.forEach(attribute -> toggleAttribute(entity, attribute, modifier, apply));
    }

    static AttributeModifier addToBase(ResourceLocation id, float amount) {
        return new AttributeModifier(UUID.nameUUIDFromBytes(id.toString()
                .getBytes(StandardCharsets.UTF_8)), id.toString(), amount,
                AttributeModifier.Operation.ADDITION);
    }

    static AttributeModifier addPercentage(ResourceLocation id, float amount) {
        return new AttributeModifier(UUID.nameUUIDFromBytes(id.toString()
                .getBytes(StandardCharsets.UTF_8)), id.toString(), amount / 100F,
                AttributeModifier.Operation.MULTIPLY_BASE);
    }

    static AttributeModifier multiplyTotal(ResourceLocation id, float multiplier) {
        return new AttributeModifier(UUID.nameUUIDFromBytes(id.toString()
                .getBytes(StandardCharsets.UTF_8)), id.toString(), multiplier - 1,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
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
                    MobEffects.JUMP
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
                    MobEffects.SLOW_FALLING
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
