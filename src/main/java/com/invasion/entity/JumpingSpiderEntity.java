package com.invasion.entity;

import com.invasion.entity.ai.goal.PounceGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

public class JumpingSpiderEntity extends NexusSpiderEntity {
	public JumpingSpiderEntity(EntityType<JumpingSpiderEntity> type, Level world) {
		super(type, world);
	}

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.GRAVITY, 0.043);
    }

    @Override
    protected float getGlobalScaleMultiplier() {
        return super.getGlobalScaleMultiplier();
    }

    @Override
    protected void initExtraGoals() {
        goalSelector.addGoal(4, new PounceGoal(this, 0.2F, 1.55F, 18));
    }

    @Override
    public int getMaxFallDistance() {
        return 13;
    }
}
