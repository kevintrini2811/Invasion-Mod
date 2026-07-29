package com.invasion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

public class IMCaveSpiderEntity extends NexusSpiderEntity {
    public IMCaveSpiderEntity(EntityType<IMCaveSpiderEntity> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.GRAVITY, 0.08);
    }

    @Override
    protected float getGlobalScaleMultiplier() {
        return super.getGlobalScaleMultiplier() * 0.7F;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!super.doHurtTarget(level, target)) {
            return false;
        }

        if (target instanceof LivingEntity living) {
            int poisonSeconds = level.getDifficulty() == Difficulty.HARD
                    ? 15
                    : level.getDifficulty() == Difficulty.NORMAL ? 7 : 0;
            if (poisonSeconds > 0) {
                living.addEffect(new MobEffectInstance(
                        MobEffects.POISON, poisonSeconds * 20, 0), this);
            }
        }
        return true;
    }
}
