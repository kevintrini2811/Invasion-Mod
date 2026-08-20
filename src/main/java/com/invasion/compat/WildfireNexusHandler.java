package com.invasion.compat;

import java.util.Map;
import java.util.WeakHashMap;

import com.invasion.entity.BoundIMMobRegistry;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.NexusAccess;
import com.faboslav.friendsandfoes.common.entity.WildfireShieldDebrisEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

final class WildfireNexusHandler {
    private static final Map<Monster, WildfireNexusGoal> GOALS =
            new WeakHashMap<>();

    private WildfireNexusHandler() {
    }

    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof WildfireShieldDebrisEntity debris)
                || !(debris.getOwner() instanceof Combatant<?> combatant)
                || !FriendsAndFoesCompatibility.isAnyWildfire(
                        debris.getOwner().getType())
                || !(event.getRayTraceResult() instanceof BlockHitResult hit)) {
            return;
        }
        NexusAccess nexus = combatant.getNexus();
        if (nexus != null && hit.getBlockPos().equals(nexus.getOrigin())) {
            nexus.damage(debris.damageSources().fireball(
                    debris, debris.getOwner()), 5);
            event.setCanceled(true);
            debris.discard();
        }
    }

    static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) return;
        for (Combatant<?> combatant : BoundIMMobRegistry.activeBound(level)) {
            if (!(combatant.asEntity() instanceof Monster mob)
                    || mob.getType() != FriendsAndFoesIntegration.WILDFIRE) {
                continue;
            }
            synchronizeAttackTarget(mob);
            WildfireNexusGoal goal = GOALS.computeIfAbsent(
                    mob, ignored -> new WildfireNexusGoal(mob, combatant));
            if (goal.canUse()) {
                goal.tick();
            } else {
                goal.stop();
            }
        }
    }

    private static void synchronizeAttackTarget(Monster mob) {
        LivingEntity brainTarget = mob.getBrain().getMemory(
                MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (isValidAttackTarget(mob, brainTarget)) {
            mob.setTarget(brainTarget);
            return;
        }
        if (brainTarget != null) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        LivingEntity selectedTarget = mob.getTarget();
        if (isValidAttackTarget(mob, selectedTarget)) {
            mob.getBrain().setMemory(
                    MemoryModuleType.ATTACK_TARGET, selectedTarget);
        } else if (selectedTarget != null) {
            mob.setTarget(null);
        }
    }

    private static boolean isValidAttackTarget(
            Monster mob, LivingEntity target) {
        return target != null && target.isAlive() && !target.isRemoved()
                && mob.canAttack(target);
    }
}
