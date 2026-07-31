package com.invasion.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
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
                .add(Attributes.MOVEMENT_SPEED, 0.22F)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.GRAVITY, 0.08);
    }

    @Override
    protected float getGlobalScaleMultiplier() {
        return isBaby() ? super.getGlobalScaleMultiplier() : 1.3F;
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
                .getTagOrEmpty(InvTags.Entities.QUEEN_SPIDER_OFFSPRING).spliterator(), false)
                .toList();
        for (int i = 0; i < offspringCount && !offspringTypes.isEmpty(); i++) {
            var type = offspringTypes.get(level().getRandom().nextInt(offspringTypes.size()));
            Entity child = type.value().create(level(), MobSpawnType.EVENT);
            if (child == null) {
                continue;
            }
            if (child instanceof NexusEntity n && hasNexus()) {
                n.setNexus(getNexus());
            }
            if (child instanceof Mob mob) {
                mob.setBaby(true);
            }
            offspring.add(child);
        }
        return offspring;
    }
}
