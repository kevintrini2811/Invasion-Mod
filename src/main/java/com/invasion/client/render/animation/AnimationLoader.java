package com.invasion.client.render.animation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.invasion.InvasionMod;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class AnimationLoader implements SimpleSynchronousResourceReloadListener {
    private static final Identifier ID = InvasionMod.id("animations");
    public static final AnimationLoader INSTANCE = new AnimationLoader();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        AnimationRegistry.instance().clear();
        AnimationRegistry.instance().register("bird_wing_flap", createBirdWingFlap());
        AnimationRegistry.instance().register("bird_run", createBirdRun());
        AnimationRegistry.instance().register("wing_flap_2_piece", createWingFlap2Piece());
        AnimationRegistry.instance().register("bird_beak", createBirdBeak());
    }

    static Animation<WingBone> createBirdWingFlap() {
        float frameUnit = 0.01666667F;
        List<KeyFrame> innerWingFrames = List.of(
                new KeyFrame(0, 2, -43.5F, 0, InterpType.LINEAR),
                new KeyFrame(5 * frameUnit, 4, -38, 0, InterpType.LINEAR),
                new KeyFrame(10 * frameUnit, 5.5F, -27.5F, 0, InterpType.LINEAR),
                new KeyFrame(15 * frameUnit, 5.5F, -7, 0, InterpType.LINEAR),
                new KeyFrame(20 * frameUnit, 5.5F, 15, 0, InterpType.LINEAR),
                new KeyFrame(25 * frameUnit, 4.5F, 30, 0, InterpType.LINEAR),
                new KeyFrame(30 * frameUnit, 2, 38, 9, InterpType.LINEAR),
                new KeyFrame(35 * frameUnit, 1, 20, 0, InterpType.LINEAR),
                new KeyFrame(40 * frameUnit, 1, 3.5F, 0, InterpType.LINEAR),
                new KeyFrame(45 * frameUnit, 1, -19, 0, InterpType.LINEAR),
                new KeyFrame(50 * frameUnit, -3, -38, 0, InterpType.LINEAR),
                new KeyFrame(55 * frameUnit, -1, -48, 0, InterpType.LINEAR)
        );
        List<KeyFrame> outerWingFrames = List.of(
                new KeyFrame(0, 2, 34.5F, 0, InterpType.LINEAR),
                new KeyFrame(5 * frameUnit, 5, 13, -7, InterpType.LINEAR),
                new KeyFrame(10 * frameUnit, 7, 8.5F, -10, InterpType.LINEAR),
                new KeyFrame(15 * frameUnit, 7.5F, -2.5F, -10, InterpType.LINEAR),
                new KeyFrame(25 * frameUnit, 5, 7, -10, InterpType.LINEAR),
                new KeyFrame(30 * frameUnit, 2, 15, 0, InterpType.LINEAR),
                new KeyFrame(35 * frameUnit, -3, 37, 12, InterpType.LINEAR),
                new KeyFrame(40 * frameUnit, -9, 56, 27, InterpType.LINEAR),
                new KeyFrame(45 * frameUnit, -13, 68, 28, InterpType.LINEAR),
                new KeyFrame(50 * frameUnit, -13.5F, 70, 31.5F, InterpType.LINEAR),
                new KeyFrame(53 * frameUnit, -9, 71, 31, InterpType.LINEAR),
                new KeyFrame(55 * frameUnit, -3.5F, 65.5F, 22, InterpType.LINEAR),
                new KeyFrame(58 * frameUnit, 0, 52, 8, InterpType.LINEAR));
        return new Animation<>(1, 0.04651163F, new EnumMap<>(Map.of(
                WingBone.RIGHT_SHOULDER, innerWingFrames,
                WingBone.LEFT_SHOULDER, KeyFrame.mirrorFramesX(innerWingFrames),
                WingBone.RIGHT_ELBOW, outerWingFrames,
                WingBone.LEFT_ELBOW, KeyFrame.mirrorFramesX(outerWingFrames)
        )), List.of());
    }

    static Animation<BirdLegBone> createBirdRun() {
        int x = 17;
        float totalFrames = 331 + x;
        float frameUnit = 1 / totalFrames;
        float runBegin = 38 * frameUnit;
        float runEnd = (170 + x) * frameUnit;

        List<KeyFrame> leftThighRunCycle = List.of(
                new KeyFrame(38 * frameUnit, -74.099998F, 0, -6.5F, InterpType.LINEAR),
                new KeyFrame(44 * frameUnit, -63.700001F, 0, -6.5F, InterpType.LINEAR),
                new KeyFrame((80 + x) * frameUnit, 13.1F, 0, -6.5F, InterpType.LINEAR),
                new KeyFrame((101 + x) * frameUnit, 35.700001F, 0, -6.5F, InterpType.LINEAR),
                new KeyFrame((110 + x) * frameUnit, 20, 0, -6.5F, InterpType.LINEAR),
                new KeyFrame((140 + x) * frameUnit, -33, 0, -6.5F, InterpType.LINEAR),
                new KeyFrame((170 + x) * frameUnit, -74.099998F, 0, -6.5F, InterpType.LINEAR),
                new KeyFrame((171 + x) * frameUnit, -76, 0, -5.6F, InterpType.LINEAR),
                new KeyFrame((211 + x) * frameUnit, -15, 0, -5, InterpType.LINEAR),
                new KeyFrame((251 + x) * frameUnit, 9, 0, 0, InterpType.LINEAR),
                new KeyFrame((291 + x) * frameUnit, -15, 0, -5, InterpType.LINEAR),
                new KeyFrame((331 + x) * frameUnit, -76, 0, -5.6F, InterpType.LINEAR));

        List<KeyFrame> leftLegRunCycle = List.of(
                new KeyFrame(38 * frameUnit, 6.6F, 0, 0, InterpType.LINEAR),
                new KeyFrame(44 * frameUnit, 6.5F, 0, 0, InterpType.LINEAR),
                new KeyFrame(47 * frameUnit, -11, 0, 0, InterpType.LINEAR),
                new KeyFrame(50 * frameUnit, -24, 0, 0, InterpType.LINEAR),
                new KeyFrame(53 * frameUnit, -32.900002F, 0, 0, InterpType.LINEAR),
                new KeyFrame(56 * frameUnit, -40.799999F, 0, 0, InterpType.LINEAR),
                new KeyFrame(59 * frameUnit, -46.700001F, 0, 0, InterpType.LINEAR),
                new KeyFrame(62 * frameUnit, -45.799999F, 0, 0, InterpType.LINEAR),
                new KeyFrame(82 * frameUnit, -45.599998F, 0, 0, InterpType.LINEAR),
                new KeyFrame(97 * frameUnit, -17.1F, 0, 0, InterpType.LINEAR),
                new KeyFrame((85 + x) * frameUnit, 0.75F, 0, 0, InterpType.LINEAR),
                new KeyFrame((90 + x) * frameUnit, -0.4F, 0, 0, InterpType.LINEAR),
                new KeyFrame((101 + x) * frameUnit, -43, 0, 0, InterpType.LINEAR),
                new KeyFrame((115 + x) * frameUnit, -60.099998F, 0, 0, InterpType.LINEAR),
                new KeyFrame((154 + x) * frameUnit, -50.5F, 0, 0, InterpType.LINEAR),
                new KeyFrame((170 + x) * frameUnit, 6.6F, 0, 0, InterpType.LINEAR),
                new KeyFrame((171 + x) * frameUnit, -37, 0, 0, InterpType.LINEAR),
                new KeyFrame((211 + x) * frameUnit, -41, 0, 0, InterpType.LINEAR),
                new KeyFrame((251 + x) * frameUnit, 15, 0, 0, InterpType.LINEAR),
                new KeyFrame((291 + x) * frameUnit, -41, 0, 0, InterpType.LINEAR),
                new KeyFrame((331 + x) * frameUnit, -37, 0, 0, InterpType.LINEAR));

        List<KeyFrame> leftAnkleRunCycle = List.of(
                new KeyFrame(38 * frameUnit, 28.799999F, -5, 0, InterpType.LINEAR),
                new KeyFrame(44 * frameUnit, -0.4F, -5, 0, InterpType.LINEAR),
                new KeyFrame(47 * frameUnit, 7.6F, -5, 0, InterpType.LINEAR),
                new KeyFrame(50 * frameUnit, 12.4F, -5, 0, InterpType.LINEAR),
                new KeyFrame(53 * frameUnit, 12.6F, -5, 0, InterpType.LINEAR),
                new KeyFrame(56 * frameUnit, 11.8F, -5, 0, InterpType.LINEAR),
                new KeyFrame(59 * frameUnit, 8.5F, -5, 0, InterpType.LINEAR),
                new KeyFrame(62 * frameUnit, 1.6F, -5, 0, InterpType.LINEAR),
                new KeyFrame(82 * frameUnit, -1, -5, 0, InterpType.LINEAR),
                new KeyFrame(87 * frameUnit, -5.5F, -5, 0, InterpType.LINEAR),
                new KeyFrame(90 * frameUnit, -0.7F, -5, 0, InterpType.LINEAR),
                new KeyFrame(93 * frameUnit, 6.8F, -5, 0, InterpType.LINEAR),
                new KeyFrame(97 * frameUnit, -4.6F, -5, 0, InterpType.LINEAR),
                new KeyFrame((85 + x) * frameUnit, 20.700001F, -5, 0, InterpType.LINEAR),
                new KeyFrame((95 + x) * frameUnit, 34.200001F, -5, 0, InterpType.LINEAR),
                new KeyFrame((100 + x) * frameUnit, 45.599998F, -5, 0, InterpType.LINEAR),
                new KeyFrame((110 + x) * frameUnit, 36.599998F, -5, 0, InterpType.LINEAR),
                new KeyFrame((115 + x) * frameUnit, 38.400002F, -5, 0, InterpType.LINEAR),
                new KeyFrame((124 + x) * frameUnit, 50, -5, 0, InterpType.LINEAR),
                new KeyFrame((140 + x) * frameUnit, 45.299999F, -5, 0, InterpType.LINEAR),
                new KeyFrame((154 + x) * frameUnit, 52.900002F, -5, 0, InterpType.LINEAR),
                new KeyFrame((170 + x) * frameUnit, 25, -5, 0, InterpType.LINEAR),
                new KeyFrame((171 + x) * frameUnit, -38, -5, 0, InterpType.LINEAR),
                new KeyFrame((211 + x) * frameUnit, 0, -5, 0, InterpType.LINEAR),
                new KeyFrame((251 + x) * frameUnit, 22, -5, 0, InterpType.LINEAR),
                new KeyFrame((291 + x) * frameUnit, 0, -5, 0, InterpType.LINEAR),
                new KeyFrame((331 + x) * frameUnit, -38, -5, 0, InterpType.LINEAR));

        List<KeyFrame> leftBackClawFrames = List.of(
                new KeyFrame(0, 77, 0, 0, InterpType.LINEAR),
                new KeyFrame((170 + x) * frameUnit, 77, 0, 0, InterpType.LINEAR),
                new KeyFrame((171 + x) * frameUnit, 84, 0, 0, InterpType.LINEAR),
                new KeyFrame((211 + x) * frameUnit, 77, 0, 0, InterpType.LINEAR),
                new KeyFrame((251 + x) * frameUnit, -7.5F, 0, 0, InterpType.LINEAR),
                new KeyFrame((291 + x) * frameUnit, 77, 0, 0, InterpType.LINEAR),
                new KeyFrame((331 + x) * frameUnit, 84, 0, 0, InterpType.LINEAR));
        return new Animation<>(1, 0.04651163F, new EnumMap<>(Map.of(
                BirdLegBone.LEFT_KNEE, Stream.concat(Stream.of(
                        new KeyFrame(0, -15, 0, -5, InterpType.LINEAR),
                        new KeyFrame(1 * frameUnit, -15, 0, -5, InterpType.LINEAR),
                        new KeyFrame(5 * frameUnit, -12.6F, 0.2F, 5, InterpType.LINEAR),
                        new KeyFrame(10 * frameUnit, 21.200001F, -0.6F, 5.2F, InterpType.LINEAR),
                        new KeyFrame(15 * frameUnit, -32, -1.7F, 5.7F, InterpType.LINEAR),
                        new KeyFrame(25 * frameUnit, -57, -6.4F, 9, InterpType.LINEAR),
                        new KeyFrame(35 * frameUnit, -76.5F, -19.299999F, 21.200001F, InterpType.LINEAR)),
                        leftThighRunCycle.stream()).toList(),
                BirdLegBone.RIGHT_KNEE, Stream.concat(Stream.of(
                        new KeyFrame(0, -15, 0, 0, InterpType.LINEAR),
                        new KeyFrame(1 * frameUnit, -15, 0, 0, InterpType.LINEAR),
                        new KeyFrame(37 * frameUnit, -15, 0, 0, InterpType.LINEAR)),
                        KeyFrame.offsetFramesCircular(KeyFrame.mirrorFramesX(leftThighRunCycle), runBegin, runEnd, (runEnd - runBegin) / 2).stream()).toList(),
                BirdLegBone.LEFT_ANKLE, Stream.concat(Stream.of(
                        new KeyFrame(0, -41, 0, 0, InterpType.LINEAR),
                        new KeyFrame(1 * frameUnit, -41, 0, 0, InterpType.LINEAR),
                        new KeyFrame(10 * frameUnit, -80.300003F, 0, 0, InterpType.LINEAR),
                        new KeyFrame(25 * frameUnit, -44.200001F, 0, 0, InterpType.LINEAR),
                        new KeyFrame(35 * frameUnit, -5.6F, 0, 0, InterpType.LINEAR)),
                        leftLegRunCycle.stream()).toList(),
                BirdLegBone.RIGHT_ANKLE, Stream.concat(Stream.of(
                        new KeyFrame(0, -41, 0, 0, InterpType.LINEAR),
                        new KeyFrame(37 * frameUnit, -41, 0, 0, InterpType.LINEAR)),
                        KeyFrame.offsetFramesCircular(KeyFrame.mirrorFramesX(leftLegRunCycle), runBegin, runEnd, (runEnd - runBegin) / 2).stream()).toList(),
                BirdLegBone.LEFT_METATARSOPHALANGEAL_ARTICULATIONS, Stream.concat(Stream.of(
                        new KeyFrame(0, -0.4F, -5, 0, InterpType.LINEAR),
                        new KeyFrame(1 * frameUnit, -0.4F, -5, 0, InterpType.LINEAR),
                        new KeyFrame(5 * frameUnit, 31.700001F, -5, 0, InterpType.LINEAR),
                        new KeyFrame(10 * frameUnit, 45, -5, 0, InterpType.LINEAR),
                        new KeyFrame(20 * frameUnit, 52.799999F, -5, 0, InterpType.LINEAR),
                        new KeyFrame(25 * frameUnit, 51.599998F, -5, 0, InterpType.LINEAR),
                        new KeyFrame(30 * frameUnit, 42.299999F, -5, 0, InterpType.LINEAR)),
                        leftAnkleRunCycle.stream()).toList(),
                BirdLegBone.RIGHT_METATARSOPHALANGEAL_ARTICULATIONS, Stream.concat(Stream.of(
                        new KeyFrame(0, -0.4F, -5, 0, InterpType.LINEAR),
                        new KeyFrame(1 * frameUnit, -0.4F, -5, 0, InterpType.LINEAR),
                        new KeyFrame(37 * frameUnit, -0.4F, -5, 0, InterpType.LINEAR)),
                        KeyFrame.offsetFramesCircular(KeyFrame.mirrorFramesX(leftAnkleRunCycle), runBegin, runEnd, (runEnd - runBegin) / 2).stream()).toList(),
                BirdLegBone.LEFT_BACK_CLAW, leftBackClawFrames,
                BirdLegBone.RIGHT_BACK_CLAW, KeyFrame.mirrorFramesX(leftBackClawFrames)
        )), List.of(
                new Phase.Builder(AnimationAction.STAND, 0, 1 / totalFrames)
                        .defaultTransition(AnimationAction.STAND, 1 / totalFrames, 0)
                        .transition(AnimationAction.STAND_TO_RUN, 1 / totalFrames, 1 / totalFrames)
                        .transition(AnimationAction.LEGS_RETRACT, 1 / totalFrames, (211 + x) / totalFrames)
                        .transition(AnimationAction.LEGS_CLAW_ATTACK_P1, 1 / totalFrames, (171 + x) / totalFrames)
                        .build(),
                new Phase.Builder(AnimationAction.STAND_TO_RUN, 1 / totalFrames, 38 / totalFrames)
                        .defaultTransition(AnimationAction.RUN, 38 / totalFrames, 38 / totalFrames)
                        .build(),
                new Phase.Builder(AnimationAction.RUN, 38 / totalFrames, (170 + x) / totalFrames)
                        .defaultTransition(AnimationAction.RUN, (170 + x) / totalFrames, 38 / totalFrames)
                        .transition(AnimationAction.STAND, (170 + x) / totalFrames, 0)
                        .build(),
                new Phase.Builder(AnimationAction.LEGS_RETRACT, (211 + x) / totalFrames, (251 + x) / totalFrames)
                        .defaultTransition(AnimationAction.LEGS_UNRETRACT, (251 + x) / totalFrames, (251 + x) / totalFrames)
                        .build(),
                new Phase.Builder(AnimationAction.LEGS_UNRETRACT, (251 + x) / totalFrames, (291 + x) / totalFrames)
                        .defaultTransition(AnimationAction.STAND, (291 + x) / totalFrames, 0)
                        .transition(AnimationAction.LEGS_RETRACT, (291 + x) / totalFrames, (211 + x) / totalFrames)
                        .transition(AnimationAction.LEGS_CLAW_ATTACK_P1, (291 + x) / totalFrames, (291 + x) / totalFrames)
                        .build(),
                new Phase.Builder(AnimationAction.LEGS_CLAW_ATTACK_P1, (291 + x) / totalFrames, (331 + x) / totalFrames)
                        .defaultTransition(AnimationAction.LEGS_CLAW_ATTACK_P2, (331 + x) / totalFrames, (171 + x) / totalFrames)
                        .build(),
                new Phase.Builder(AnimationAction.LEGS_CLAW_ATTACK_P2, (171 + x) / totalFrames, (211 + x) / totalFrames)
                        .defaultTransition(AnimationAction.STAND, (211 + x) / totalFrames, 0)
                        .transition(AnimationAction.LEGS_RETRACT, (211 + x) / totalFrames, (211 + x) / totalFrames)
                        .transition(AnimationAction.LEGS_CLAW_ATTACK_P1, (211 + x) / totalFrames, (291 + x) / totalFrames)
                        .build()
                ));
    }

    static Animation<WingBone> createWingFlap2Piece() {
        float frameUnit = 0.004524887F;
        List<KeyFrame> rightInnerWingFrames = List.of(
                new KeyFrame(0, 2, -48, 0, 7, -8, 6, InterpType.LINEAR),
                new KeyFrame(5 * frameUnit, 4, -38, 0, InterpType.LINEAR),
                new KeyFrame(10 * frameUnit, 5.5F, -27.5F, 0, InterpType.LINEAR),
                new KeyFrame(15 * frameUnit, 5.5F, -7, 0, InterpType.LINEAR),
                new KeyFrame(20 * frameUnit, 5.5F, 15, 0, InterpType.LINEAR),
                new KeyFrame(25 * frameUnit, 4.5F, 30, 0, InterpType.LINEAR),
                new KeyFrame(30 * frameUnit, 2, 38, 9, InterpType.LINEAR),
                new KeyFrame(35 * frameUnit, 1, 20, 0, InterpType.LINEAR),
                new KeyFrame(40 * frameUnit, 1, 3.5F, 0, InterpType.LINEAR),
                new KeyFrame(45 * frameUnit, 1, -19, 0, InterpType.LINEAR),
                new KeyFrame(50 * frameUnit, -3, -38, 0, InterpType.LINEAR),
                new KeyFrame(55 * frameUnit, -1, -48, 0, InterpType.LINEAR),
                new KeyFrame(60 * frameUnit, 2, -48, 0, InterpType.LINEAR),
                new KeyFrame(61 * frameUnit, 5.5F, -7, 0, 7, -8, 6, InterpType.LINEAR),
                new KeyFrame(121 * frameUnit, 0.71F, 88.599998F, 0, 11, -8, 9, InterpType.LINEAR),
                new KeyFrame(181 * frameUnit, 5.5F, -7, 0, 7, -8, 6, InterpType.LINEAR),
                new KeyFrame(209 * frameUnit, 5.5F, -5, 0, InterpType.LINEAR),
                new KeyFrame(221 * frameUnit, 5.5F, -7, 0, InterpType.LINEAR));
        List<KeyFrame> rightOuterWingFrames = List.of(
                new KeyFrame(0, 2, 34.5F, 0, 23, 1, 0, InterpType.LINEAR),
                new KeyFrame(5 * frameUnit, 5, 13, -7, InterpType.LINEAR),
                new KeyFrame(10 * frameUnit, 7, 8.5F, -10, InterpType.LINEAR),
                new KeyFrame(15 * frameUnit, 7.5F, -2.5F, -10, InterpType.LINEAR),
                new KeyFrame(25 * frameUnit, 5, 7, -10, InterpType.LINEAR),
                new KeyFrame(30 * frameUnit, 2, 15, 0, InterpType.LINEAR),
                new KeyFrame(35 * frameUnit, -3, 37, 12, InterpType.LINEAR),
                new KeyFrame(40 * frameUnit, -9, 56, 27, InterpType.LINEAR),
                new KeyFrame(45 * frameUnit, -13, 68, 28, InterpType.LINEAR),
                new KeyFrame(50 * frameUnit, -13.5F, 70, 31.5F, InterpType.LINEAR),
                new KeyFrame(53 * frameUnit, -9, 71, 31, InterpType.LINEAR),
                new KeyFrame(55 * frameUnit, -3.5F, 65.5F, 22, InterpType.LINEAR),
                new KeyFrame(58 * frameUnit, 0, 52, 8, InterpType.LINEAR),
                new KeyFrame(60 * frameUnit, 2, 34.5F, 0, InterpType.LINEAR),
                new KeyFrame(61 * frameUnit, -5, -2.5F, -10, 23, 1, 0, InterpType.LINEAR),
                new KeyFrame(76 * frameUnit, 0, 0, 15, 22, 1, 0, InterpType.LINEAR),
                new KeyFrame(101 * frameUnit, 0, 0, 83, 20.33F, 1, 0, InterpType.LINEAR),
                new KeyFrame(121 * frameUnit, 0, 0, 90, 19, 1, 0, InterpType.LINEAR),
                new KeyFrame(141 * frameUnit, 0, 0, 83, 20.33F, 1, 0, InterpType.LINEAR),
                new KeyFrame(166 * frameUnit, 0, 0, 15, 22, 1, 0, InterpType.LINEAR),
                new KeyFrame(181 * frameUnit, -5, -2.5F, -10, 23, 1, 0, InterpType.LINEAR),
                new KeyFrame(209 * frameUnit, -5, -1.3F, -10, InterpType.LINEAR),
                new KeyFrame(221 * frameUnit, -5, -2.5F, -10, InterpType.LINEAR));

        return new Animation<>(1, 0.01666667F, new EnumMap<>(Map.of(
                WingBone.LEFT_SHOULDER, rightInnerWingFrames,
                WingBone.RIGHT_SHOULDER, KeyFrame.mirrorFramesX(rightInnerWingFrames),
                WingBone.LEFT_ELBOW, rightOuterWingFrames,
                WingBone.RIGHT_ELBOW, KeyFrame.mirrorFramesX(rightOuterWingFrames))), List.of(
                        new Phase.Builder(AnimationAction.WINGFLAP, 0, 0.2714932F)
                            .defaultTransition(AnimationAction.WINGFLAP, 0.2714932F, 0)
                            .transition(AnimationAction.WINGTUCK, 0.06787331F, 0.2760181F)
                            .transition(AnimationAction.WINGGLIDE, 0.06787331F, 0.8190045F)
                            .build(),
                        new Phase.Builder(AnimationAction.WINGTUCK, 0.2760181F, 0.5429865F)
                            .defaultTransition(AnimationAction.WINGSPREAD, 0.5429865F, 0.5475113F)
                            .build(),
                        new Phase.Builder(AnimationAction.WINGSPREAD, 0.5475113F, 0.8190045F)
                            .defaultTransition(AnimationAction.WINGTUCK, 0.8190045F, 0.2760181F)
                            .transition(AnimationAction.WINGFLAP, 0.8190045F, 0.06787331F)
                            .transition(AnimationAction.WINGGLIDE, 0.8190045F, 0.8190045F)
                            .build(),
                        new Phase.Builder(AnimationAction.WINGGLIDE, 0.8190045F, 1)
                            .defaultTransition(AnimationAction.WINGGLIDE, 1, 0.8190045F)
                            .transition(AnimationAction.WINGFLAP, 1, 0.06787331F)
                            .transition(AnimationAction.WINGTUCK, 1, 0.2760181F)
                            .build()));
    }

    static Animation<MouthBones> createBirdBeak() {
        float _frameUnit = 0.008333334F;
        return new Animation<>(1, 0.1F, new EnumMap<>(Map.of(
                MouthBones.UPPER_MOUTH, List.of(
                        new KeyFrame(0 * _frameUnit, 0, 0, 0, InterpType.LINEAR),
                        new KeyFrame(60 * _frameUnit, -8, 0, 0, InterpType.LINEAR),
                        new KeyFrame(120 * _frameUnit, 0, 0, 0, InterpType.LINEAR)),
                MouthBones.LOWER_MOUTH, List.of(
                        new KeyFrame(0 * _frameUnit, 0, 0, 0, InterpType.LINEAR),
                        new KeyFrame(60 * _frameUnit, 20, 0, 0, InterpType.LINEAR),
                        new KeyFrame(120 * _frameUnit, 0, 0, 0, InterpType.LINEAR)))), List.of(
                new Phase.Builder(AnimationAction.MOUTH_OPEN, 0, 0.5F).defaultTransition(AnimationAction.MOUTH_CLOSE, 0.5F, 0.5083333F).build(),
                new Phase.Builder(AnimationAction.MOUTH_CLOSE, 0.5F, 1).defaultTransition(AnimationAction.MOUTH_OPEN, 1, 0).build())
        );
    }
}
