package com.invasion.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.invasion.entity.EntityIMZombie;
import java.lang.reflect.Method;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

class InfernalMobsHealthMixinTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void convertedAndWaveZombiesUseTheirOwnHealthRegardlessOfSpawnOrder()
            throws ReflectiveOperationException {
        // All instances share the class that Infernal Mobs caches as 6 HP.
        for (float health : new float[] {6, 20, 30, 65, 6, 20}) {
            EntityIMZombie zombie = mock(EntityIMZombie.class);
            when(zombie.getMaxHealth()).thenReturn(health);
            CallbackInfoReturnable<Double> callback = lookup(zombie);
            assertTrue(callback.isCancelled());
            assertEquals((double) health, callback.getReturnValue());
        }
    }

    @Test
    void vanillaMobsKeepInfernalMobsHealthConfiguration()
            throws ReflectiveOperationException {
        assertFalse(lookup(mock(Zombie.class)).isCancelled());
    }

    private CallbackInfoReturnable<Double> lookup(LivingEntity entity)
            throws ReflectiveOperationException {
        InfernalMobsHealthMixin mixin = new InfernalMobsHealthMixin() {};
        Method method = InfernalMobsHealthMixin.class.getDeclaredMethod(
                "invmod$useIndividualHealth", LivingEntity.class,
                CallbackInfoReturnable.class);
        method.setAccessible(true);
        CallbackInfoReturnable<Double> callback =
                new CallbackInfoReturnable<>("getMobClassMaxHealth", true);
        method.invoke(mixin, entity, callback);
        return callback;
    }
}
