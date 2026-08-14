package com.invasion.entity.mutant;

import org.jetbrains.annotations.Nullable;

import com.invasion.entity.ai.goal.MutantAttackNexusGoal;
import com.invasion.entity.ai.goal.MutantGoToNexusGoal;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.IHasNexus;
import com.invasion.nexus.NexusAccess;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;

public interface IMMutantMob
        extends Combatant<PathfinderMob>, EntityConstruct.BuildableMob {
    @Override
    default PathfinderMob asEntity() {
        return (PathfinderMob) this;
    }

    @Override
    default String getLegacyName() {
        return getClass().getSimpleName();
    }

    @Override
    default void onSpawned(
            @Nullable NexusAccess nexus, EntityConstruct spawnConditions) {
        setNexus(nexus);
        if (nexus != null) {
            PathfinderMob mob = asEntity();
            double doubledHealth = mob.getMaxHealth() * 2.0D;
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(doubledHealth);
            mob.setHealth((float) doubledHealth);
        }
    }

    /** Runs the Mutant Monsters attack against a marker at the Nexus. */
    default int performNexusAttack(ServerLevel level, NexusAccess nexus) {
        PathfinderMob mob = asEntity();
        Entity marker = EntityTypes.MARKER.create(level, EntitySpawnReason.EVENT);
        if (marker != null) {
            marker.snapTo(
                    nexus.getOrigin().getX() + 0.5D,
                    nexus.getOrigin().getY() + 0.5D,
                    nexus.getOrigin().getZ() + 0.5D,
                    mob.getYRot(), mob.getXRot());
            mob.doHurtTarget(level, marker);
        }
        int damage = Math.max(2,
                (int) Math.ceil(mob.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        nexus.damage(mob.damageSources().mobAttack(mob), damage);
        return 40;
    }

    default void addNexusGoals() {
        PathfinderMob mob = asEntity();
        mob.goalSelector.addGoal(4, new MutantGoToNexusGoal(mob, this));
        mob.goalSelector.addGoal(3, new MutantAttackNexusGoal(mob, this));
    }

    IHasNexus.Handle getNexusHandle();
}
