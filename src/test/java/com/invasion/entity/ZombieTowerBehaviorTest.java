package com.invasion.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZombieTowerBehaviorTest {
    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @ParameterizedTest
    @ValueSource(classes = {ZombieBuilderEntity.class, ZombieMinerEntity.class})
    void installsConfiguredControllerAndDisablesNativeTowers(Class<? extends ZombieBuilderEntity> type) throws Exception {
        ZombieBuilderEntity mob = mock(type);
        when(mob.getNavigation()).thenReturn(mock(PathNavigation.class));
        when(mob.usesConfiguredEngineerTower()).thenCallRealMethod();
        for (String name : new String[] {"goalSelector", "targetSelector"}) {
            var field = Mob.class.getDeclaredField(name);
            field.setAccessible(true);
            GoalSelector selector = new GoalSelector();
            field.set(mob, selector);
            if (name.equals("goalSelector")) {
                when(((com.invasion.mixin.MobAccessor) mob).invmod$getGoalSelector()).thenReturn(selector);
            } else {
                when(((com.invasion.mixin.MobAccessor) mob).invmod$getTargetSelector()).thenReturn(selector);
            }
        }
        doCallRealMethod().when(mob).registerGoals();
        doCallRealMethod().when(mob).tryStartTowerBuild();
        doCallRealMethod().when(mob).tryReuseExistingTower();
        mob.registerGoals();
        assertTrue(mob.usesConfiguredEngineerTower());
        assertFalse(mob.tryStartTowerBuild());
        assertFalse(mob.tryReuseExistingTower());
        var goalField = Mob.class.getDeclaredField("goalSelector");
        goalField.setAccessible(true);
        var goals = ((GoalSelector) goalField.get(mob)).getAvailableGoals();
        assertEquals(1, goals.stream().filter(wrapped -> wrapped.getPriority() == 0
                && wrapped.getGoal().getClass().getSimpleName().equals("ConfiguredTerrainGoal")).count());
        assertEquals(1, goals.stream().filter(wrapped -> wrapped.getPriority() == 0
                && wrapped.getGoal().getClass().getSimpleName().equals("ClimbNexusLadderGoal")).count());
        assertTrue(goals.stream().filter(wrapped -> wrapped.getGoal() instanceof
                com.invasion.entity.ai.goal.MineBlockGoal).allMatch(wrapped -> wrapped.getPriority() > 0));
    }
}
