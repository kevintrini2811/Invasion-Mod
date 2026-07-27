package com.invasion.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import com.invasion.InvTags;
import com.invasion.entity.ai.goal.LayEggGoal;
import com.invasion.entity.ai.goal.PredicatedGoal;

public class QueenSpiderEntity extends NexusSpiderEntity implements Reproducer {
	public QueenSpiderEntity(EntityType<QueenSpiderEntity> type, Level world) {
		super(type, world);
	}

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.59F)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.GRAVITY, 0.18);
    }

    @Override
    protected float getGlobalScaleMultiplier() {
        return super.getGlobalScaleMultiplier() + 1F;
    }

    @Override
    protected void initExtraGoals() {
        goalSelector.addGoal(1, new PredicatedGoal(new LayEggGoal(this, 1, () -> getOffspring(null)), () -> !isBaby()));
    }

    @Override
    public List<Entity> getOffspring(Entity partner) {
        List<Entity> offspring = new ArrayList<>();
        int offspringCount = 3 + level().getRandom().nextInt(4);
        var offspringTypes = java.util.stream.StreamSupport.stream(level().registryAccess()
            .lookupOrThrow(Registries.ENTITY_TYPE)
            .getTagOrEmpty(InvTags.Entities.QUEEN_SPIDER_OFFSPRING).spliterator(), false).toList();
        if (!offspringTypes.isEmpty()) {
            for (int i = 0; i < offspringCount; i++) {
                var type = offspringTypes.get(level().getRandom().nextInt(offspringTypes.size()));
                    Entity child = type.value().create(level(), EntitySpawnReason.EVENT);
                    if (child instanceof NexusEntity n) {
                        n.setNexus(getNexus());
                    }
                    if (child instanceof Mob l) {
                        l.setBaby(true);
                    }
                    offspring.add(child);
            }
        }
        return offspring;
    }
}
