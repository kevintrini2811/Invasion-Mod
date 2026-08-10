package com.invasion.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.monster.skeleton.BoggedModel;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import java.util.function.Function;

/** Creates baby skeleton geometry without depending on TinySkeletons classes. */
final class IMBabySkeletonModels {
    private IMBabySkeletonModels() {
    }

    static <S extends SkeletonRenderState> SkeletonModel<S> skeleton() {
        return new SkeletonModel<>(babyRoot());
    }

    static IMWitherSkeletonModel witherSkeleton() {
        return new IMWitherSkeletonModel(
                SkeletonModel.createBodyLayer()
                        .apply(HumanoidModel.BABY_TRANSFORMER)
                        .apply(net.minecraft.client.model.geom.builders
                                .MeshTransformer.scaling(1.2F))
                        .bakeRoot());
    }

    static BoggedModel bogged() {
        return new BoggedModel(BoggedModel.createBodyLayer()
                .apply(HumanoidModel.BABY_TRANSFORMER)
                .bakeRoot());
    }

    static <S extends SkeletonRenderState> SkeletonModel<S> parched() {
        return new SkeletonModel<>(SkeletonModel.createSingleModelDualBodyLayer()
                .apply(HumanoidModel.BABY_TRANSFORMER)
                .bakeRoot());
    }

    static <S extends SkeletonRenderState> SkeletonModel<S> clothing(
            float deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(
                new CubeDeformation(deformation), 0.0F);
        return new SkeletonModel<>(LayerDefinition.create(
                HumanoidModel.BABY_TRANSFORMER.apply(mesh), 64, 32)
                .bakeRoot());
    }

    static <S extends SkeletonRenderState, M extends HumanoidModel<S>>
            ArmorModelSet<M> armor(
                    Function<ModelPart, M> factory, float scale) {
        return HumanoidModel.createArmorMeshSet(
                        LayerDefinitions.INNER_ARMOR_DEFORMATION,
                        LayerDefinitions.OUTER_ARMOR_DEFORMATION)
                .map(mesh -> {
                    LayerDefinition layer = LayerDefinition.create(
                                    HumanoidModel.BABY_TRANSFORMER.apply(mesh),
                                    64, 32);
                    if (scale != 1.0F) {
                        layer = layer.apply(
                                net.minecraft.client.model.geom.builders
                                        .MeshTransformer.scaling(scale));
                    }
                    return factory.apply(layer.bakeRoot());
                });
    }

    private static ModelPart babyRoot() {
        MeshDefinition mesh = HumanoidModel.createMesh(
                CubeDeformation.NONE, 0.0F);
        SkeletonMesh.addLimbs(mesh);
        return LayerDefinition.create(
                HumanoidModel.BABY_TRANSFORMER.apply(mesh), 64, 32)
                .bakeRoot();
    }

    private static final class SkeletonMesh<S extends SkeletonRenderState>
            extends SkeletonModel<S> {
        private SkeletonMesh(ModelPart root) {
            super(root);
        }

        private static void addLimbs(MeshDefinition mesh) {
            createDefaultSkeletonMesh(mesh.getRoot());
        }
    }
}
