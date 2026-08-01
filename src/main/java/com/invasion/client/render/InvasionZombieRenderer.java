package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.AbstractIMZombieEntity;
import com.invasion.mixin.AgeableMobRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
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
    private final HumanoidModel<InvasionZombieRenderState> normalBabyModel;
    private final HumanoidModel<InvasionZombieRenderState> bruteBabyModel;
    private final boolean pigman;

    public InvasionZombieRenderer(EntityRendererProvider.Context context, boolean pigman) {
        super(
                context,
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                new ZombieModel<>(createLegacyUvBabyLayer().bakeRoot()),
                0.5F);
        normalModel = model;
        bruteModel = new LargeZombieModel(LargeZombieModel.createBodyLayer().bakeRoot());
        normalBabyModel = new ZombieModel<>(createLegacyUvBabyLayer().bakeRoot());
        bruteBabyModel = new LargeZombieModel(
                LargeZombieModel.createBabyBodyLayer().bakeRoot());
        this.pigman = pigman;

        ArmorModelSet<HumanoidModel<InvasionZombieRenderState>> normalArmor = ArmorModelSet.bake(
                ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new);
        ArmorModelSet<HumanoidModel<InvasionZombieRenderState>> babyArmor =
                createLegacyUvBabyArmor();
        addLayer(new VariantArmorLayer(
                this, normalArmor, babyArmor, context, false));
        addLayer(new HeadArmorLayer<>(
                this, context,
                state -> state.brute
                        ? state.headEquipment
                        : ItemStack.EMPTY,
                0.0F, 0.0F, 0.55F));

        HumanoidModel<InvasionZombieRenderState> emptyHead =
                new LargeZombieModel(LargeZombieModel.createArmorBodyLayer(
                        new CubeDeformation(1.0F), EquipmentSlot.CHEST).bakeRoot());
        HumanoidModel<InvasionZombieRenderState> emptyChest =
                new LargeZombieModel(LargeZombieModel.createArmorBodyLayer(
                        new CubeDeformation(1.0F), EquipmentSlot.CHEST).bakeRoot());
        ArmorModelSet<HumanoidModel<InvasionZombieRenderState>> bruteArmor =
                new ArmorModelSet<>(
                        emptyHead,
                        emptyChest,
                        normalArmor.legs(),
                        normalArmor.feet());
        addLayer(new VariantArmorLayer(
                this, bruteArmor, bruteArmor, context, true));
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
        ((AgeableMobRendererAccessor) (Object) this).invasion$setBabyModel(
                state.brute ? bruteBabyModel : normalBabyModel);
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

    private static LayerDefinition createLegacyUvBabyLayer() {
        MeshDefinition adultMesh = HumanoidModel.createMesh(
                CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(
                HumanoidModel.BABY_TRANSFORMER.apply(adultMesh), 64, 64);
    }

    private static ArmorModelSet<HumanoidModel<InvasionZombieRenderState>>
            createLegacyUvBabyArmor() {
        return HumanoidModel.createArmorMeshSet(
                        new CubeDeformation(0.5F),
                        new CubeDeformation(1.0F))
                .map(mesh -> new HumanoidModel<>(LayerDefinition.create(
                        HumanoidModel.BABY_TRANSFORMER.apply(mesh),
                        64, 32).bakeRoot()));
    }

    private final class VariantArmorLayer extends HumanoidArmorLayer<
            InvasionZombieRenderState,
            HumanoidModel<InvasionZombieRenderState>,
            HumanoidModel<InvasionZombieRenderState>> {
        private final boolean bruteLayer;
        private final HumanoidArmorLayer<
                InvasionZombieRenderState,
                HumanoidModel<InvasionZombieRenderState>,
                HumanoidModel<InvasionZombieRenderState>> legacyBabyLayer;

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
            legacyBabyLayer = bruteLayer
                    ? null
                    : new HumanoidArmorLayer<>(
                            parent,
                            babyArmor,
                            babyArmor,
                            context.getEquipmentRenderer());
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                InvasionZombieRenderState state, float yRot, float xRot) {
            boolean usesBruteModel = state.brute && !state.isBaby;
            if (usesBruteModel == bruteLayer) {
                if (state.isBaby && legacyBabyLayer != null) {
                    // The mod skins use the classic adult armor UV layout.
                    // Select its equipment texture while retaining the
                    // already baby-scaled armor geometry.
                    state.isBaby = false;
                    ItemStack headEquipment = state.headEquipment;
                    if (state.brute) {
                        state.headEquipment = ItemStack.EMPTY;
                    }
                    try {
                        legacyBabyLayer.submit(
                                poseStack, collector, light,
                                state, yRot, xRot);
                    } finally {
                        state.headEquipment = headEquipment;
                        state.isBaby = true;
                    }
                } else {
                    super.submit(
                            poseStack, collector, light,
                            state, yRot, xRot);
                }
            }
        }
    }
}
