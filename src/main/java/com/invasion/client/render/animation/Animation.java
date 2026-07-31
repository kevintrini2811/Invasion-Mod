package com.invasion.client.render.animation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

public record Animation<T extends Enum<T>>(
        float duration,
        float speed,
        EnumMap<T, List<KeyFrame>> keyframes,
        List<Phase> phases
    ) {
    public static final Animation<WingBone> EMPTY = new Animation<>(1, 1,
            new EnumMap<>(WingBone.class), List.of(
                    new Phase(AnimationAction.STAND, 0, 1, new Transition(AnimationAction.STAND, 1, 0))
            ));

    public List<KeyFrame> getKeyFrames(T skeletonPart) {
        return keyframes.getOrDefault(skeletonPart, List.of());
    }

    public Animator<T> createAnimator(Map<T, ModelPart> parts) {
        return new Animator<>(parts, this);
    }

    public <E extends Entity, K extends AnimationController> K createState(E entity, AnimationAction initialAction, BiFunction<E, AnimationState<T>, K> controllerFactory) {
        return controllerFactory.apply(entity, new AnimationState<>(this).setNewAction(initialAction));
    }
}