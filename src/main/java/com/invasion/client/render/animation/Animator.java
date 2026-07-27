package com.invasion.client.render.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Animator<T extends Enum<T>> {
    private final List<Part> parts;
    private final float animationPeriod;

    private static final Vector3f TEMP_VECTOR = new Vector3f();

    public Animator() {
        this(1);
    }

    public Animator(float animationPeriod) {
        this(animationPeriod, new ArrayList<>());
    }

    public Animator(float animationPeriod, List<Part> parts) {
        this.animationPeriod = animationPeriod;
        this.parts = parts;
    }

    public Animator(Map<T, ModelPart> modelParts, Animation<T> animation) {
        this(animation.duration(), modelParts.entrySet().stream().flatMap(entry -> {
            List<KeyFrame> keyFrames = animation.getKeyFrames(entry.getKey());
            if (!keyFrames.isEmpty()) {
                return Stream.of(new Part(entry.getValue(), keyFrames));
            }
            return Stream.empty();
        }).collect(Collectors.toList()));
    }

    public void updateAnimation(float newTime) {
        parts.forEach(part -> part.updateAnimation(newTime, animationPeriod));
    }

    private static class Part {
        private final ModelPart part;
        private int currentIndex;
        private final List<KeyFrame> keyFrames;

        private KeyFrame prevFrame;

        public Part(ModelPart part, List<KeyFrame> keyFrames) {
            this.part = part;
            this.keyFrames = keyFrames;
            this.prevFrame = keyFrames.get(0);
        }

        public void updateAnimation(float newTime, float period) {
            currentIndex++;
            currentIndex = findFrameIndex(prevFrame.time() <= newTime ? currentIndex : 0, newTime, currentIndex);
            KeyFrame nextFrame = keyFrames.get(currentIndex % keyFrames.size());
            prevFrame = keyFrames.get((currentIndex + keyFrames.size() - 1) % keyFrames.size());

            interpolate(nextFrame, newTime, period);
        }

        private int findFrameIndex(int startingPoint, float newTime, int fallbackIndex) {
            for (; startingPoint < keyFrames.size(); startingPoint++) {
                KeyFrame keyFrame = keyFrames.get(startingPoint);
                if (keyFrame.time() >= newTime) {
                    return startingPoint - 1;
                }
            }

            return fallbackIndex;
        }

        private void interpolate(KeyFrame nextFrame, float time, float period) {
            if (prevFrame.interpType() != InterpType.LINEAR) {
                return;
            }

            float dtPrev = time - prevFrame.time();
            float dtFrame = nextFrame.time() - prevFrame.time();
            if (dtFrame < 0) {
                dtFrame += period;
            }

            float r = dtPrev / dtFrame;

            Vector3fc rotation = KeyFrame.lerp(r, prevFrame.rotation(), nextFrame.rotation(), TEMP_VECTOR);
            part.xRot = rotation.x();
            part.yRot = rotation.y();
            part.zRot = rotation.z();

            if (prevFrame.hasPos()) {
                Vector3fc pivot = nextFrame.hasPos()
                        ? KeyFrame.lerp(r, prevFrame.pivot(), nextFrame.pivot(), TEMP_VECTOR)
                        : prevFrame.pivot();
                part.x = pivot.x();
                part.y = pivot.y();
                part.z = pivot.z();
            }
        }
    }
}