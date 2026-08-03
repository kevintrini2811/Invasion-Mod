package com.invasion.entity.ai.goal;

import com.invasion.entity.NexusEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** Uses Minecraft's maintained ranged combat scheduler with Nexus mobs. */
public final class EntityAIKillWithArrow<T extends LivingEntity>
        extends RangedAttackGoal {
    public <E extends PathfinderMob & NexusEntity & RangedAttackMob>
            EntityAIKillWithArrow(E entity, Class<? extends T> targetClass,
                    int attackDelay, float attackRange) {
        super(entity, 1.0D, attackDelay, attackRange);
    }
}
