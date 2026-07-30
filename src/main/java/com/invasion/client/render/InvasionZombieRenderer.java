package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.AbstractIMZombieEntity;
import com.invasion.mixin.AgeableMobRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class InvasionZombieRenderer<T extends AbstractIMZombieEntity>
        extends HumanoidMobRenderer<T, InvasionZombieRenderState, HumanoidModel<InvasionZombieRenderState>> {
    private static final List<Identifier> ZOMBIE_TEXTURES = List.of(
            texture("entity/zombie/old_zombie_t1.png"),
            texture("entity/zombie/zombie_t1.png"),
            texture("entity/zombie/zombie_t2.png"),
            texture("entity/zombie_pigman/zombie_pigman_t3.png"),
            texture("entity/zombie/zombie_t2a.png"),
            texture("entity/zombie/zombie_tar.png"),
            texture("entity/zombie/zombie_t3.png"));
    private static final List<Identifier> PIGMAN_TEXTURES = List.of(
            texture("entity/zombie_pigman/zombie_pigman.png"),
            texture("entity/zombie_pigman/zombie_pigman.png"),
            texture("entity/zombie_pigman/zombie_pigman_t3.png"));

    private final HumanoidModel<InvasionZombieRenderState> normalModel;
    private final HumanoidModel<InvasionZombieRenderState> bruteModel;
    private final boolean pigman;

    public InvasionZombieRenderer(EntityRendererProvider.Context context, boolean pigman) {
        super(
                context,
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                new BabyZombieModel<>(
                        context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
                0.5F);
        normalModel = model;
        bruteModel = new LargeZombieModel(LargeZombieModel.createBodyLayer().bakeRoot());
        this.pigman = pigman;

        ArmorModelSet<HumanoidModel<InvasionZombieRenderState>> normalArmor = ArmorModelSet.bake(
                ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new);
        ArmorModelSet<HumanoidModel<InvasionZombieRenderState>> babyArmor = ArmorModelSet.bake(
                ModelLayers.ZOMBIE_BABY_ARMOR,
                context.getModelSet(),
                HumanoidModel::new);
        addLayer(new VariantArmorLayer(
                this, normalArmor, babyArmor, context, false));
        addLayer(new HeadArmorLayer<>(
                this, context,
                state -> state.brute
                        ? state.headEquipment
                        : ItemStack.EMPTY,
                0.0F, 0.0F, 0.7F));
    }

    @Override
    public InvasionZombieRenderState createRenderState() {
        return new InvasionZombieRenderState();
    }

    @Override
    public void extractRenderState(T entity, InvasionZombieRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.textureId = entity.getTextureId();
        state.invasionScale = entity.scaleAmount();
        state.brute = pigman ? entity.getTier() == 3 : state.textureId == 3 || state.textureId == 6;
        state.isAggressive = entity.isAggressive();
    }

    @Override
    public void submit(InvasionZombieRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        // AgeableMobRenderer resets `model` from this field at the start of submit.
        // Select the legacy brute model at the source so its 10x5 torso is retained.
        ((AgeableMobRendererAccessor) (Object) this).invasion$setAdultModel(
                state.brute ? bruteModel : normalModel);
        super.submit(state, poseStack, collector, cameraState);
    }

    @Override
    protected void scale(InvasionZombieRenderState state, PoseStack poseStack) {
        float scale = state.invasionScale;
        poseStack.scale(scale, (2.0F + scale) / 3.0F, scale);
    }

    @Override
    public Identifier getTextureLocation(InvasionZombieRenderState state) {
        List<Identifier> textures = pigman ? PIGMAN_TEXTURES : ZOMBIE_TEXTURES;
        int index = state.textureId;
        return textures.get(index >= 0 && index < textures.size() ? index : 0);
    }

    private static Identifier texture(String path) {
        return InvasionMod.id("textures/" + path);
    }

    private final class VariantArmorLayer extends HumanoidArmorLayer<
            InvasionZombieRenderState,
            HumanoidModel<InvasionZombieRenderState>,
            HumanoidModel<InvasionZombieRenderState>> {
        private final boolean bruteLayer;

        VariantArmorLayer(
                InvasionZombieRenderer<T> parent,
                ArmorModelSet<HumanoidModel<InvasionZombieRenderState>> adultArmor,
                ArmorModelSet<HumanoidModel<InvasionZombieRenderState>> babyArmor,
                EntityRendererProvider.Context context,
                boolean bruteLayer) {
            super(
                    parent,
                    adultArmor,
                    babyArmor,
                    context.getEquipmentRenderer());
            this.bruteLayer = bruteLayer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                InvasionZombieRenderState state, float yRot, float xRot) {
            boolean usesBruteModel = state.brute && !state.isBaby;
            if (usesBruteModel == bruteLayer) {
                super.submit(poseStack, collector, light, state, yRot, xRot);
            }
        }
    }
}
