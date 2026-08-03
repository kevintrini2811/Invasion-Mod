package com.invasion.entity;

import java.util.List;
import java.util.Optional;

import com.invasion.InvMobEffects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;

public final class IMWitchPotionEntity extends ThrownSplashPotion {
    private static final int SUPPORT_DURATION = 20 * 20;
    private static final int HARM_DURATION = 5 * 20;
    private Type invasionType = Type.HEALING;

    public IMWitchPotionEntity(
            net.minecraft.world.entity.EntityType<? extends IMWitchPotionEntity> type,
            Level level) {
        super(type, level);
    }

    public IMWitchPotionEntity(Level level, IMWitchEntity owner, Type type) {
        this(InvEntities.WITCH_POTION, level);
        invasionType = type;
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        setItem(createPotionStack(type));
    }

    private static ItemStack createPotionStack(Type type) {
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(), Optional.of(type.color), List.of(),
                Optional.of(type.translationKey)));
        return stack;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("invasionPotionType", invasionType.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Type[] values = Type.values();
        invasionType = values[Math.clamp(
                input.getIntOr("invasionPotionType", 0), 0, values.length - 1)];
    }

    @Override
    public void onHitAsPotion(
            ServerLevel level, ItemStack stack, HitResult hitResult) {
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(4.0D, 2.0D, 4.0D),
                LivingEntity::isAlive)) {
            if (distanceToSqr(entity) > 16.0D) {
                continue;
            }
            if (invasionType.support) {
                if (entity instanceof com.invasion.nexus.Combatant<?>
                        && !(entity instanceof IMWolfEntity)) {
                    applySupport(entity);
                }
            } else if (IMWitchEntity.isPlayerAlly(entity, level)) {
                entity.addEffect(new MobEffectInstance(
                        invasionType.effect, HARM_DURATION), getOwner());
            }
        }
    }

    private void applySupport(LivingEntity entity) {
        switch (invasionType) {
            case HEALING -> entity.heal(10.0F);
            case STRENGTH -> entity.addEffect(new MobEffectInstance(
                    InvMobEffects.INVASION_STRENGTH, SUPPORT_DURATION), getOwner());
            case SPEED -> entity.addEffect(new MobEffectInstance(
                    MobEffects.SPEED, SUPPORT_DURATION, 1), getOwner());
            case HASTE -> entity.addEffect(new MobEffectInstance(
                    MobEffects.HASTE, SUPPORT_DURATION, 1), getOwner());
            default -> {
            }
        }
    }

    public enum Type {
        HEALING(true, 0xF82423, "invasion_healing", null),
        STRENGTH(true, 0xB61E1E, "invasion_strength", null),
        SPEED(true, 0x7CAFC6, "invasion_speed", null),
        HASTE(true, 0xD9C043, "invasion_haste", null),
        NAUSEA(false, 0x551D4A, "invasion_nausea", MobEffects.NAUSEA),
        POISON(false, 0x4E9331, "invasion_poison", MobEffects.POISON),
        WITHER(false, 0x352A27, "invasion_wither", MobEffects.WITHER),
        BLINDNESS(false, 0x1F1F23, "invasion_blindness", MobEffects.BLINDNESS),
        HUNGER(false, 0x587653, "invasion_hunger", MobEffects.HUNGER);

        final boolean support;
        final int color;
        final String translationKey;
        final net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect;

        Type(boolean support, int color, String translationKey,
                net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
            this.support = support;
            this.color = color;
            this.translationKey = translationKey;
            this.effect = effect;
        }
    }
}
