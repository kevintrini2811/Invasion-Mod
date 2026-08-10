package com.invasion.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

/** Creates baby skeleton geometry without depending on TinySkeletons classes. */
final class IMBabySkeletonModels {
    private IMBabySkeletonModels() {
    }

    static <S extends SkeletonRenderState> SkeletonModel<S> skeleton() {
        return new SkeletonModel<>(babyRoot());
    }

    static IMWitherSkeletonModel witherSkeleton() {
        return new IMWitherSkeletonModel(babyRoot());
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
