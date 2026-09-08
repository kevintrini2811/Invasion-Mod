package com.invasion.compat;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;

/** Uses signatures, as the 1.20.1 runtime renames vanilla slime methods. */
final class LegacyNexusJumpControl {
    private LegacyNexusJumpControl() {}

    static NexusJumpControl of(Mob mob) {
        Object control = mob.getMoveControl();
        if (control instanceof NexusJumpControl jumpControl) return jumpControl;
        if (!Vanilla.CONTROL_CLASS.isInstance(control)) return null;
        return new NexusJumpControl() {
            @Override public void invasion$setDirection(float direction, boolean aggressive) {
                invoke(Vanilla.DIRECTION, control, direction, aggressive);
            }
            @Override public void invasion$setWantedMovement(double speed) {
                invoke(Vanilla.MOVEMENT, control, speed);
            }
        };
    }

    private static void invoke(Method method, Object control, Object... arguments) {
        try {
            method.invoke(control, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to steer legacy Nexus slime", exception);
        }
    }

    private static final class Vanilla {
        private static final Class<?> CONTROL_CLASS = Arrays.stream(Slime.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("SlimeMoveControl"))
                .findFirst().orElseThrow();
        private static final Method DIRECTION = method(float.class, boolean.class);
        private static final Method MOVEMENT = method(double.class);

        private static Method method(Class<?>... parameters) {
            for (Method method : CONTROL_CLASS.getDeclaredMethods()) {
                if (Arrays.equals(method.getParameterTypes(), parameters)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new IllegalStateException("Missing legacy slime movement method");
        }
    }
}
